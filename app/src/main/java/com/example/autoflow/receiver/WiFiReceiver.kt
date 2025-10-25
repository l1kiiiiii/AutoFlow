package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.*
import org.json.JSONObject

class WiFiReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WiFiReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📶 WiFi state changed: ${intent.action}")

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

        Log.d(TAG, "📶 WiFi state: $stateString")
        checkWifiTriggers(context, stateString, null)
    }

    private fun handleNetworkStateChange(context: Context, intent: Intent) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo

        if (connectionInfo != null && connectionInfo.ssid != null) {
            // Remove quotes from SSID
            val ssid = connectionInfo.ssid.replace("\"", "")
            Log.d(TAG, "📶 Connected to WiFi: $ssid")
            checkWifiTriggers(context, "CONNECTED", ssid)
        } else {
            Log.d(TAG, "📶 WiFi disconnected")
            checkWifiTriggers(context, "DISCONNECTED", null)
        }
    }

    private fun checkWifiTriggers(context: Context, state: String, connectedSsid: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val workflows = database.workflowDao().getActiveWorkflows()

                workflows.forEach { workflow ->
                    val triggers = workflow.toTriggers()

                    triggers.forEach { trigger ->
                        if (trigger.type == "WIFI" && matchesWifiTrigger(trigger.value, state, connectedSsid)) {
                            Log.d(TAG, "✅ WiFi trigger matched for workflow: ${workflow.workflowName}")

                            withContext(Dispatchers.Main) {
                                ActionExecutor.executeWorkflow(context, workflow)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking WiFi triggers", e)
            }
        }
    }

    /**
     * ✅ Check if trigger value matches current WiFi state
     */
    private fun matchesWifiTrigger(triggerValue: String, currentState: String, currentSsid: String?): Boolean {
        return try {
            val json = JSONObject(triggerValue)
            val targetState = json.optString("state", "")
            val targetSsid = json.optString("ssid", null)

            // Check state match
            if (targetState != currentState) {
                return false
            }

            // If trigger specifies SSID, check SSID match
            if (targetSsid != null) {
                return targetSsid == currentSsid
            }

            // State matches and no specific SSID required
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing WiFi trigger value: $triggerValue", e)
            false
        }
    }
}
