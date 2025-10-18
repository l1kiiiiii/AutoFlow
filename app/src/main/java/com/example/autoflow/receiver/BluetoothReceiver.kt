package com.example.autoflow.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.*
import org.json.JSONObject

class BluetoothReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📱 Bluetooth event: ${intent.action}")

        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                handleBluetoothStateChange(context, state)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let { handleDeviceConnected(context, it) }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let { handleDeviceDisconnected(context, it) }
            }
        }
    }

    private fun handleBluetoothStateChange(context: Context, state: Int) {
        val stateString = when (state) {
            BluetoothAdapter.STATE_ON -> "ON"
            BluetoothAdapter.STATE_OFF -> "OFF"
            BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
            BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
            else -> return
        }

        Log.d(TAG, "📱 Bluetooth state: $stateString")
        checkBluetoothTriggers(context, stateString, null, null)
    }

    private fun handleDeviceConnected(context: Context, device: BluetoothDevice) {
        Log.d(TAG, "📱 Device connected: ${device.name} (${device.address})")
        checkBluetoothTriggers(context, "CONNECTED", device.address, device.name)
    }

    private fun handleDeviceDisconnected(context: Context, device: BluetoothDevice) {
        Log.d(TAG, "📱 Device disconnected: ${device.name} (${device.address})")
        checkBluetoothTriggers(context, "DISCONNECTED", device.address, device.name)
    }

    private fun checkBluetoothTriggers(context: Context, state: String, deviceAddress: String?, deviceName: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val workflows = database.workflowDao().getActiveWorkflows()

                workflows.forEach { workflow ->
                    val triggers = workflow.toTriggers()

                    triggers.forEach { trigger ->
                        if (trigger.type == "BLUETOOTH" && matchesBluetoothTrigger(trigger.value, state, deviceAddress, deviceName)) {
                            Log.d(TAG, "✅ Bluetooth trigger matched for workflow: ${workflow.workflowName}")

                            withContext(Dispatchers.Main) {
                                ActionExecutor.executeWorkflow(context, workflow)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking Bluetooth triggers", e)
            }
        }
    }

    /**
     * ✅ Check if trigger value matches current Bluetooth state/device
     */
    private fun matchesBluetoothTrigger(
        triggerValue: String,
        currentState: String,
        currentDeviceAddress: String?,
        currentDeviceName: String?
    ): Boolean {
        return try {
            val json = JSONObject(triggerValue)
            val targetState = json.optString("state", "")
            val targetDeviceAddress = json.optString("deviceAddress", null)
            val targetDeviceName = json.optString("deviceName", null)

            // Check state match (if specified)
            if (targetState.isNotEmpty() && targetState != currentState) {
                return false
            }

            // Check device address match (if specified)
            if (targetDeviceAddress != null && targetDeviceAddress != currentDeviceAddress) {
                return false
            }

            // Check device name match (if specified)
            if (targetDeviceName != null && targetDeviceName != currentDeviceName) {
                return false
            }

            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing Bluetooth trigger value: $triggerValue", e)
            false
        }
    }
}
