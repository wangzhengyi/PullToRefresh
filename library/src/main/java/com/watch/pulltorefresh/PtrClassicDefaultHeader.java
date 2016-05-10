package com.watch.pulltorefresh;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.watch.pulltorefresh.indicator.PtrIndicator;

public class PtrClassicDefaultHeader extends FrameLayout implements PtrUIHandler {
    private static final int ROTATE_ANIMATION_TIME = 150;
    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    /**
     * 显示刷新提示的TextView.
     */
    private TextView mTitleTextView;

    /**
     * 箭头图标.
     */
    private ImageView mRotateImageView;

    /**
     * 进度圆环.
     */
    private View mProgressBar;

    public PtrClassicDefaultHeader(Context context) {
        this(context, null);
    }

    public PtrClassicDefaultHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PtrClassicDefaultHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        buildAnimation();
        initViews();
    }

    private void buildAnimation() {
        mFlipAnimation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(ROTATE_ANIMATION_TIME);
        mFlipAnimation.setFillAfter(true);

        mReverseFlipAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(ROTATE_ANIMATION_TIME);
        mReverseFlipAnimation.setFillAfter(true);
    }

    protected void initViews() {
        View header = LayoutInflater.from(getContext()).inflate(R.layout.ptr_classic_default_header, this);

        mRotateImageView = (ImageView) header.findViewById(R.id.ptr_classic_header_rotate_view);
        mTitleTextView = (TextView) header.
                findViewById(R.id.ptr_classic_header_rotate_view_header_title);
        mProgressBar = header.findViewById(R.id.ptr_classic_header_rotate_view_progressbar);

        resetView();
    }

    private void resetView() {
        hideRotateView();
        mProgressBar.setVisibility(INVISIBLE);
    }

    private void hideRotateView() {
        mRotateImageView.clearAnimation();
        mRotateImageView.setVisibility(INVISIBLE);
    }

    @Override
    public void onUIReset(PtrFrameLayout frame) {
        resetView();
    }

    @Override
    public void onUIRefreshPrepare(PtrFrameLayout frame) {
        mProgressBar.setVisibility(INVISIBLE);
        mRotateImageView.setVisibility(VISIBLE);
        mTitleTextView.setVisibility(VISIBLE);

        mTitleTextView.setText(getResources().getString(R.string.pull_down_refresh_weather));
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        hideRotateView();
        mProgressBar.setVisibility(VISIBLE);
        mTitleTextView.setVisibility(VISIBLE);
        mTitleTextView.setText(getResources().getString(R.string.ptr_refreshing));
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame) {
        hideRotateView();
        mProgressBar.setVisibility(INVISIBLE);
        mTitleTextView.setVisibility(INVISIBLE);
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status,
                                   PtrIndicator ptrIndicator) {

        final int mOffsetToRefresh = frame.getOffsetToRefresh();
        final int currentPos = ptrIndicator.getCurrentPos();
        final int lastPos = ptrIndicator.getLastPos();

        if (currentPos < mOffsetToRefresh && lastPos >= mOffsetToRefresh) {
            // 从释放刷新->下拉刷新状态
            if (isUnderTouch && status == PtrFrameLayout.PTR_STATUS_PREPARE) {
                setTitleTextToPullDownRefresh(frame);
                if (mRotateImageView != null) {
                    mRotateImageView.clearAnimation();
                    mRotateImageView.startAnimation(mReverseFlipAnimation);
                }
            }
        } else if (currentPos > mOffsetToRefresh && lastPos <= mOffsetToRefresh) {
            // 从下拉刷新->释放刷新状态
            if (isUnderTouch && status == PtrFrameLayout.PTR_STATUS_PREPARE) {
                setTitleTextReleaseRefresh(frame);
                if (mRotateImageView != null) {
                    mRotateImageView.clearAnimation();
                    mRotateImageView.startAnimation(mFlipAnimation);
                }
            }
        }
    }

    private void setTitleTextReleaseRefresh(PtrFrameLayout frame) {
        if (!frame.isPullToRefresh()) {
            mTitleTextView.setVisibility(VISIBLE);
            mTitleTextView.setText(getResources().getString(R.string.release_refresh_weather));
        }
    }

    @SuppressWarnings("UnusedParameters")
    private void setTitleTextToPullDownRefresh(PtrFrameLayout frame) {
        mTitleTextView.setVisibility(VISIBLE);
        mTitleTextView.setText(getResources().getString(R.string.pull_down_refresh_weather));
    }
}
