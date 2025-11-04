package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ✅ COMPLETELY FIXED WiFiReceiver
 */
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
            WifiManager.WIFI_STATE_ENABLING -> "TURNING_ON"
            WifiManager.WIFI_STATE_DISABLING -> "TURNING_OFF"
            else -> "UNKNOWN"
        }

        Log.d(TAG, "WiFi state: $stateString")
        checkWifiWorkflows(context, stateString, null)
    }

    private fun handleNetworkStateChange(context: Context, intent: Intent) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo
        val ssid = connectionInfo?.ssid?.replace("\"", "") ?: ""

        if (ssid.isNotEmpty() && ssid != "<unknown ssid>") {
            Log.d(TAG, "Connected to WiFi: $ssid")
            checkWifiWorkflows(context, "CONNECTED", ssid)
        }
    }

    private fun checkWifiWorkflows(context: Context, state: String, ssid: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val workflows = database.workflowDao().getAllSync()

                workflows.filter { it.isEnabled }.forEach { workflow ->
                    val triggers = workflow.toTriggers()
                    val matchingTrigger = triggers.find { trigger ->
                        trigger.type == "WIFI" && matchesWifiTrigger(trigger.value, state, ssid)
                    }

                    if (matchingTrigger != null) {
                        Log.d(TAG, "WiFi trigger matched for workflow: ${workflow.workflowName}")

                        // ✅ FIXED: Use ActionExecutor.executeWorkflow
                        val actionExecutor = ActionExecutor.getInstance()
                        actionExecutor.executeWorkflow(context, workflow)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking WiFi workflows", e)
            }
        }
    }

    private fun matchesWifiTrigger(triggerValue: String?, state: String, ssid: String?): Boolean {
        return try {
            val json = JSONObject(triggerValue ?: "{}")
            val targetState = json.optString("state", "").uppercase()
            val targetSsid = json.optString("ssid", "")

            val stateMatches = when (targetState) {
                "ON" -> state == "ON"
                "OFF" -> state == "OFF"
                "CONNECTED" -> state == "CONNECTED"
                else -> true
            }

            val ssidMatches = if (targetSsid.isEmpty()) {
                true
            } else {
                targetSsid == ssid
            }

            stateMatches && ssidMatches
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WiFi trigger value: $triggerValue", e)
            false
        }
    }
}
