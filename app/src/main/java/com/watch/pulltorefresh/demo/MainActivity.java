package com.watch.pulltorefresh.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.watch.pulltorefresh.PtrDefaultHandler;
import com.watch.pulltorefresh.PtrFrameLayout;

public class MainActivity extends AppCompatActivity {
    private TextView mTextView;

    private int index;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            PtrFrameLayout frameLayout = (PtrFrameLayout) msg.obj;

            mTextView.setText("content num=" + (++index));

            frameLayout.refreshComplete();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

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
}
