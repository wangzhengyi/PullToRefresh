package com.watch.pulltorefresh;

import android.view.View;

/**
 * 下拉刷新接口抽象
 */
public interface PtrHandler {
    /**
     * 判断是否可以下拉刷新
     */
    boolean checkCanDoRefresh(final PtrFrameLayout frame, final View content, final View header);

    /**
     * 刷新回调函数
     */
    void onRefreshBegin(final PtrFrameLayout frame);
}
