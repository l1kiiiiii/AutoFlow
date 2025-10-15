package com.example.autoflow.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothReceiver"
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        // Check Bluetooth permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Bluetooth permission not granted")
                return
            }
        }

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    val macAddress = it.address
                    val deviceName = it.name ?: "Unknown"

                    Log.d(TAG, "📲 Bluetooth connected: $deviceName ($macAddress)")

                    val pendingResult = goAsync()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val workflows = db.workflowDao().getAllWorkflowsSync()

                            workflows.filter { it.isEnabled }.forEach { workflow ->
                                val triggers = workflow.toTriggers()

                                triggers.forEach { trigger ->
                                    if (trigger is com.example.autoflow.model.Trigger.BluetoothTrigger) {
                                        if (trigger.deviceAddress == macAddress) {
                                            Log.d(TAG, "✅ Bluetooth trigger matched for workflow: ${workflow.workflowName}")
                                            // ✅ FIXED: Use executeWorkflow instead of executeActions
                                            ActionExecutor.executeWorkflow(context, workflow)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error executing Bluetooth trigger", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    Log.d(TAG, "📴 Bluetooth disconnected: ${it.name} (${it.address})")
                    // Handle disconnect triggers if needed
                }
            }
        }
    }
}
