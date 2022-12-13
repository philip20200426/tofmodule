package com.nbd.autofocus;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import com.nbd.autofocus.tofmodule.TofHelper;

public class WorkHandlerThread extends HandlerThread {
    //private static final String TAG = "TofThread";
    private static final String TAG =WorkHandlerThread.class.getSimpleName();

    public static final int TYPE_FAIL = -1;
    public static final int TYPE_INIT = 1;
    public static final int TYPE_GET = 2;
    private Handler mMainHandler;

    TofHelper mTofHelper;
    TofHelper.ParamInfo mParamInfo;

    public WorkHandlerThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        Log.e(TAG, "run run run");
        super.run();
        Log.e(TAG, "run run run----");
    }

    //注入主线程Handler
    public void setUIHandler(Handler Mainhandler) {
        mMainHandler = Mainhandler;
        Log.e(TAG, "setUIHandler: 2.主线程的handler传入到Download线程");
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
            mMainHandler.sendEmptyMessage(TYPE_FAIL);
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

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public boolean quit() {
        return super.quit();
    }



}
