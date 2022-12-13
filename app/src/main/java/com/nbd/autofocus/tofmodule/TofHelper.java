package com.nbd.autofocus.tofmodule;

import android.util.Log;

public class TofHelper {
    public static int test;
    private static TofHelper mTofHelper;

    private ParamInfo mParamInfo = new ParamInfo();
    public ParamInfo getmParamInfo() {
        return mParamInfo;
    }

    public static TofHelper getInstance() {
        synchronized (TofHelper.class) {
            if (mTofHelper == null) {
                Log.e("TofModule", "test static =======================");
                System.loadLibrary("tofmodule");
                mTofHelper = new TofHelper();
            }
        }
        return  mTofHelper;
    }

    public class ResultsData {
        public int status;
        public short[] distanceMm;
        public byte[] targetStatus;
    }

    public class ParamInfo {
        public int frequencyHz;
        public int resolutionValue;
        public int targetOrder;
        public int rangingMode;
        public int integrationTimeMs;
        public String device;
        public byte[] data;
    }

    public native byte setParamInfo(ParamInfo paramInfoSet);
    public native byte openModule();
    public native ResultsData getResultsData();
}
