package com.nbd.autofocus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;


import com.nbd.tofmodule.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private WorkHandlerThread mWorkHandlerThread;
    private Handler mUiHandler;//主线程的Handler
    private WorkHandler mWorkHandler;
    private static int mCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.e(TAG, "onCreate");
        Thread1();
    }

    private void Thread1() {
        HandlerThread mHandlerThread = new HandlerThread("WorkThread", Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start(); // 启动Loop
        mWorkHandler = new WorkHandler(mHandlerThread.getLooper()); // 线程与Handler关联
        mWorkHandler.setCurrentHandler(mWorkHandler);
        mWorkHandler.sendEmptyMessage(WorkHandler.TYPE_INIT);
    }

    private void Thread2() {
        mWorkHandlerThread = new WorkHandlerThread("WorkHandlerThread");
        mWorkHandlerThread.start();
        mUiHandler = new Handler(mWorkHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.e(TAG, "WorkHandlerThread state : " + msg.what + " mCount " + mCount);
                switch (msg.what) {
                    case WorkHandlerThread.TYPE_FAIL:
                        Log.e(TAG, "open fail");
                        if (mUiHandler != null) {
                            mUiHandler.removeCallbacksAndMessages(null);
                            mUiHandler = null;
                        }
                        if (mWorkHandlerThread != null) {
                            mWorkHandlerThread.quitSafely();
                        }
                        break;
                    case WorkHandlerThread.TYPE_INIT:
                        mWorkHandlerThread.init();
                        mUiHandler.sendEmptyMessage(WorkHandlerThread.TYPE_GET);
                        break;
                    case WorkHandlerThread.TYPE_GET:
                        mWorkHandlerThread.getData();
                        try {
                            Thread.sleep(6000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mCount++;
                        mUiHandler.sendEmptyMessage(WorkHandlerThread.TYPE_GET);
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
        mWorkHandlerThread.setUIHandler(mUiHandler);
        mUiHandler.sendEmptyMessage(WorkHandlerThread.TYPE_INIT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
    }

    @Override
    protected void onDestroy() {
        if (mUiHandler != null) {
            mUiHandler.removeCallbacksAndMessages(null);
            mUiHandler = null;
        }
        if (mWorkHandlerThread != null) {
            mWorkHandlerThread.quitSafely();
        }
        if (mWorkHandler != null) {
            mWorkHandler.removeCallbacksAndMessages(null);
            mWorkHandler = null;
        }
        super.onDestroy();
    }
}