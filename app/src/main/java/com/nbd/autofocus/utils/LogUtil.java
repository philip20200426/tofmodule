package com.nbd.autofocus.utils;

import android.util.Log;

public class LogUtil {
    private static final String TAG = LogUtil.class.getSimpleName();
    static boolean isOpenDebug = true;

    public static void v(String module, String msg) {
        if (isOpenDebug) {
            Log.v(module, msg);
        }
    }

    public static void i(String module, String msg) {
        if (isOpenDebug) {
            Log.i(module, msg);
        }
    }

    public static void d(String module, String msg) {
        if (isOpenDebug) {
            Log.d(module, msg);
        }
    }


    public static void e(String module, String msg) {
        Log.e(module, msg);
    }


}
