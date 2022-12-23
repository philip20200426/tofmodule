package com.nbd.autofocus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.TextView;


import com.nbd.autofocus.utils.LogUtil;
import com.nbd.tofmodule.R;
import com.nbd.tofmodule.databinding.ActivityMainBinding;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private WorkHandlerThread mWorkHandlerThread;
    private Handler mUiHandler;//主线程的Handler
    private WorkHandler mWorkHandler;
    private static int mCount = 0;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mTextView =  findViewById(R.id.sample_text);

        // 启动service
        Intent mIntent=new Intent(MainActivity.this, TofService.class) ;
        mIntent.putExtra("abc", 160927);
        startService(mIntent);
        //Thread1();

        Handler mHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                mTextView.setText(String.valueOf(mCount++));
                super.handleMessage(msg);
            }
        };
        //每隔一秒使用 handler发送一下消息,也就是每隔一秒执行一次,一直重复执行
        Timer timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //使用handler发送消息
                Message message = new Message();
                mHandler.sendMessage(message);
            }
        },0,1000);//每 1s执行一次
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
}