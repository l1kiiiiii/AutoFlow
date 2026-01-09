package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.*
import org.json.JSONObject

class WiFiReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WiFiReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                handleWifiStateChange(context, wifiState)
            }
            WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                handleNetworkStateChange(context, intent)
            }
        }
    }

    private fun handleWifiStateChange(context: Context, wifiState: Int) {
        val stateString = when (wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> "ON"
            WifiManager.WIFI_STATE_DISABLED -> "OFF"
            WifiManager.WIFI_STATE_ENABLING -> "ENABLING"
            WifiManager.WIFI_STATE_DISABLING -> "DISABLING"
            else -> return
        }
        checkWifiTriggers(context, stateString, "")
    }

    private fun handleNetworkStateChange(context: Context, intent: Intent) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Use ConnectivityManager for network type checking if possible, but for SSID we usually need WifiManager
        // Note: SSID access requires location permission on modern Android

        @Suppress("DEPRECATION")
        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)

        if (networkInfo?.isConnected == true) {
            // ✅ FIX: Explicitly suppress deprecation as we need a quick synchronous check
            @Suppress("DEPRECATION")
            val connectionInfo = wifiManager.connectionInfo

            if (connectionInfo != null && connectionInfo.ssid != null) {
                val ssid = connectionInfo.ssid.replace("\"", "")
                Log.d(TAG, "📶 Connected to WiFi: $ssid")
                checkWifiTriggers(context, "CONNECTED", ssid)
            }
        } else if (networkInfo?.isConnected == false) {
            Log.d(TAG, "📶 WiFi disconnected")
            checkWifiTriggers(context, "DISCONNECTED", "")
        }
    }

    private fun checkWifiTriggers(context: Context, state: String, connectedSsid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                // Use Repository pattern
                val repository = WorkflowRepository(database.workflowDao())
                val workflows = repository.getAllWorkflowsList()

                workflows.forEach { workflow ->
                    if (workflow.isEnabled) {
                        val triggers = workflow.toTriggers()
                        triggers.forEach { trigger ->
                            if (trigger.type == "WIFI" && matchesWifiTrigger(trigger.value, state, connectedSsid)) {
                                withContext(Dispatchers.Main) {
                                    ActionExecutor.executeWorkflow(context, workflow)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking WiFi triggers", e)
            }
        }
    }

    private fun matchesWifiTrigger(triggerValue: String, currentState: String, currentSsid: String): Boolean {
        return try {
            val json = JSONObject(triggerValue)
            val targetState = json.optString("state", "")
            val targetSsid = json.optString("ssid", "")

            if (targetState.isNotEmpty() && targetState != currentState) return false
            if (targetSsid.isNotEmpty() && targetSsid != currentSsid) return false

            true
        } catch (e: Exception) {
            false
        }
    }
}