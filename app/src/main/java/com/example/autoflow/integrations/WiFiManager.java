package com.example.autoflow.integrations;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

public class WiFiManager {
    private final Context context;
    private final WifiManager wifiManager;
    private WiFiCallback callback;

    public interface WiFiCallback {
        void onWifiStateChanged(boolean isConnected);
    }

    public WiFiManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void startMonitoring(WiFiCallback callback) {
        this.callback = callback;
        IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiStateReceiver, intentFilter);
    }

    public void stopMonitoring() {
        try {
            context.unregisterReceiver(wifiStateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }

    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                if (callback != null) {
                    callback.onWifiStateChanged(wifiState == WifiManager.WIFI_STATE_ENABLED);
                }
            }
        }
    };
}