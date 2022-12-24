package com.nbd.autofocus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
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
    private TofService mTofService;
    Handler mHandler;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mTextView =  findViewById(R.id.sample_text);

        // 启动service
/*        Intent mIntent=new Intent(MainActivity.this, TofService.class) ;
        mIntent.putExtra("abc", 160927);
        startService(mIntent);*/
        //Thread1();

        //      绑定 service
        Intent bindIntent = new Intent(MainActivity.this, TofService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);


        mHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                mTextView.setText(String.valueOf(msg.what));
                super.handleMessage(msg);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        LogUtil.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onStop() {
        timer.cancel();
        unbindService(connection);
        LogUtil.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
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

    /**
     * 在主线程中运行
     * 把 service 链接起来
     * 拿到 service 对象
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个MsgService对象
            mTofService = ((TofService.TofBinder)service).getService();
            LogUtil.d(TAG, "onServiceConnected: " + name);
            timer = new Timer();
            //timer.schedule(task, 0); // 此处delay为0表示没有延迟，立即执行一次task
            //timer.schedule(task, 1000); // 延迟1秒，执行一次task
            timer.schedule(task, 200, 60); // 第二个参数是0：立即执行一次task，然后每隔500ms执行一次task
            //响应接口返回的数据
        }
    };

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            // 每隔一秒使用 handler发送一下消息,也就是每隔一秒执行一次,一直重复执行
            // 使用handler发送消息
            Message message = new Message();
            if (mTofService != null) {
                message.what = mTofService.getData()[36];
            }
            mHandler.sendMessage(message);
        }
    };

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