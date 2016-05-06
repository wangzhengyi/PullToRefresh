package com.watch.pulltorefresh;

import android.view.View;

/**
 * Created by wzy on 16-5-4.
 */
public interface PtrHandler {
    boolean checkCanDoRefresh(final PtrFrameLayout frame, final View content, final View header);

    void onRefreshBegin(final PtrFrameLayout frame);
}
