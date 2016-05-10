package com.watch.pulltorefresh;

import android.view.View;

/**
 * 下拉刷新接口抽象
 */
public interface PtrHandler {
    /**
     * 判断当前View是否能刷新,UI层面的控制.(例如:ListView和ScrollView是否处于顶部)
     */
    boolean checkCanDoRefresh(final PtrFrameLayout frame, final View content, final View header);

    /**
     * 刷新回调函数
     */
    void onRefreshBegin(final PtrFrameLayout frame);
}
