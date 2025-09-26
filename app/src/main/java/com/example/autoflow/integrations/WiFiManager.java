package com.example.autoflow.integrations;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

/**
 * Enhanced WiFiManager for AutoFlow automation app
 * Provides WiFi state monitoring and control with proper security measures
 */
public class WiFiManager {
    private static final String TAG = "WiFiManager";
    private final Context context;
    private final WifiManager wifiManager;
    private WiFiCallback callback;
    private boolean isReceiverRegistered = false;

    public interface WiFiCallback {
        void onWifiStateChanged(boolean isEnabled);
        void onWifiConnected(@NonNull String ssid);
        void onWifiDisconnected();
        void onError(@NonNull String errorMessage);
    }

    public WiFiManager(@NonNull Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);

        if (this.wifiManager == null) {
            Log.e(TAG, "WifiManager service not available on this device");
        }
    }

    /**
     * Check if WiFi is enabled with proper permission handling
     * @return true if WiFi is enabled, false otherwise
     */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    public boolean isWiFiEnabled() {
        if (wifiManager == null) {
            Log.w(TAG, "WifiManager is null - device may not support WiFi");
            return false;
        }

        // Explicit permission check
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "ACCESS_WIFI_STATE permission not granted");
            return false;
        }

        try {
            return wifiManager.isWifiEnabled();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException checking WiFi state: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error checking WiFi state: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current WiFi state using getWifiState() for more detailed status
     * @return WiFi state constant or WIFI_STATE_UNKNOWN
     */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    public int getWiFiState() {
        if (wifiManager == null) {
            return WifiManager.WIFI_STATE_UNKNOWN;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "ACCESS_WIFI_STATE permission not granted for getWifiState");
            return WifiManager.WIFI_STATE_UNKNOWN;
        }

        try {
            return wifiManager.getWifiState();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException getting WiFi state: " + e.getMessage());
            return WifiManager.WIFI_STATE_UNKNOWN;
        }
    }

    /**
     * Check if required permissions are granted
     * @return true if all required permissions are granted
     */
    public boolean hasRequiredPermissions() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get required permissions array
     */
    @NonNull
    public String[] getRequiredPermissions() {
        return new String[]{
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };
    }

    /**
     * Start monitoring WiFi state changes with enhanced callback
     */
    public boolean startMonitoring(@Nullable WiFiCallback callback) {
        this.callback = callback;

        if (!hasRequiredPermissions()) {
            String error = "Required WiFi permissions not granted";
            Log.w(TAG, error);
            if (callback != null) {
                callback.onError(error);
            }
            return false;
        }

        try {
            if (!isReceiverRegistered) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

                context.registerReceiver(wifiStateReceiver, intentFilter);
                isReceiverRegistered = true;
                Log.d(TAG, "WiFi state receiver registered successfully");
                return true;
            }
        } catch (SecurityException e) {
            String error = "SecurityException registering WiFi receiver: " + e.getMessage();
            Log.e(TAG, error);
            if (callback != null) {
                callback.onError(error);
            }
            return false;
        } catch (Exception e) {
            String error = "Unexpected error registering WiFi receiver: " + e.getMessage();
            Log.e(TAG, error);
            if (callback != null) {
                callback.onError(error);
            }
            return false;
        }

        return true;
    }

    /**
     * Stop monitoring WiFi state changes with proper cleanup
     */
    public void stopMonitoring() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(wifiStateReceiver);
                Log.d(TAG, "WiFi state receiver unregistered successfully");
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "WiFi receiver was not registered: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering WiFi receiver: " + e.getMessage());
            } finally {
                isReceiverRegistered = false;
            }
        }

        callback = null;
    }

    /**
     * Enable/disable WiFi (deprecated in API 29+, included for compatibility)
     */
    @RequiresPermission(allOf = {
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
    })
    @Deprecated
    public boolean setWiFiEnabled(boolean enabled) {
        if (wifiManager == null) {
            Log.w(TAG, "WifiManager is null, cannot change WiFi state");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.w(TAG, "setWifiEnabled is deprecated and not functional on Android 10+");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "CHANGE_WIFI_STATE permission not granted");
            return false;
        }

        try {
            return wifiManager.setWifiEnabled(enabled);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException changing WiFi state: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error changing WiFi state: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current WiFi connection info
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    @Nullable
    public String getCurrentSSID() {
        if (wifiManager == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Location permission required for SSID access on Android 10+");
                return null;
            }
        }

        try {
            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                // Remove quotes from SSID
                if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                return ssid;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException getting WiFi info: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting WiFi info: " + e.getMessage());
        }

        return null;
    }

    /**
     * Clean up resources when WiFiManager is no longer needed
     */
    public void cleanup() {
        stopMonitoring();
    }

    /**
     * Enhanced BroadcastReceiver with security considerations and detailed state handling
     */
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (intent == null || intent.getAction() == null) {
                Log.w(TAG, "Received null or invalid intent in WiFi receiver");
                return;
            }

            if (callback == null) {
                Log.d(TAG, "WiFi state changed but no callback registered");
                return;
            }

            try {
                String action = intent.getAction();

                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                    int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    handleWifiStateChange(wifiState);
                } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                    handleNetworkStateChange(intent);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing WiFi broadcast: " + e.getMessage());
                if (callback != null) {
                    callback.onError("Error processing WiFi state change: " + e.getMessage());
                }
            }
        }

        private void handleWifiStateChange(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_STATE_ENABLED:
                    Log.d(TAG, "WiFi enabled");
                    callback.onWifiStateChanged(true);
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    Log.d(TAG, "WiFi disabled");
                    callback.onWifiStateChanged(false);
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                    Log.d(TAG, "WiFi enabling");
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    Log.d(TAG, "WiFi disabling");
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:
                default:
                    Log.w(TAG, "Unknown WiFi state: " + wifiState);
                    break;
            }
        }

        private void handleNetworkStateChange(Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ requires location permission for network info
                if (ActivityCompat.checkSelfPermission(WiFiManager.this.context,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            try {
                android.net.NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.getType() == android.net.ConnectivityManager.TYPE_WIFI) {
                    if (networkInfo.isConnected()) {
                        String ssid = getCurrentSSID();
                        if (ssid != null) {
                            Log.d(TAG, "WiFi connected to: " + ssid);
                            callback.onWifiConnected(ssid);
                        }
                    } else {
                        Log.d(TAG, "WiFi disconnected");
                        callback.onWifiDisconnected();
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in network state change: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error handling network state change: " + e.getMessage());
            }
        }
    };
}
