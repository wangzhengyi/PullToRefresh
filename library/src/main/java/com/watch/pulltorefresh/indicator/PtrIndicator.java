package com.watch.pulltorefresh.indicator;

import android.graphics.PointF;

public class PtrIndicator {
    public final static int POS_START = 0;
    /**
     * 触发下拉刷新的高度.
     */
    protected int mOffsetToRefresh = 0;

    /**
     * 手指按下的坐标.
     */
    private PointF mPtrLastMove = new PointF();
    private float mOffsetX;
    private float mOffsetY;
    private int mCurrentPos = 0;
    private int mLastPos = 0;
    /**
     * Header View的测量高度.
     */
    private int mHeaderHeight;
    private int mPressedPos = 0;
    /**
     * 触发下拉刷新的高度与HeaderView的高度比例.
     */
    private float mRatioOfHeaderHeightToRefresh = 1.2f;
    /**
     * 阻尼系数.
     */
    private float mResistance = 1.7f;
    private boolean mIsUnderTouch = false;
    private int mOffsetToKeepHeaderWhileLoading = -1;
    private int mRefreshCompleteY = 0;

    public float getResistance() {
        return mResistance;
    }

    public void setResistance(float resistance) {
        mResistance = resistance;
    }

    public boolean isUnderTouch() {
        return mIsUnderTouch;
    }

    /**
     * MotionEvent.ACTION_UP和MotionEvent.ACTION_CANCEL.
     */
    public void onRelease() {
        mIsUnderTouch = false;
    }

    public void onUIRefreshComplete() {
        mRefreshCompleteY = mCurrentPos;
    }

    public boolean goDownCrossFinishPosition() {
        return mCurrentPos >= mRefreshCompleteY;
    }

    /**
     * 获取下拉刷新比例值.
     */
    public float getRatioOfHeaderHeightToRefresh() {
        return mRatioOfHeaderHeightToRefresh;
    }

    /**
     * 根据比例设置下拉刷新的高度值.
     */
    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        mRatioOfHeaderHeightToRefresh = ratio;
        mOffsetToRefresh = (int) (mHeaderHeight * ratio);
    }

    /**
     * 获取触发下拉刷新的高度值.
     */
    public int getOffsetToRefresh() {
        return mOffsetToRefresh;
    }

    /**
     * 设置触发下拉刷新的高度值.
     */
    public void setOffsetToRefresh(int offset) {
        mRatioOfHeaderHeightToRefresh = offset * 1.0f / mHeaderHeight;
        mOffsetToRefresh = offset;
    }

    /**
     * 记录MotionEvent.ACTION_DOWN事件.
     */
    public void onPressDown(float x, float y) {
        mIsUnderTouch = true;
        mPressedPos = mCurrentPos;
        mPtrLastMove.set(x, y);
    }

    /**
     * 记录MotionEvent.ACTION_MOVE事件.
     */
    public final void onMove(float x, float y) {
        float offsetX = x - mPtrLastMove.x;
        float offsetY = y - mPtrLastMove.y;
        // 阻尼系数用于增加拉动效果.
        setOffset(offsetX, offsetY / mResistance);
        mPtrLastMove.set(x, y);
    }

    private void setOffset(float x, float y) {
        mOffsetX = x;
        mOffsetY = y;
    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getOffsetY() {
        return mOffsetY;
    }

    public int getLastPosY() {
        return mLastPos;
    }

    public int getCurrentPosY() {
        return mCurrentPos;
    }

    public final void setCurrentPos(int current) {
        mLastPos = mCurrentPos;
        mCurrentPos = current;
        onUpdatePos(current, mLastPos);
    }

    protected void onUpdatePos(int current, int last) {

    }

    /**
     * 设置Header View的高度.
     */
    public void setHeaderHeight(int height) {
        mHeaderHeight = height;
        updateOffsetToRefresh();
    }

    /**
     * 获取Header View的高度.
     */
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    /**
     * 更新触发下拉刷新的高度.
     */
    protected void updateOffsetToRefresh() {
        mOffsetToRefresh = (int) (mRatioOfHeaderHeightToRefresh * mHeaderHeight);
    }

    public void convertFrom(PtrIndicator ptrSlider) {
        mCurrentPos = ptrSlider.mCurrentPos;
        mLastPos = ptrSlider.mLastPos;
        mHeaderHeight = ptrSlider.mHeaderHeight;
    }

    /**
     * Header View是否有偏移.
     */
    public boolean hasLeftStartPosition() {
        return mCurrentPos > POS_START;
    }

    /**
     * 是否刚开始下拉.
     */
    public boolean hasJustLeftStartPosition() {
        return mLastPos == POS_START && hasLeftStartPosition();
    }

    public boolean hasJustBackToStartPosition() {
        return mLastPos != POS_START && isInStartPosition();
    }

    public boolean isOverOffsetToRefresh() {
        return mCurrentPos >= getOffsetToRefresh();
    }

    public boolean isInStartPosition() {
        return mCurrentPos == POS_START;
    }

    public boolean crossRefreshLineFromTopToBottom() {
        return mLastPos < getOffsetToRefresh() && mCurrentPos >= getOffsetToRefresh();
    }

    public boolean hasJustReachedHeaderHeightFromTopToBottom() {
        return mLastPos < mHeaderHeight && mCurrentPos >= mHeaderHeight;
    }

    public boolean isOverOffsetToKeepHeaderWhileLoading() {
        return mCurrentPos > getOffsetToKeepHeaderWhileLoading();
    }

    public int getOffsetToKeepHeaderWhileLoading() {
        return mOffsetToKeepHeaderWhileLoading >= 0 ?
                mOffsetToKeepHeaderWhileLoading : mHeaderHeight;
    }

    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mOffsetToKeepHeaderWhileLoading = offset;
    }

    public boolean isAlreadyHere(int to) {
        return mCurrentPos == to;
    }

    public float getLastPercent() {
        final float oldPercent = mHeaderHeight == 0 ? 0 : mLastPos * 1f / mHeaderHeight;
        return oldPercent;
    }

    public float getCurrentPercent() {
        final float currentPercent = mHeaderHeight == 0 ? 0 : mCurrentPos * 1f / mHeaderHeight;
        return currentPercent;
    }

    public boolean willOverTop(int to) {
        return to < POS_START;
    }

    public boolean hasMovedAfterPressedDown() {
        return mCurrentPos == POS_START;
    }
}
