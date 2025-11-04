package com.example.autoflow.receiver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ✅ COMPLETELY FIXED BluetoothReceiver
 */
class BluetoothReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📡 Bluetooth event: ${intent.action}")

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
            else -> "UNKNOWN"
        }

        Log.d(TAG, "Bluetooth state changed to: $stateString")
        checkBluetoothWorkflows(context, "STATE_CHANGE", stateString, null)
    }

    private fun handleDeviceConnected(context: Context, device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val deviceName = device.name ?: "Unknown"
        val deviceAddress = device.address

        Log.d(TAG, "Device connected: $deviceName ($deviceAddress)")
        checkBluetoothWorkflows(context, "CONNECTED", deviceName, deviceAddress)
    }

    private fun handleDeviceDisconnected(context: Context, device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val deviceName = device.name ?: "Unknown"
        val deviceAddress = device.address

        Log.d(TAG, "Device disconnected: $deviceName ($deviceAddress)")
        checkBluetoothWorkflows(context, "DISCONNECTED", deviceName, deviceAddress)
    }

    private fun checkBluetoothWorkflows(
        context: Context,
        eventType: String,
        deviceName: String?,
        deviceAddress: String?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val workflows = database.workflowDao().getAllSync()

                workflows.filter { it.isEnabled }.forEach { workflow ->
                    val triggers = workflow.toTriggers()
                    val matchingTrigger = triggers.find { trigger ->
                        trigger.type == "BLUETOOTH" && matchesBluetoothTrigger(
                            trigger.value,
                            eventType,
                            deviceName,
                            deviceAddress
                        )
                    }

                    if (matchingTrigger != null) {
                        Log.d(TAG, "Bluetooth trigger matched for workflow: ${workflow.workflowName}")

                        // ✅ FIXED: Use ActionExecutor.executeWorkflow
                        val actionExecutor = ActionExecutor.getInstance()
                        actionExecutor.executeWorkflow(context, workflow)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Bluetooth workflows", e)
            }
        }
    }

    private fun matchesBluetoothTrigger(
        triggerValue: String?,
        eventType: String,
        deviceName: String?,
        deviceAddress: String?
    ): Boolean {
        return try {
            val json = JSONObject(triggerValue ?: "{}")

            // Check device address/name match
            val targetAddress = json.optString("deviceAddress", "")
            val targetName = json.optString("deviceName", "")
            val triggerType = json.optString("triggerType", "CONNECTED")

            val deviceMatches = when {
                targetAddress.isNotEmpty() -> targetAddress == deviceAddress
                targetName.isNotEmpty() -> targetName == deviceName
                else -> true // Match any device
            }

            val eventMatches = when (triggerType.uppercase()) {
                "CONNECTED" -> eventType == "CONNECTED"
                "DISCONNECTED" -> eventType == "DISCONNECTED"
                "STATE_CHANGE" -> eventType == "STATE_CHANGE"
                else -> true
            }

            deviceMatches && eventMatches
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Bluetooth trigger value: $triggerValue", e)
            false
        }
    }
}
