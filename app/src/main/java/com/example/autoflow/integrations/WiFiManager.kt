package com.example.autoflow.integrations

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat

class WiFiManager(context: Context) {

    private val context: Context = context.applicationContext
    private val wifiManager: WifiManager? = this.context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var callback: WiFiCallback? = null
    private var isReceiverRegistered = false

    companion object {
        private const val TAG = "WiFiManager"
    }

    init {
        if (wifiManager == null) {
            Log.e(TAG, "WifiManager service not available")
        }
    }

    //  CALLBACK INTERFACE 

    interface WiFiCallback {
        fun onWifiStateChanged(isEnabled: Boolean)
        fun onWifiConnected(ssid: String)
        fun onWifiDisconnected()
        fun onError(errorMessage: String)
    }

    //  PUBLIC METHODS 

    fun isWiFiEnabled(): Boolean {
        if (wifiManager == null) {
            Log.w(TAG, "WifiManager is null")
            return false
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "ACCESS_WIFI_STATE permission not granted")
            return false
        }

        return try {
            wifiManager.isWifiEnabled
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            false
        }
    }

    fun getWiFiState(): Int {
        if (wifiManager == null) return WifiManager.WIFI_STATE_UNKNOWN

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            return WifiManager.WIFI_STATE_UNKNOWN
        }

        return try {
            wifiManager.wifiState
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            WifiManager.WIFI_STATE_UNKNOWN
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
    }

    fun startMonitoring(callback: WiFiCallback?): Boolean {
        this.callback = callback

        if (!hasRequiredPermissions()) {
            val error = "Required WiFi permissions not granted"
            Log.w(TAG, error)
            callback?.onError(error)
            return false
        }

        return try {
            if (!isReceiverRegistered) {
                val intentFilter = IntentFilter().apply {
                    addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                    addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                }
                context.registerReceiver(wifiStateReceiver, intentFilter)
                isReceiverRegistered = true
                Log.d(TAG, "✅ WiFi receiver registered")
                true
            } else {
                true
            }
        } catch (e: SecurityException) {
            val error = "SecurityException: ${e.message}"
            Log.e(TAG, error)
            callback?.onError(error)
            false
        } catch (e: Exception) {
            val error = "Unexpected error: ${e.message}"
            Log.e(TAG, error)
            callback?.onError(error)
            false
        }
    }

    fun stopMonitoring() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(wifiStateReceiver)
                Log.d(TAG, "✅ WiFi receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.d(TAG, "Receiver already unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering: ${e.message}")
            } finally {
                isReceiverRegistered = false
            }
        }
        callback = null
    }

    @Deprecated("Deprecated in API 29+")
    fun setWiFiEnabled(enabled: Boolean): Boolean {
        if (wifiManager == null) {
            Log.w(TAG, "WifiManager is null")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.w(TAG, "setWifiEnabled is deprecated on Android 10+")
            return false
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "CHANGE_WIFI_STATE permission not granted")
            return false
        }

        return try {
            @Suppress("DEPRECATION")
            wifiManager.setWifiEnabled(enabled)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            false
        }
    }

    fun getCurrentSSID(): String? {
        if (wifiManager == null) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Location permission required on Android 10+")
                return null
            }
        }

        return try {
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo?.ssid?.removeSurrounding("\"")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            null
        }
    }

    fun cleanup() {
        stopMonitoring()
    }

    //  BROADCAST RECEIVER 

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == null) {
                Log.w(TAG, "Received null intent")
                return
            }

            if (callback == null) {
                Log.d(TAG, "No callback registered")
                return
            }

            try {
                when (intent.action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                        handleWifiStateChange(wifiState)
                    }
                    WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                        handleNetworkStateChange(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing broadcast: ${e.message}")
                callback?.onError("Error processing WiFi state change: ${e.message}")
            }
        }
    }

    private fun handleWifiStateChange(wifiState: Int) {
        when (wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> {
                Log.d(TAG, "WiFi enabled")
                callback?.onWifiStateChanged(true)
            }
            WifiManager.WIFI_STATE_DISABLED -> {
                Log.d(TAG, "WiFi disabled")
                callback?.onWifiStateChanged(false)
            }
            WifiManager.WIFI_STATE_ENABLING -> Log.d(TAG, "WiFi enabling")
            WifiManager.WIFI_STATE_DISABLING -> Log.d(TAG, "WiFi disabling")
            else -> Log.w(TAG, "Unknown WiFi state: $wifiState")
        }
    }

    private fun handleNetworkStateChange(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        try {
            @Suppress("DEPRECATION")
            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)

            if (networkInfo?.type == ConnectivityManager.TYPE_WIFI) {
                if (networkInfo.isConnected) {
                    val ssid = getCurrentSSID()
                    if (ssid != null) {
                        Log.d(TAG, "WiFi connected to: $ssid")
                        callback?.onWifiConnected(ssid)
                    }
                } else {
                    Log.d(TAG, "WiFi disconnected")
                    callback?.onWifiDisconnected()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
        }
    }
}
