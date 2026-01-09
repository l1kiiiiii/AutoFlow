package com.example.autoflow.receiver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowRepository
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
                // ✅ FIX: Use the version-safe helper function
                val device = intent.getParcelableExtraCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let { handleDeviceConnected(context, it) }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                // ✅ FIX: Use the version-safe helper function
                val device = intent.getParcelableExtraCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let { handleDeviceDisconnected(context, it) }
            }
        }
    }

    // ✅ NEW: Clean Code Helper for Parcelables
    private inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
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
        // Check triggers with empty device info
        checkBluetoothTriggers(context, stateString, "", "")
    }

    private fun handleDeviceConnected(context: Context, device: BluetoothDevice) {
        val address = device.address ?: return
        saveDeviceState(context, address, true)
        if (hasBluetoothPermission(context)) {
            try {
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address ?: ""
                checkBluetoothTriggers(context, "CONNECTED", deviceAddress, deviceName)
            } catch (e: SecurityException) {
                checkBluetoothTriggers(context, "CONNECTED", device.address ?: "", "Unknown Device")
            }
        } else {
            checkBluetoothTriggers(context, "CONNECTED", device.address ?: "", "Unknown Device")
        }
    }

    private fun handleDeviceDisconnected(context: Context, device: BluetoothDevice) {
        val address = device.address ?: return

        //  SAVE STATE: Mark this device as disconnected
        saveDeviceState(context, address, false)
        if (hasBluetoothPermission(context)) {
            try {
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address ?: ""
                checkBluetoothTriggers(context, "DISCONNECTED", deviceAddress, deviceName)
            } catch (e: SecurityException) {
                checkBluetoothTriggers(context, "DISCONNECTED", device.address ?: "", "Unknown Device")
            }
        } else {
            checkBluetoothTriggers(context, "DISCONNECTED", device.address ?: "", "Unknown Device")
        }
    }
    private fun saveDeviceState(context: Context, address: String, isConnected: Boolean) {
        val prefs = context.getSharedPreferences("bt_device_states", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(address, isConnected).apply()
        Log.d(TAG, "💾 Saved BT State: $address = $isConnected")
    }
    private fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkBluetoothTriggers(context: Context, state: String, deviceAddress: String, deviceName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                // Use the Repository pattern here too for consistency
                val repository = WorkflowRepository(database.workflowDao())
                val workflows = repository.getAllWorkflowsList()

                workflows.forEach { workflow ->
                    if (workflow.isEnabled) {
                        val triggers = workflow.toTriggers()
                        triggers.forEach { trigger ->
                            if (trigger.type == "BLUETOOTH" && matchesBluetoothTrigger(trigger.value, state, deviceAddress, deviceName)) {
                                withContext(Dispatchers.Main) {
                                    ActionExecutor.executeWorkflow(context, workflow)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking Bluetooth triggers", e)
            }
        }
    }

    private fun matchesBluetoothTrigger(
        triggerValue: String,
        currentState: String,
        currentDeviceAddress: String,
        currentDeviceName: String
    ): Boolean {
        return try {
            val json = JSONObject(triggerValue)
            val targetState = json.optString("state", "")
            val targetDeviceAddress = json.optString("deviceAddress", "")
            val targetDeviceName = json.optString("deviceName", "")

            if (targetState.isNotEmpty() && targetState != currentState) return false
            if (targetDeviceAddress.isNotEmpty() && targetDeviceAddress != currentDeviceAddress) return false
            if (targetDeviceName.isNotEmpty() && targetDeviceName != currentDeviceName) return false

            true
        } catch (e: Exception) {
            false
        }
    }
}