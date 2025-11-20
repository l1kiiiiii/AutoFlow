package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import com.example.autoflow.util.TriggerParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ Event-Driven WiFi Receiver
 * Listens for System Broadcasts -> Checks ALL active workflows
 * Fires EVERY TIME the WiFi state changes and matches a trigger
 */
class WiFiReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WiFiReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // ✅ 1. Keep alive
        val pendingResult = goAsync()

        Log.d(TAG, "📶 System WiFi Event Received: ${intent.action}")

        // Determine current state immediately from System Service (Most accurate)
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isEnabled = wifiManager.isWifiEnabled

        val connectionInfo = wifiManager.connectionInfo
        val isConnected = connectionInfo != null && connectionInfo.networkId != -1 && connectionInfo.ssid != "<unknown ssid>"
        // Clean SSID (remove quotes)
        val currentSsid = connectionInfo?.ssid?.replace("\"", "") ?: ""

        val stateString = if (isConnected) "CONNECTED" else if (isEnabled) "ON" else "OFF"
        Log.d(TAG, "🔍 Current WiFi State: $stateString (SSID: $currentSsid)")

        // ✅ 2. Check ALL active workflows
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                // Get ONLY enabled workflows to save resources
                val activeWorkflows = db.workflowDao().getActiveWorkflows()

                activeWorkflows.forEach { workflow ->
                    val triggers = workflow.toTriggers()

                    // Filter for WiFi triggers in this workflow
                    triggers.filter { it.type == Constants.TRIGGER_WIFI }.forEach { trigger ->
                        val config = TriggerParser.parseWifiData(trigger)

                        if (config != null) {
                            // ✅ 3. Evaluate Logic
                            val match = when (config.state.uppercase()) {
                                "ON" -> isEnabled
                                "OFF" -> !isEnabled
                                "CONNECTED" -> isConnected && (config.ssid.isNullOrBlank() || config.ssid == currentSsid)
                                "DISCONNECTED" -> !isConnected
                                else -> false
                            }

                            if (match) {
                                Log.d(TAG, "✅ Trigger Matched: '${workflow.workflowName}'")
                                ActionExecutor.executeWorkflow(context, workflow)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking workflows", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
