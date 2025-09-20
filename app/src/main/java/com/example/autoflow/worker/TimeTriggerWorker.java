// worker/TimeTriggerWorker.java
package com.example.autoflow.worker;

import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.net.wifi.WifiManager;

public class TimeTriggerWorker extends Worker {
    public TimeTriggerWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        String actionType = getInputData().getString("actionType");
        if ("TOGGLE_WIFI".equals(actionType)) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);
        }
        return Result.success();
    }
}