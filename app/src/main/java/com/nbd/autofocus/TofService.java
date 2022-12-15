package com.nbd.autofocus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

import androidx.annotation.NonNull;

import com.nbd.autofocus.tofmodule.TofHelper;
import com.nbd.autofocus.utils.LogUtil;
import com.nbd.motorlibrary.JarTest;

public class TofService extends Service {
    private static final String TAG = TofService.class.getSimpleName();
    private TofHelper mTofHelper;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private static int mCount = 0;

    public static final int TYPE_FAIL = -1;
    public static final int TYPE_INIT = 0;
    public static final int TYPE_GET = 1;
    public static final int TYPE_LOOP_GET = 2;
    public static final int TYPE_EXIT = 3;

/*    enum StateMachine {
        public static final int TYPE_FAIL = -1;
        public static final int TYPE_INIT = 1;
        public static final int TYPE_GET = 2;
        public static final int TYPE_LOOP_GET = 0;
    }*/

    TofHelper.ParamInfo mParamInfo;
    ServiceReceiver  mServiceReceiver;

    public TofService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "setvalue " + JarTest.getvalue());
        Log.e(TAG, "onCreate");
        LogUtil.d(TAG, "-----------");
        mServiceReceiver = new ServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("tof.service.state");
        registerReceiver(mServiceReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand " + intent.getIntExtra("abc", 0));

        startForeground();
        startTofThread();
        mHandler.sendEmptyMessage(TYPE_INIT);
        JarTest.setvalue(6);
        Log.e(TAG, "setvalue " + JarTest.getvalue());
        return super.onStartCommand(intent, flags, startId);
    }

    public void startTofThread() {
        mHandlerThread = new HandlerThread("WorkHandlerThread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.e(TAG, "WorkHandlerThread state : " + msg.what + " mCount " + mCount);
                switch (msg.what) {
                    case TYPE_EXIT:
                        Log.e(TAG, "Thread Exit");
                        flushModule();
                        break;
                    case TYPE_FAIL:
                        Log.e(TAG, "Open Module Fail");
                        flushModule();
                        break;
                    case TYPE_INIT:
                        init();
                        mHandler.sendEmptyMessage(TYPE_GET);
                        break;
                    case TYPE_GET:
                        getData();
                        break;
                    case TYPE_LOOP_GET:
                        getData();
                        try {
                            Thread.sleep(6000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mCount++;
                        mHandler.sendEmptyMessage(TYPE_LOOP_GET);
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        flushModule();
        stopForeground();
        super.onDestroy();
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String CHANNEL_ONE_ID = getPackageName();
            String CHANNEL_ONE_NAME = "CHANNEL_ONE_NAME";
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ONE_ID, CHANNEL_ONE_NAME, IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            notificationChannel.setShowBadge(false);
            if (manager != null) {
                manager.createNotificationChannel(notificationChannel);
            }
            Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ONE_ID).build();
            startForeground(1, notification);
        }
    }

    private void stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
    }

    public byte init() {
        mTofHelper = TofHelper.getInstance();
        mParamInfo = mTofHelper.getmParamInfo();
        byte[] b = new byte[10];
        for (int i = 0; i < 9; i++) {
            b[i] = (byte) (i + 97);
        }
        mParamInfo.data = b;
        mParamInfo.device = "i2c0";
        mParamInfo.resolutionValue = 64;
        mParamInfo.rangingMode = 1;
        mParamInfo.frequencyHz = 15;
        mParamInfo.targetOrder = 1;
        if (mTofHelper.openModule() == 0) {
            mTofHelper.setParamInfo(mParamInfo);
            return -1;
        } else {
            mHandler.sendEmptyMessage(TYPE_FAIL);
        }
        return 0;
    }

    public int getData() {

        TofHelper.ResultsData mResultsData = mTofHelper.getResultsData();
        Log.i(TAG, "Zone : " + 6 +
                ", Target status : " + mResultsData.targetStatus[6] +
                ", distance : " + mResultsData.distanceMm[6]);
/*        for (int i = 0; i < mParamInfo.resolutionValue; i++) {
            Log.i(TAG, "Zone : " + i +
                    ", Target status : " + mResultsData.targetStatus[i] +
                    ", distance : " + mResultsData.distanceMm[i]);
        }*/
        return mResultsData.status;
    }

    public void flushModule() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
    }
    private class  ServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "------ " + intent.getStringExtra("operate"));
            if ("loop".equals(intent.getStringExtra("operate"))) {
                mHandler.sendEmptyMessage(TYPE_LOOP_GET);
            }
            switch (intent.getStringExtra("operate")) {
                case "loop":
                    mHandler.sendEmptyMessage(TYPE_LOOP_GET);
                    break;
                case "init":
                    mHandler.sendEmptyMessage(TYPE_INIT);
                    break;
                case "get":
                    mHandler.sendEmptyMessage(TYPE_GET);
                    break;
                case "exit":
                    mHandler.sendEmptyMessage(TYPE_EXIT);
                    break;
                default:
                    break;
            }
        }
    }
}