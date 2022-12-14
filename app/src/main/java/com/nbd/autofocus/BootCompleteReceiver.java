package com.nbd.autofocus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";
    // 在系统层面，在Android 10 (API 级别 29) 及更高版本对后台应用启动Activity进行了限制。
    // Android10中, 当App的Activity不在前台时，其启动Activity会被系统拦截，导致无法启动。
    // 解决方式：修改系统源码或者代码里申请权限，但是需要用户点击接受。
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent mIntent = new Intent(context, TofService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Android Boot Completed !!!");
                context.startForegroundService(mIntent);
            }
        }
    }
}