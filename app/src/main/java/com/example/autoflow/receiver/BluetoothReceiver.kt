package com.example.autoflow.receiver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import com.example.autoflow.util.TriggerParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ Event-Driven Bluetooth Receiver
 * Listens for System Broadcasts -> Checks ALL active workflows
 * Fires EVERY TIME Bluetooth state/connection changes and matches a trigger
 */
class BluetoothReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothReceiver"
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val action = intent.action

        Log.d(TAG, "📱 Bluetooth System Event: $action")

        // Determine Event Type
        val (currentState, device) = when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> "CONNECTED" to intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> "DISCONNECTED" to intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> "ON" to null
                    BluetoothAdapter.STATE_OFF -> "OFF" to null
                    else -> {
                        pendingResult.finish()
                        return // Ignore transition states
                    }
                }
            }
            else -> {
                pendingResult.finish()
                return
            }
        }

        val deviceAddress = device?.address
        val deviceName = device?.name
        Log.d(TAG, "🔍 State: $currentState, Device: $deviceName ($deviceAddress)")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val workflows = db.workflowDao().getActiveWorkflows()

                workflows.forEach { workflow ->
                    val triggers = workflow.toTriggers()

                    triggers.filter { it.type == "BLUETOOTH" }.forEach { trigger ->
                        val config = TriggerParser.parseBluetoothData(trigger)

                        if (config != null) {
                            // ✅ Logic Evaluation
                            var match = false

                            // 1. State Check
                            val configState = when {
                                currentState == "CONNECTED" && config.deviceAddress != null -> "CONNECTED"
                                currentState == "DISCONNECTED" && config.deviceAddress != null -> "DISCONNECTED"
                                currentState == "ON" -> "ON"
                                currentState == "OFF" -> "OFF"
                                else -> ""
                            }

                            if (configState.isNotEmpty()) {
                                match = true

                                // 2. Specific Device Check (Only if connected/disconnected)
                                if (currentState == "CONNECTED" || currentState == "DISCONNECTED") {
                                    // If trigger specifies a device, it MUST match
                                    if (!config.deviceAddress.isNullOrBlank()) {
                                        match = (config.deviceAddress == deviceAddress)
                                    }
                                }
                            }

                            if (match) {
                                Log.d(TAG, "✅ Trigger Matched: '${workflow.workflowName}'")
                                ActionExecutor.executeWorkflow(context, workflow)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking Bluetooth workflows", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
