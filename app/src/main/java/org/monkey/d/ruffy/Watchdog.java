package org.monkey.d.ruffy;


/**
 * Created by adrian on 03/05/18.
 */


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;


/*
 * In order to stop Ruffy hard, send the following broadcast:
 *
       final Intent intent = new Intent("org.monkey.d.ruffy.ruffy.WATCHDOG");
       final Bundle bundle = new Bundle();
       bundle.putString("uniquePayload", "" + System.currentTimeMillis());
       intent.putExtras(bundle).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
       MainApp.instance().sendBroadcast(intent);
 */


public class Watchdog extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processes = ((ActivityManager) manager).getRunningAppProcesses();
            for (int i = 0; i < processes.size(); i++) {
                if(processes.get(i).processName.equals("org.monkey.d.ruffy.ruffy.driver.Ruffy")) {
                    android.os.Process.killProcess(processes.get(i).pid);
                }
            }
        }
    }
}
