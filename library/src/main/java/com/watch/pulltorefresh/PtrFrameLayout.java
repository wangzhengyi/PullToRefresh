package com.watch.pulltorefresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import com.watch.pulltorefresh.indicator.PtrIndicator;
import com.watch.pulltorefresh.util.L;


public class PtrFrameLayout extends ViewGroup {

    // status enum
    public final static byte PTR_STATUS_INIT = 1;
    public final static byte PTR_STATUS_PREPARE = 2;
    public final static byte PTR_STATUS_LOADING = 3;
    public final static byte PTR_STATUS_COMPLETE = 4;
    public static boolean DEBUG = true;
    // auto refresh status
    private static byte FLAG_AUTO_REFRESH_AT_ONCE = 0x01;
    private static byte FLAG_AUTO_REFRESH_BUT_LATER = 0x01 << 1;
    private static byte FLAG_ENABLE_NEXT_PTR_AT_ONCE = 0x01 << 2;
    private static byte FLAG_PIN_CONTENT = 0x01 << 3;
    private static byte MASK_AUTO_REFRESH = 0x03;

    /**
     * 下拉刷新的Content View.
     */
    protected View mContentView;
    private byte mStatus = PTR_STATUS_INIT;

    /**
     * 回弹到刷新高度的延迟时间.
     */
    private int mDurationToClose = 200;

    /**
     * 刷新完成后,Header View的回弹时间.
     */
    private int mDurationToCloseHeader = 10000;

    /**
     * 下拉刷新是否保持Header View的显示.
     */
    private boolean mKeepHeaderWhenRefresh = true;

    /**
     * 释放刷新还是下拉刷新.(true: 释放刷新;false: 下拉刷新)
     */
    private boolean mPullToRefresh = false;

    /**
     * 下拉刷新的Header View.
     */
    private View mHeaderView;
    private PtrUIHandlerHolder mPtrUIHandlerHolder = PtrUIHandlerHolder.create();
    private PtrHandler mPtrHandler;
    // working parameters
    private ScrollChecker mScrollChecker;

    /**
     * 横向移动的最小距离.
     */
    private int mPagingTouchSlop;
    private int mHeaderHeight;
    /**
     * 默认禁止横向移动.
     */
    private boolean mDisableWhenHorizontalMove = false;
    private int mFlag = 0x00;

    /**
     * 是否判定当前MotionEvent为横向移动.
     */
    private boolean mPermitForHorizontal = false;

    /**
     * 记录之前一个MotionEvent事件.
     */
    private MotionEvent mLastMoveEvent;

    private PtrUIHandlerHook mRefreshCompleteHook;

    private int mLoadingMinTime = 500;
    private long mLoadingStartTime = 0;
    private PtrIndicator mPtrIndicator;
    private boolean mHasSendCancelEvent = false;

    /**
     * 数据加载时间小于500ms,进行延迟加载
     */
    private Runnable mPerformRefreshCompleteDelay = new Runnable() {
        @Override
        public void run() {
            performRefreshComplete();
        }
    };

    public PtrFrameLayout(Context context) {
        this(context, null);
    }

