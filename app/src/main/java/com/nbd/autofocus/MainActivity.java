package com.nbd.autofocus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.nbd.tofmodule.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private WorkHandlerThread mWorkHandlerThread;
    private Handler mUiHandler;//主线程的Handler
    private static int mCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.e(TAG, "onCreate");

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
        super.onDestroy();
    }

}