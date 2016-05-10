# Pull To Refresh

下拉刷新控件,参考开源项目[android-Ultra-Pull-To-Refresh](https://github.com/liaohuqiu/android-Ultra-Pull-To-Refresh)的实现思路,高度解耦下拉刷新头部和下拉刷新内容.并且,有详细的代码注释.

相比android-Ultra-Pull-To-Refresh:
1. 更精简的代码实现.
2. 更丰富的代码注释.

# 集成方式

采用library project方式集成.

在gradle中:

```
dependencies {
    compile project(':library')
}
```

在Android.mk中:
```makefile
ptr_dir := library
src_dirs := src $(ptr_dir)/src
res_dirs := res $(ptr_dir)/res

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_AAPT_FLAGS := --extra-packages com.watch.pulltorefresh
```

# 使用方式

## 自定义参数

* resistance: 阻尼系数,用于人为增加下拉距离,增强下拉体验.计算公式：offsetY = realOffsetY / resistance.
* ratio_of_header_height_to_refresh: 触发刷新时的高度比例(与Header View的高度比例).
* duration_to_close: 回弹到刷新点的时间.
* duration_to_close_header: 头部回弹时间.
* pull_to_fresh: 下拉刷新(true)还是释放刷新(false).
* keep_header_when_refresh: 刷新过程中是否保持Header View的显示.

## 使用示例

#### xml配置
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.watch.pulltorefresh.PtrClassicFrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:ptr="http://schemas.android.com/apk/res-auto"
    android:id="@+id/id_ptr_frame_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000"

    ptr:duration_to_close="300"
    ptr:duration_to_close_header="1000"
    ptr:keep_header_when_refresh="true"
    ptr:pull_to_fresh="false"
    ptr:ratio_of_header_height_to_refresh="1.5"
    ptr:resistance="1.7">


    <TextView
        android:id="@+id/id_content"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:text="Hello World!"
        android:textColor="#ffffff"
        android:textSize="30sp" />
</com.watch.pulltorefresh.PtrClassicFrameLayout>
```

#### 代码使用
```java
private Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
        PtrFrameLayout frameLayout = (PtrFrameLayout) msg.obj;

        mTextView.setText("content num=" + (++ index));

        frameLayout.refreshComplete();
    }
};

private void initView() {
    PtrFrameLayout ptrFrameLayout = (PtrFrameLayout) findViewById(R.id.id_ptr_frame_layout);
    ptrFrameLayout.setPtrHandler(new PtrDefaultHandler() {
        @Override
        public void onRefreshBegin(final PtrFrameLayout frame) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Message msg = mHandler.obtainMessage(1);
                    msg.obj = frame;
                    mHandler.sendMessage(msg);
                }
            }).start();
        }
    });

    mTextView = (TextView) findViewById(R.id.id_content);
}
```