    public PtrFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PtrFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPtrIndicator = new PtrIndicator();

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PtrFrameLayout, 0, 0);
        if (ta != null) {

            mPtrIndicator.setResistance(ta.getFloat(R.styleable.PtrFrameLayout_resistance,
                    mPtrIndicator.getResistance()));

            mDurationToClose = ta.getInt(R.styleable.PtrFrameLayout_duration_to_close,
                    mDurationToClose);
            mDurationToCloseHeader = ta.getInt(R.styleable.PtrFrameLayout_duration_to_close_header,
                    mDurationToCloseHeader);


            float ratio = ta.getFloat(R.styleable.PtrFrameLayout_ratio_of_header_height_to_refresh,
                    mPtrIndicator.getRatioOfHeaderHeightToRefresh());
            mPtrIndicator.setRatioOfHeaderHeightToRefresh(ratio);

            mKeepHeaderWhenRefresh = ta.getBoolean(
                    R.styleable.PtrFrameLayout_keep_header_when_refresh, mKeepHeaderWhenRefresh);

            mPullToRefresh = ta.getBoolean(R.styleable.PtrFrameLayout_pull_to_fresh,
                    mPullToRefresh);
            ta.recycle();
        }

        mScrollChecker = new ScrollChecker();

        final ViewConfiguration conf = ViewConfiguration.get(getContext());
        mPagingTouchSlop = conf.getScaledTouchSlop() * 2;
    }

    @Override
    protected void onFinishInflate() {
        final int childCount = getChildCount();
        if (childCount > 2 && childCount <= 0) {
            throw new IllegalStateException("PtrFrameLayout only can host 2 elements");
        } else if (childCount == 2) {
            mHeaderView = getChildAt(0);
            mContentView = getChildAt(1);
        } else {
            mContentView = getChildAt(0);
        }

        if (mHeaderView != null) {
            // TODO:作用
            mHeaderView.bringToFront();
        }
        super.onFinishInflate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mScrollChecker != null) {
            mScrollChecker.destroy();
        }

        if (mPerformRefreshCompleteDelay != null) {
            removeCallbacks(mPerformRefreshCompleteDelay);
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && p instanceof MarginLayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (DEBUG) {
            L.d("onMeasure frame: width: %s, height: %s, padding: %s %s %s %s",
                    getMeasuredHeight(), getMeasuredWidth(),
                    getPaddingLeft(), getPaddingRight(), getPaddingTop(), getPaddingBottom());

        }

        // 测量Header View, 重点获取Header View的高度.
        if (mHeaderView != null) {
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            mHeaderHeight = mHeaderView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            L.d("HeaderHeight=" + mHeaderHeight);
            mPtrIndicator.setHeaderHeight(mHeaderHeight);
        }

        // 测量Content View.
        if (mContentView != null) {
            measureChildWithMargins(mContentView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            if (DEBUG) {
                ViewGroup.MarginLayoutParams lp = (MarginLayoutParams)
                        mContentView.getLayoutParams();
                L.d("onMeasure content, width: %s, height: %s, margin: %s %s %s %s",
                        getMeasuredWidth(), getMeasuredHeight(),
                        lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin);
                L.d("onMeasure, currentPos: %s, lastPos: %s, top: %s",
                        mPtrIndicator.getCurrentPos(), mPtrIndicator.getLastPos(),
                        mContentView.getTop());
            }
        }
    }

    @Override
    protected void onLayout(boolean flag, int i, int j, int k, int l) {
        layoutChildren();
    }

    private void layoutChildren() {
        int offsetY = mPtrIndicator.getCurrentPos();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        // 布局Header View
        if (mHeaderView != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            // 初始向上偏移mHeaderHeight,隐藏HeaderView.
            final int top = -(mHeaderHeight - paddingTop - lp.topMargin - offsetY);
            final int right = left + mHeaderView.getMeasuredWidth();
            final int bottom = top + mHeaderView.getMeasuredHeight();
            mHeaderView.layout(left, top, right, bottom);
            if (DEBUG) {
                L.d("onLayout header: %s %s %s %s", left, top, right, bottom);
            }
        }

        // 布局Content View
        if (mContentView != null) {
            if (isPinContent()) {
                // 如果内容不跟随移动,则offsetY始终为0.
                offsetY = 0;
            }
            MarginLayoutParams lp = (MarginLayoutParams) mContentView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin + offsetY;
            final int right = left + mContentView.getMeasuredWidth();
            final int bottom = top + mContentView.getMeasuredHeight();
            if (DEBUG) {
                L.d("onLayout content: %s %s %s %s", left, top, right, bottom);
            }
            mContentView.layout(left, top, right, bottom);
        }
    }

    public boolean dispatchTouchEventSupper(MotionEvent e) {
        return super.dispatchTouchEvent(e);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (!isEnabled() || mContentView == null || mHeaderView == null) {
            return dispatchTouchEventSupper(e);
        }

        int action = e.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mHasSendCancelEvent = false;
                mPtrIndicator.onPressDown(e.getX(), e.getY());

                mScrollChecker.abortIfWorking();

                mPermitForHorizontal = false;
                dispatchTouchEventSupper(e);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mPtrIndicator.onRelease();
                if (mPtrIndicator.hasLeftStartPosition()) {
                    if (DEBUG) {
                        L.d("call onRelease when user release");
                    }
                    onRelease(false);
                    if (mPtrIndicator.hasMovedAfterPressedDown()) {
                        sendCancelEvent();
                        return true;
                    }
                    return dispatchTouchEventSupper(e);
                } else {
                    return dispatchTouchEventSupper(e);
                }

            case MotionEvent.ACTION_MOVE:
                mLastMoveEvent = e;
                mPtrIndicator.onMove(e.getX(), e.getY());
                float offsetX = mPtrIndicator.getOffsetX();
                float offsetY = mPtrIndicator.getOffsetY();

                if (mDisableWhenHorizontalMove
                        && !mPermitForHorizontal
                        && Math.abs(offsetX) > mPagingTouchSlop
                        && Math.abs(offsetX) > Math.abs(offsetY)
                        && mPtrIndicator.isInStartPosition()) {
                    // 不阻止横向移动,且判定为横向移动,并且HeaderView没有发生位移,则将Move事件交给父类处理.
                    mPermitForHorizontal = true;
                }

                if (mPermitForHorizontal) {
                    // 判定为横向移动,则交给父类处理.
                    return dispatchTouchEventSupper(e);
                }

                boolean moveDown = offsetY > 0;
                boolean moveUp = !moveDown;
                boolean canMoveUp = mPtrIndicator.hasLeftStartPosition();

                // 如果是下拉事件,但是子控件可以向下滑动,则将Move事件将给父类处理.
                if (moveDown && mPtrHandler != null
                        && !mPtrHandler.checkCanDoRefresh(this, mContentView, mHeaderView)) {
                    return dispatchTouchEventSupper(e);
                }

                // 处理下拉或上滑事件.
                if ((moveUp && canMoveUp) || moveDown) {
                    movePos(offsetY);
                    return true;
                }
        }
        return dispatchTouchEventSupper(e);
    }

    /**
     * 处理下拉或上滑事件.
     *
     * @param deltaY 距离上次move事件的y轴偏.e.g.deltaY > 0, 向下移动; deltaY < 0, 向上移动.
     */
    private void movePos(float deltaY) {
        if ((deltaY < 0 && mPtrIndicator.isInStartPosition())) {
            // Header View已经完全隐藏, 无法向上移动.
            return;
        }

        // 当前HeaderView的y轴绝对位置
        int to = mPtrIndicator.getCurrentPos() + (int) deltaY;
        // 向上移动的最大值就是Header View全部隐藏
        if (mPtrIndicator.willOverTop(to)) {
            to = PtrIndicator.POS_START;
        }

        mPtrIndicator.setCurrentPos(to);

        // 计算移动偏移量
        int posOffset = to - mPtrIndicator.getLastPos();
        updatePos(posOffset);
    }

    /**
     * 根据移动偏移更新下拉刷新状态
     */
    private void updatePos(int posOffset) {
        if (posOffset == 0) {
            return;
        }

        boolean isUnderTouch = mPtrIndicator.isUnderTouch();

        // PtrFrameLayout接管MotionEvent后,需要发送CANCEL事件给子控件
        if (isUnderTouch && !mHasSendCancelEvent && mPtrIndicator.hasMovedAfterPressedDown()) {
            mHasSendCancelEvent = true;
            sendCancelEvent();
        }

        if ((mPtrIndicator.hasJustLeftStartPosition() && mStatus == PTR_STATUS_INIT) ||
                (mPtrIndicator.goDownCrossFinishPosition()
                        && mStatus == PTR_STATUS_COMPLETE
                        && isEnabledNextPtrAtOnce())) {
            // 设置刷新状态为准备刷新状态
            mStatus = PTR_STATUS_PREPARE;
            // 将Header View的UI设置为准备刷新的UI.
            mPtrUIHandlerHolder.onUIRefreshPrepare(this);
            if (DEBUG) {
                L.i("PtrUIHandler: onUIRefreshPrepare, 准备刷新, mFlag %s", mFlag);
            }
        }

        // 用户手动回到PtrFrameLayout的初始状态
        if (mPtrIndicator.hasJustBackToStartPosition()) {
            tryToNotifyReset();

            // recover event to children
            if (isUnderTouch) {
                sendDownEvent();
            }
        }

        // Pull to Refresh
        if (mStatus == PTR_STATUS_PREPARE) {
            // 如果达到刷新高度且mPullToRefresh=true,则立刻刷新
            if (isUnderTouch && !isAutoRefresh() && mPullToRefresh
                    && mPtrIndicator.crossRefreshLineFromTopToBottom()) {
                tryToPerformRefresh();
            }

            // reach header height while auto refresh
            if (performAutoRefreshButLater()
                    && mPtrIndicator.hasJustReachedHeaderHeightFromTopToBottom()) {
                tryToPerformRefresh();
            }
        }

        if (DEBUG) {
            L.v("updatePos: change: %s, current: %s last: %s, top: %s, headerHeight: %s",
                    posOffset, mPtrIndicator.getCurrentPos(), mPtrIndicator.getLastPos(), mContentView.getTop(), mHeaderHeight);
        }

        // TODO: offsetTopAndBottom作用.
        mHeaderView.offsetTopAndBottom(posOffset);
        if (!isPinContent()) {
            mContentView.offsetTopAndBottom(posOffset);
        }
        // TODO: 针对ViewGroup的invalidate作用.
        invalidate();

        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIPositionChange(this, isUnderTouch, mStatus, mPtrIndicator);
        }
    }


    /**
     * 向父类发送ACTION_CANCEL事件
     */
    private void sendCancelEvent() {
        if (DEBUG) {
            L.d("send cancel event");
        }
        // The ScrollChecker will update position and lead to send cancel event when mLastMoveEvent is null.
        // fix #104, #80, #92
        if (mLastMoveEvent == null) {
            return;
        }
        MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(),
                last.getEventTime() + ViewConfiguration.getLongPressTimeout(),
                MotionEvent.ACTION_CANCEL, last.getX(), last.getY(), last.getMetaState());
        dispatchTouchEventSupper(e);
    }

    /**
     * 向父类发送ACTION_DOWN事件
     */
    private void sendDownEvent() {
        if (DEBUG) {
            L.d("send down event");
        }
        final MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(), last.getEventTime(),
                MotionEvent.ACTION_DOWN, last.getX(), last.getY(), last.getMetaState());
        dispatchTouchEventSupper(e);
    }

    /**
     * MOTION_UP, MOTION_DOWN对应的操作.
     * @param stayForLoading 是否在当前位置进行刷新. false:回弹到指定高度. true:在当前位置刷新.
     */
    private void onRelease(boolean stayForLoading) {

        tryToPerformRefresh();

        if (mStatus == PTR_STATUS_LOADING) {
            // keep header for fresh
            if (mKeepHeaderWhenRefresh) {
                // scroll header back
                if (mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && !stayForLoading) {
                    L.e("tryToScrollTo operation!");
                    mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToKeepHeaderWhileLoading(),
                            mDurationToClose);
                } else {
                    // do nothing
                    L.e("onRelease do nothing!");
                }
            } else {
                tryScrollBackToTopWhileLoading();
            }
        } else {
            if (mStatus == PTR_STATUS_COMPLETE) {
                notifyUIRefreshComplete(false);
            } else {
                // 刚下拉一下未达到刷新距离释放后操作,视为放弃刷新.
                tryScrollBackToTopAbortRefresh();
            }
        }
    }

    /**
     * please DO REMEMBER resume the hook
     *
     * @param hook
     */

    public void setRefreshCompleteHook(PtrUIHandlerHook hook) {
        mRefreshCompleteHook = hook;
        hook.setResumeAction(new Runnable() {
            @Override
            public void run() {
                if (DEBUG) {
                    L.d("mRefreshCompleteHook resume.");
                }
                notifyUIRefreshComplete(true);
            }
        });
    }

    /**
     * Scroll back to to if is not under touch
     */
    private void tryScrollBackToTop() {
        if (!mPtrIndicator.isUnderTouch()) {
            mScrollChecker.tryToScrollTo(PtrIndicator.POS_START, mDurationToCloseHeader);
        }
    }


    private void tryScrollBackToTopWhileLoading() {
        tryScrollBackToTop();
    }

    private void tryScrollBackToTopAfterComplete() {
        tryScrollBackToTop();
    }

    private void tryScrollBackToTopAbortRefresh() {
        tryScrollBackToTop();
    }

    /**
     * 准备开始刷新操作
     */
    private void tryToPerformRefresh() {
        if (mStatus != PTR_STATUS_PREPARE) {
            // 开始刷新操作前,刷新状态必须为PTR_STATUS_PREPARE
            return;
        }

        if ((mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && isAutoRefresh())
                || mPtrIndicator.isOverOffsetToRefresh()) {
            // 设置刷新状态为PTR_STATUS_LOADING
            mStatus = PTR_STATUS_LOADING;
            // 执行刷新操作
            performRefresh();
        }
    }

    /**
     * 执行刷新操作
     */
    private void performRefresh() {
        mLoadingStartTime = System.currentTimeMillis();
        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIRefreshBegin(this);
            if (DEBUG) {
                L.i("PtrUIHandler: onUIRefreshBegin");
            }
        }
        if (mPtrHandler != null) {
            mPtrHandler.onRefreshBegin(this);
        }
    }

    /**
     * If at the top and not in loading, reset
     */
    private boolean tryToNotifyReset() {
        if ((mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_PREPARE)
                && mPtrIndicator.isInStartPosition()) {
            if (mPtrUIHandlerHolder.hasHandler()) {
                mPtrUIHandlerHolder.onUIReset(this);
                if (DEBUG) {
                    L.i("PtrUIHandler: onUIReset");
                }
            }
            mStatus = PTR_STATUS_INIT;
            clearFlag();
            return true;
        }
        return false;
    }

    protected void onPtrScrollAbort() {
        if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            // 目前刷新头部有偏移且处于自动刷新状态
            if (DEBUG) {
                L.d("call onRelease after scroll abort");
            }
            onRelease(true);
        }
    }

    protected void onPtrScrollFinish() {
        if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            if (DEBUG) {
                L.d("call onRelease after scroll finish");
            }
            onRelease(true);
        }
    }

    /**
     * 用户加载数据结束后需要回调PtrFrameLayout的refreshComplete方法.
     */
    final public void refreshComplete() {
        if (DEBUG) {
            L.i("refreshComplete");
        }

        if (mRefreshCompleteHook != null) {
            mRefreshCompleteHook.reset();
        }

        int delay = (int) (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime));
        if (delay <= 0) {
            if (DEBUG) {
                L.d("performRefreshComplete at once");
            }
            performRefreshComplete();
        } else {
            postDelayed(mPerformRefreshCompleteDelay, delay);
            if (DEBUG) {
                L.d("performRefreshComplete after delay: %s", delay);
            }
        }
    }

    /**
     * Do refresh complete work when time elapsed is greater than {@link #mLoadingMinTime}
     */
    private void performRefreshComplete() {
        mStatus = PTR_STATUS_COMPLETE;

        // if is auto refresh do nothing, wait scroller stop
        if (mScrollChecker.mIsRunning && isAutoRefresh()) {
            // do nothing
            if (DEBUG) {
                L.d("performRefreshComplete do nothing, scrolling: %s, auto refresh: %s",
                        mScrollChecker.mIsRunning, mFlag);
            }
            return;
        }

        notifyUIRefreshComplete(false);
    }

    /**
     * Do real refresh work. If there is a hook, execute the hook first.
     *
     * @param ignoreHook
     */
    private void notifyUIRefreshComplete(boolean ignoreHook) {
        /**
         * After hook operation is done, {@link #notifyUIRefreshComplete} will be call in resume action to ignore hook.
         */
        if (mPtrIndicator.hasLeftStartPosition() && !ignoreHook && mRefreshCompleteHook != null) {
            if (DEBUG) {
                L.d("notifyUIRefreshComplete mRefreshCompleteHook run.");
            }

            mRefreshCompleteHook.takeOver();
            return;
        }
        if (mPtrUIHandlerHolder.hasHandler()) {
            if (DEBUG) {
                L.i("PtrUIHandler: onUIRefreshComplete");
            }
            mPtrUIHandlerHolder.onUIRefreshComplete(this);
        }
        mPtrIndicator.onUIRefreshComplete();
        tryScrollBackToTopAfterComplete();
        tryToNotifyReset();
    }

    public void autoRefresh() {
        autoRefresh(true, mDurationToCloseHeader);
    }

    public void autoRefresh(boolean atOnce) {
        autoRefresh(atOnce, mDurationToCloseHeader);
    }

    private void clearFlag() {
        // remove auto fresh flag
        mFlag = mFlag & ~MASK_AUTO_REFRESH;
    }

    public void autoRefresh(boolean atOnce, int duration) {

        if (mStatus != PTR_STATUS_INIT) {
            return;
        }

        mFlag |= atOnce ? FLAG_AUTO_REFRESH_AT_ONCE : FLAG_AUTO_REFRESH_BUT_LATER;

        mStatus = PTR_STATUS_PREPARE;
        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIRefreshPrepare(this);
            if (DEBUG) {
                L.i("PtrUIHandler: onUIRefreshPrepare, mFlag %s", mFlag);
            }
        }
        mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToRefresh(), duration);
        if (atOnce) {
            mStatus = PTR_STATUS_LOADING;
            performRefresh();
        }
    }

    public boolean isAutoRefresh() {
        return (mFlag & MASK_AUTO_REFRESH) > 0;
    }

    private boolean performAutoRefreshButLater() {
        return (mFlag & MASK_AUTO_REFRESH) == FLAG_AUTO_REFRESH_BUT_LATER;
    }

    /**
     * 是否允许刷新完成后立刻再进行刷新.
     */
    public boolean isEnabledNextPtrAtOnce() {
        return (mFlag & FLAG_ENABLE_NEXT_PTR_AT_ONCE) > 0;
    }

    /**
     * If @param enable has been set to true. The user can perform next PTR at once.
     *
     * @param enable
     */
    public void setEnabledNextPtrAtOnce(boolean enable) {
        if (enable) {
            mFlag = mFlag | FLAG_ENABLE_NEXT_PTR_AT_ONCE;
        } else {
            mFlag = mFlag & ~FLAG_ENABLE_NEXT_PTR_AT_ONCE;
        }
    }

    /**
     * 下拉刷新的Content View是否跟随下拉移动.
     */
    public boolean isPinContent() {
        return (mFlag & FLAG_PIN_CONTENT) > 0;
    }

    /**
     * 设置Content View是否跟随手势移动.
     *
     * @param pinContent true:不跟随 false:跟随
     */
    public void setPinContent(boolean pinContent) {
        if (pinContent) {
            mFlag = mFlag | FLAG_PIN_CONTENT;
        } else {
            mFlag = mFlag & ~FLAG_PIN_CONTENT;
        }
    }

    /**
     * 设置是否禁止横向移动.
     *
     * @param disable true:允许横向移动; false:禁止横向移动
     */
    public void disableWhenHorizontalMove(boolean disable) {
        mDisableWhenHorizontalMove = disable;
    }

    /**
     * 设置数据加载的最小时间.
     */
    public void setLoadingMinTime(int time) {
        mLoadingMinTime = time;
    }

    /**
     * 设置PtrIndicator.
     */
    public void setPtrIndicator(PtrIndicator slider) {
        if (mPtrIndicator != null && mPtrIndicator != slider) {
            slider.convertFrom(mPtrIndicator);
        }
        mPtrIndicator = slider;
    }

    /**
     * 获取Header View的高度.
     */
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    /**
     * 获取Header View对象.
     */
    public View getHeaderView() {
        return mHeaderView;
    }

    /**
     * 在PtrFrameLayout上添加Header View.
     */
    public void setHeaderView(View header) {
        if (mHeaderView != null && header != null && mHeaderView != header) {
            removeView(mHeaderView);
        }
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            header.setLayoutParams(lp);
        }
        mHeaderView = header;
        addView(header);
    }

    /**
     * 获取下拉刷新的高度.
     */
    public int getOffsetToRefresh() {
        return mPtrIndicator.getOffsetToRefresh();
    }

    /**
     * 设置下拉刷新的高度.
     */
    public void setOffsetToRefresh(int offset) {
        mPtrIndicator.setOffsetToRefresh(offset);
    }

    /**
     * 获取下拉刷新的阻尼系数.
     */
    public float getRatioOfHeaderToHeightRefresh() {
        return mPtrIndicator.getRatioOfHeaderHeightToRefresh();
    }

    /**
     * 设置下拉刷新的阻尼系数.
     */
    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        mPtrIndicator.setRatioOfHeaderHeightToRefresh(ratio);
    }

    /**
     * 返回刷新方式.
     *
     * @return true:释放刷新 false:下拉刷新
     */
    public boolean isPullToRefresh() {
        return mPullToRefresh;
    }

    /**
     * 设置是否达到下拉刷新高度立刻刷新
     *
     * @param pullToRefresh true:下拉直接刷新 false:释放刷新
     */
    public void setPullToRefresh(boolean pullToRefresh) {
        mPullToRefresh = pullToRefresh;
    }

    /**
     * 判定下拉刷新是否保持Header View显示.
     */
    public boolean isKeepHeaderWhenRefresh() {
        return mKeepHeaderWhenRefresh;
    }

    /**
     * 设定下拉刷新时是否显示Header View
     *
     * @param keepOrNot true:显示; false:不显示.
     */
    public void setKeepHeaderWhenRefresh(boolean keepOrNot) {
        mKeepHeaderWhenRefresh = keepOrNot;
    }

    /**
     * 获取Header View的回弹延迟时间.
     */
    public long getDurationToCloseHeader() {
        return mDurationToCloseHeader;
    }

    /**
     * 设置Header View的回弹延迟时间.
     */
    public void setDurationToCloseHeader(int duration) {
        mDurationToCloseHeader = duration;
    }

    /**
     * 获取回弹到刷新高度的延迟时间.
     */
    public float getDurationToClose() {
        return mDurationToClose;
    }

    /**
     * 设置回弹到刷新高度的延迟时间.
     */
    public void setDurationToClose(int duration) {
        mDurationToClose = duration;
    }

    /**
     * 获取阻尼系数.
     */
    public float getResistance() {
        return mPtrIndicator.getResistance();
    }

    /**
     * 设置阻尼系数.
     */
    public void setResistance(float resistance) {
        mPtrIndicator.setResistance(resistance);
    }

    /**
     * 设置下拉刷新功能实现类.
     */
    public void setPtrHandler(PtrHandler ptrHandler) {
        mPtrHandler = ptrHandler;
    }

    /**
     * 添加下拉刷新Header View显示类.
     */
    public void addPtrUIHandler(PtrUIHandler ptrUIHandler) {
        PtrUIHandlerHolder.addHandler(mPtrUIHandlerHolder, ptrUIHandler);
    }

    /**
     * 删除指定的下拉刷新头部.
     */
    public void removePtrUIHandler(PtrUIHandler ptrUIHandler) {
        mPtrUIHandlerHolder = PtrUIHandlerHolder.removeHandler(mPtrUIHandlerHolder, ptrUIHandler);
    }

    /**
     * 获取下拉刷新时Header View的显示高度.
     */
    public int getOffsetToKeepHeaderWhileLoading() {
        return mPtrIndicator.getOffsetToKeepHeaderWhileLoading();
    }

    /**
     * 设置下拉刷新时Header View的显示高度.
     */
    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mPtrIndicator.setOffsetToKeepHeaderWhileLoading(offset);
    }

    /**
     * 封装Scroller实现回弹效果.
     */
    class ScrollChecker implements Runnable {
        private int mLastY;
        private Scroller mScroller;
        private boolean mIsRunning = false;
        private int mStart;
        private int mTo;

        public ScrollChecker() {
            mScroller = new Scroller(getContext());
        }

        public void run() {
            boolean finish = !mScroller.computeScrollOffset() || mScroller.isFinished();
            int curY = mScroller.getCurrY();
            int deltaY = curY - mLastY;
            if (DEBUG) {
                if (deltaY != 0) {
                    L.e("scroll: %s, start: %s, to: %s, currentPos: %s, current :%s, last: %s, " +
                                    "delta: %s", finish, mStart, mTo, mPtrIndicator.getCurrentPos(),
                            curY, mLastY, deltaY);
                }
            }
            if (!finish) {
                // 如果滑动没有停止
                mLastY = curY;
                movePos(deltaY);
                post(this);
            } else {
                finish();
            }
        }

        private void finish() {
            if (DEBUG) {
                L.v("finish, currentPos:%s", mPtrIndicator.getCurrentPos());
            }
            reset();
            onPtrScrollFinish();
        }

        private void reset() {
            mIsRunning = false;
            mLastY = 0;
            removeCallbacks(this);
        }

        /**
         * Destory方法:1.重置标志位;2.防止内存泄露.
         */
        private void destroy() {
            reset();
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
        }

        /**
         * 阻止滑动回弹,重置滑动标志位.
         */
        public void abortIfWorking() {
            if (mIsRunning) {
                L.e("ScrollerChecker abort if working!");
                if (!mScroller.isFinished()) {
                    L.e("强制停止！！！！");
                    mScroller.forceFinished(true);
                }
                onPtrScrollAbort();
                reset();
            } else {
                L.e("ScrollerChecker is not working!");
            }
        }

        public void tryToScrollTo(int to, int duration) {
            if (mPtrIndicator.isAlreadyHere(to)) {
                return;
            }
            mStart = mPtrIndicator.getCurrentPos();
            mTo = to;
            int distance = to - mStart;
            if (DEBUG) {
                L.d("tryToScrollTo: start: %s, distance:%s, to:%s", mStart, distance, to);
            }
            removeCallbacks(this);

            mLastY = 0;

            // fix #47: Scroller should be reused, https://github.com/liaohuqiu/android-Ultra-Pull-To-Refresh/issues/47
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            mScroller.startScroll(0, 0, 0, distance, duration);
            post(this);
            mIsRunning = true;
        }
    }
}
