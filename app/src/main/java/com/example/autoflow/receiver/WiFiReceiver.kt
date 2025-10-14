package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WiFiReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WiFiReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            // Check WiFi permission
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "WiFi permission not granted")
                return
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo

            if (wifiInfo != null && wifiInfo.ssid != null) {
                val ssid = wifiInfo.ssid.removeSurrounding("\"")
                Log.d(TAG, "📶 Connected to WiFi: $ssid")

                val pendingResult = goAsync()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val workflows = db.workflowDao().getAllWorkflowsSync()

                        workflows.filter { it.isEnabled }.forEach { workflow ->
                            val triggers = workflow.toTriggers()

                            triggers.forEach { trigger ->
                                if (trigger is com.example.autoflow.model.Trigger.WiFiTrigger) {
                                    if (trigger.ssid == ssid) {
                                        Log.d(TAG, "✅ WiFi trigger matched for workflow: ${workflow.workflowName}")
                                        // ✅ FIXED: Use executeWorkflow instead of executeActions
                                        ActionExecutor.executeWorkflow(context, workflow)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing WiFi trigger", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
