
package com.example.autoflow.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.autoflow.R
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toActions
import com.example.autoflow.model.Action
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import com.example.autoflow.util.Constants.EXTRA_WORKFLOW_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ Refactored AlarmReceiver using Kotlin Coroutines
 *
 * Key Features:
 * - Uses goAsync() to keep receiver alive during DB operations
 * - Uses suspend functions instead of callbacks
 * - Proper coroutine scope management
 * - Handles workflow execution from AlarmScheduler
 * - Handles system toggles (WiFi/Bluetooth)
 * - Handles app blocking/unblocking
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "autoflow_notifications"
        private const val CHANNEL_NAME = "AutoFlow Alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 AlarmReceiver triggered")

        // 1. Handle Workflow Execution (from AlarmScheduler)
        if (intent.hasExtra(EXTRA_WORKFLOW_ID)) {
            val workflowId = intent.getLongExtra(EXTRA_WORKFLOW_ID, -1L)
            handleWorkflowExecution(context, workflowId)
            return
        }

        // 2. Handle System Toggles (WiFi/Bluetooth)
        // (These run synchronously, so no need for goAsync)
        if (intent.hasExtra("wifi_state")) {
            handleWiFiToggle(context, intent)
        } else if (intent.hasExtra("bluetooth_state")) {
            handleBluetoothToggle(context, intent)
        } else if (intent.hasExtra("app_packages")) {
            handleBlockApps(context, intent)
        } else if (intent.action == Constants.ACTION_UNBLOCK_APPS || intent.action == "com.example.autoflow.UNBLOCK_APPS") {
            handleUnblockApps(context, intent)
        }
    }

    /**
     * ✅ Handle workflow execution with goAsync() for reliability
     * Uses suspend functions instead of callbacks
     */
    private fun handleWorkflowExecution(context: Context, workflowId: Long) {
        if (workflowId <= 0) {
            Log.e(TAG, "❌ Invalid workflow ID received: $workflowId")
            return
        }

        // ✅ CRITICAL: Use goAsync() to keep the Receiver alive during DB operations
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "⏰ Processing alarm for workflow ID: $workflowId")

                val database = AppDatabase.getDatabase(context)
                val repository = WorkflowRepository(database.workflowDao())

                // ✅ FIX: Use Suspend function (No Callback)
                val workflow = repository.getWorkflowById(workflowId)

                if (workflow == null) {
                    Log.w(TAG, "⚠️ Workflow $workflowId not found")
                    return@launch
                }

                if (!workflow.isEnabled) {
                    Log.w(TAG, "⚠️ Workflow '${workflow.workflowName}' is disabled")
                    return@launch
                }

                Log.d(TAG, "✅ Executing workflow: '${workflow.workflowName}'")

                val actions = workflow.toActions()
                var successCount = 0
                var failCount = 0

                actions.forEach { action ->
                    try {
                        val success = ActionExecutor.executeAction(context, action)
                        if (success) {
                            successCount++
                            Log.d(TAG, "✅ Action executed: ${action.type}")
                        } else {
                            failCount++
                            Log.w(TAG, "⚠️ Action failed: ${action.type}")
                        }
                    } catch (e: Exception) {
                        failCount++
                        Log.e(TAG, "❌ Action error: ${action.type}", e)
                    }
                }

                Log.d(TAG, "🎉 Workflow completed: '${workflow.workflowName}'")
                Log.d(TAG, "   ✅ Success: $successCount actions")
                Log.d(TAG, "   ❌ Failed: $failCount actions")

                // Optional: Show toast on Main thread
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        Toast.makeText(
                            context,
                            "Executed: ${workflow.workflowName} ($successCount/${actions.size})",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        // Ignore UI errors in background
                        Log.d(TAG, "Toast skipped (UI not available)")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in AlarmReceiver", e)
            } finally {
                // ✅ CRITICAL: Must call finish() to release the wake lock
                pendingResult.finish()
                Log.d(TAG, "✅ PendingResult finished")
            }
        }
    }

    // =========================================================================
    // SYSTEM TOGGLE HANDLERS
    // =========================================================================

    private fun handleWiFiToggle(context: Context, intent: Intent) {
        val wifiStateStr = intent.getStringExtra("wifi_state") ?: "false"
        val wifiState = wifiStateStr.toBoolean()
        Log.d(TAG, "📶 WiFi toggle request: ${if (wifiState) "ON" else "OFF"}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleWiFiToggleModern(context, wifiState)
        } else {
            handleWiFiToggleLegacy(context, wifiState)
        }
    }

    private fun handleWiFiToggleModern(context: Context, wifiState: Boolean) {
        Log.d(TAG, "📶 Opening WiFi settings via notification (Android 10+)")
        showWiFiToggleNotification(context, wifiState)
        Toast.makeText(
            context,
            "Tap notification to toggle WiFi ${if (wifiState) "ON" else "OFF"}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun handleWiFiToggleLegacy(context: Context, wifiState: Boolean) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = wifiState
            Log.d(TAG, "✅ WiFi ${if (wifiState) "enabled" else "disabled"}")
            Toast.makeText(context, "WiFi ${if (wifiState) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ WiFi permission denied", e)
            openWiFiSettings(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ WiFi toggle failed", e)
            openWiFiSettings(context)
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun showWiFiToggleNotification(context: Context, wifiState: Boolean) {
        createNotificationChannel(context)

        val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            100,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ WiFi Toggle Required")
            .setContentText("Tap to ${if (wifiState) "turn ON" else "turn OFF"} WiFi")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                0,
                if (wifiState) "Turn WiFi ON" else "Turn WiFi OFF",
                pendingIntent
            )
            .setFullScreenIntent(pendingIntent, true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(200, notification)
            Log.d(TAG, "✅ WiFi toggle notification sent")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Notification permission denied", e)
        }
    }

    private fun openWiFiSettings(context: Context) {
        try {
            val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(wifiIntent)
            Toast.makeText(context, "Please toggle WiFi manually", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open WiFi settings", e)
        }
    }

    // =========================================================================
    // BLUETOOTH HANDLERS
    // =========================================================================

    private fun handleBluetoothToggle(context: Context, intent: Intent) {
        val bluetoothStateStr = intent.getStringExtra("bluetooth_state") ?: "false"
        val bluetoothState = bluetoothStateStr.toBoolean()
        Log.d(TAG, "📡 Bluetooth toggle request: ${if (bluetoothState) "ON" else "OFF"}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            handleBluetoothToggleModern(context, bluetoothState)
        } else {
            handleBluetoothToggleLegacy(context, bluetoothState)
        }
    }

    private fun handleBluetoothToggleModern(context: Context, bluetoothState: Boolean) {
        Log.d(TAG, "📡 Opening Bluetooth settings via notification (Android 13+)")
        showBluetoothToggleNotification(context, bluetoothState)
        Toast.makeText(
            context,
            "Tap notification to toggle Bluetooth ${if (bluetoothState) "ON" else "OFF"}",
            Toast.LENGTH_LONG
        ).show()
    }

    @SuppressLint("MissingPermission")
    private fun handleBluetoothToggleLegacy(context: Context, bluetoothState: Boolean) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter == null) {
                Log.e(TAG, "❌ Bluetooth adapter not available")
                Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_SHORT).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    Log.e(TAG, "❌ BLUETOOTH_CONNECT permission not granted")
                    openBluetoothSettings(context)
                    return
                }
            }

            @Suppress("DEPRECATION")
            if (bluetoothState) {
                adapter.enable()
            } else {
                adapter.disable()
            }

            Log.d(TAG, "✅ Bluetooth ${if (bluetoothState) "enabled" else "disabled"}")
            Toast.makeText(context, "Bluetooth ${if (bluetoothState) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Bluetooth permission denied", e)
            openBluetoothSettings(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bluetooth toggle failed", e)
            openBluetoothSettings(context)
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun showBluetoothToggleNotification(context: Context, bluetoothState: Boolean) {
        createNotificationChannel(context)

        val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ Bluetooth Toggle Required")
            .setContentText("Tap to ${if (bluetoothState) "turn ON" else "turn OFF"} Bluetooth")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                0,
                if (bluetoothState) "Turn Bluetooth ON" else "Turn Bluetooth OFF",
                pendingIntent
            )
            .setFullScreenIntent(pendingIntent, true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(201, notification)
            Log.d(TAG, "✅ Bluetooth toggle notification sent")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Notification permission denied", e)
        }
    }

    private fun openBluetoothSettings(context: Context) {
        try {
            val bluetoothIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(bluetoothIntent)
            Toast.makeText(context, "Please toggle Bluetooth manually", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open Bluetooth settings", e)
        }
    }

    // =========================================================================
    // APP BLOCKING HANDLERS
    // =========================================================================

    private fun handleBlockApps(context: Context, intent: Intent) {
        val packages = intent.getStringExtra("app_packages") ?: ""

        Log.d(TAG, "🚫 Executing BLOCK_APPS action")
        Log.d(TAG, "   Packages: $packages")

        if (packages.isBlank()) {
            Log.w(TAG, "⚠️ No packages to block")
            return
        }

        val action = Action(Constants.ACTION_BLOCK_APPS, null, null, null).apply {
            value = packages
        }

        val success = ActionExecutor.executeAction(context, action)

        if (success) {
            Log.i(TAG, "✅ Block apps action executed successfully")
            Toast.makeText(context, "Apps blocked", Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "❌ Block apps action failed")
            Toast.makeText(context, "Failed to block apps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUnblockApps(context: Context, intent: Intent) {
        Log.d(TAG, "🔓 Executing UNBLOCK_APPS action")

        val action = Action(Constants.ACTION_UNBLOCK_APPS, null, null, null)

        val success = ActionExecutor.executeAction(context, action)

        if (success) {
            Log.i(TAG, "✅ Unblock apps action executed successfully")
            Toast.makeText(context, "Apps unblocked", Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "❌ Unblock apps action failed")
            Toast.makeText(context, "Failed to unblock apps", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================================
    // NOTIFICATION CHANNEL SETUP
    // =========================================================================

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from AutoFlow workflows"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}