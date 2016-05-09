package com.watch.pulltorefresh;

import com.watch.pulltorefresh.indicator.PtrIndicator;

public interface PtrUIHandler {
    void onUIReset(PtrFrameLayout frame);

    /**
     * 下拉头部第一次出现时调用,表示准备刷新,例如文字提示下拉
     */
    void onUIRefreshPrepare(PtrFrameLayout frame);

    /**
     * 刷新进行时的UI显示.
     */
    void onUIRefreshBegin(PtrFrameLayout frame);

    void onUIRefreshComplete(PtrFrameLayout frame);

    void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status,
                            PtrIndicator ptrIndicator);
}
