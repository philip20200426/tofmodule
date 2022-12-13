package com.nbd.autofocus;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.nbd.autofocus.tofmodule.TofHelper;

import java.util.logging.LogRecord;

public  class WorkHandler extends Handler {

    private static final String TAG =WorkHandler.class.getSimpleName();

    public static final int TYPE_FAIL = -1;
    public static final int TYPE_INIT = 1;
    public static final int TYPE_GET = 2;
    private Handler mMainHandler;
    private Handler mCurrentHandler;

    TofHelper mTofHelper;
    TofHelper.ParamInfo mParamInfo;
    private static int mCount = 0;

    public WorkHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        Log.e(TAG, "WorkHandlerThread state : " + msg.what + " mCount " + mCount);
        switch (msg.what) {
            case WorkHandlerThread.TYPE_FAIL:
                Log.e(TAG, "open fail");
                if (mCurrentHandler != null) {
                    mCurrentHandler.removeCallbacksAndMessages(null);
                    mCurrentHandler = null;
                }
                break;
            case WorkHandlerThread.TYPE_INIT:
                init();
                mCurrentHandler.sendEmptyMessage(TYPE_GET);
                break;
            case WorkHandlerThread.TYPE_GET:
                getData();
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mCount++;
                mCurrentHandler.sendEmptyMessage(TYPE_GET);
                break;
            default:
                break;
        }
        super.handleMessage(msg);
    }

    //注入主线程Handler
    public void setUIHandler(Handler Mainhandler) {
        mMainHandler = Mainhandler;
        Log.e(TAG, "setUIHandler: 2.主线程的handler传入到Download线程");
    }

    //注入主线程Handler
    public void setCurrentHandler(Handler mHandler) {
        mCurrentHandler = mHandler;
        Log.e(TAG, "setUIHandler: 2.主线程的handler传入到Download线程");
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
            mCurrentHandler.sendEmptyMessage(TYPE_FAIL);
        }
        return 0;
    }
}
