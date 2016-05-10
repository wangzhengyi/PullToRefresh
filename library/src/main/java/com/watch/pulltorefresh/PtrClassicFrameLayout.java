package com.watch.pulltorefresh;

import android.content.Context;
import android.util.AttributeSet;

public class PtrClassicFrameLayout extends PtrFrameLayout {

    private PtrClassicDefaultHeader mPtrClassicHeader;

    public PtrClassicFrameLayout(Context context) {
        this(context, null);
    }

    public PtrClassicFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PtrClassicFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    private void initViews() {
        mPtrClassicHeader = new PtrClassicDefaultHeader(getContext());
        setHeaderView(mPtrClassicHeader);
        addPtrUIHandler(mPtrClassicHeader);
    }

    @SuppressWarnings("unused")
    public PtrClassicDefaultHeader getHeader() {
        return mPtrClassicHeader;
    }
}
