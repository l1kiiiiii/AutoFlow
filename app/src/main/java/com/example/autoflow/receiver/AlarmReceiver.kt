package com.example.autoflow.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.compose.ui.test.isEnabled
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.autoflow.R
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.integrations.SoundModeManager
import com.example.autoflow.model.Action
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "autoflow_notifications"
        private const val CHANNEL_NAME = "AutoFlow Alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Extract workflow ID from intent
        val workflowId = intent.getLongExtra(Constants.KEY_WORKFLOW_ID, -1L)

        if (workflowId == -1L|| workflowId == 0L) {
            Log.e(TAG, "❌ Invalid workflow ID received")
            return
        }

        Log.d(TAG, "⏰ Alarm triggered for workflow: $workflowId")

        // Use goAsync() to allow coroutine to complete before receiver dies
        val pendingResult = goAsync()

        // Execute workflow in background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load workflow from database
                val database = AppDatabase.getDatabase(context)
                val workflow = database.workflowDao().getByIdSync(workflowId)  // ✅ FIXED: getByIdSync

                if (workflow == null) {
                    Log.e(TAG, "❌ Workflow $workflowId not found in database")
                    pendingResult.finish()
                    return@launch
                }

                // Check if workflow is enabled
                if (!workflow.isEnabled) {
                    Log.d(TAG, "⚠️ Workflow $workflowId is disabled, skipping execution")
                    pendingResult.finish()
                    return@launch
                }

                Log.d(TAG, "✅ Executing workflow: ${workflow.workflowName}")
                Log.d(TAG, "   Triggers: ${workflow.triggerDetails}")
                Log.d(TAG, "   Actions: ${workflow.actionDetails}")

                // Execute all actions in the workflow
                // ActionExecutor will parse the actionDetails JSON and execute each action
                // with its proper title, message, priority, etc.
                ActionExecutor.executeWorkflow(context, workflow)

                Log.d(TAG, "✅ Workflow execution completed")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error executing workflow $workflowId: ${e.message}", e)
            } finally {
                // Always finish the pending result
                pendingResult.finish()
            }
        }
    }

    private fun handleNotification(context: Context, intent: Intent) {
        val title = intent.getStringExtra("notification_title") ?: "AutoFlow"
        val message = intent.getStringExtra("notification_message") ?: "Trigger activated"

        Log.d(TAG, "📬 Sending notification: $title")
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d(TAG, "✅ Notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Notification permission denied", e)
            Toast.makeText(context, "Notification permission required", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleSoundMode(context: Context, intent: Intent) {
        val soundMode = intent.getStringExtra("sound_mode") ?: "Silent"
        Log.d(TAG, "🔊 Setting sound mode to: $soundMode")

        val soundModeManager = SoundModeManager(context)
        val success = soundModeManager.setSoundMode(soundMode)

        if (success) {
            Log.d(TAG, "✅ Sound mode changed to: $soundMode")
            Toast.makeText(context, "Sound mode: $soundMode", Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "❌ Failed to change sound mode")
            if (soundMode == "DND" && !soundModeManager.hasDNDPermission()) {
                Toast.makeText(context, "DND permission required", Toast.LENGTH_LONG).show()
                soundModeManager.openDNDSettings()
            } else {
                Toast.makeText(context, "Failed to set sound mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    /**
     * Modern WiFi toggle (Android 10+) - Uses notification with full-screen intent
     */
    private fun handleWiFiToggleModern(context: Context, wifiState: Boolean) {
        Log.d(TAG, "📶 Opening WiFi settings via notification")

        // Don't try to launch activity directly - it will be blocked
        // Instead, show a high-priority notification that user can tap
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
            Log.e(TAG, "❌ WiFi permission denied: ${e.message}", e)
            openWiFiSettings(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ WiFi toggle failed: ${e.message}", e)
            openWiFiSettings(context)
        }
    }

    /**
     * Show WiFi toggle notification with full-screen intent
     */
    private fun showWiFiToggleNotification(context: Context, wifiState: Boolean) {
        createNotificationChannel(context)

        val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            100,
            settingsIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ WiFi Toggle Required")
            .setContentText("Tap to ${if (wifiState) "turn ON" else "turn OFF"} WiFi")
            .setPriority(NotificationCompat.PRIORITY_MAX)  // ✅ Changed to MAX
            .setCategory(NotificationCompat.CATEGORY_ALARM)  // ✅ Added category
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // ✅ Show on lockscreen
            .addAction(
                0,
                if (wifiState) "Turn WiFi ON" else "Turn WiFi OFF",
                pendingIntent
            )
            .setFullScreenIntent(pendingIntent, true)  // ✅ Full-screen intent
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(200, notification)
            Log.d(TAG, "✅ WiFi toggle notification sent with full-screen intent")
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
            Log.e(TAG, "❌ Failed to open WiFi settings: ${e.message}", e)
        }
    }

    private fun handleBluetoothToggle(context: Context, intent: Intent) {
        val bluetoothStateStr = intent.getStringExtra("bluetooth_state") ?: "false"
        val bluetoothState = bluetoothStateStr.toBoolean()
        Log.d(TAG, "📶 Bluetooth toggle request: ${if (bluetoothState) "ON" else "OFF"}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            handleBluetoothToggleModern(context, bluetoothState)
        } else {
            handleBluetoothToggleLegacy(context, bluetoothState)
        }
    }
    /**
     * Modern Bluetooth toggle (Android 13+) - Uses notification with full-screen intent
     */
    private fun handleBluetoothToggleModern(context: Context, bluetoothState: Boolean) {
        Log.d(TAG, "📶 Opening Bluetooth settings via notification")

        // Don't try to launch activity directly - it will be blocked
        // Instead, show a high-priority notification that user can tap
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
            Log.e(TAG, "❌ Bluetooth permission denied: ${e.message}", e)
            openBluetoothSettings(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bluetooth toggle failed: ${e.message}", e)
            openBluetoothSettings(context)
        }
    }

    /**
     * Show Bluetooth toggle notification with full-screen intent
     */
    private fun showBluetoothToggleNotification(context: Context, bluetoothState: Boolean) {
        createNotificationChannel(context)

        val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            101,
            settingsIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ Bluetooth Toggle Required")
            .setContentText("Tap to ${if (bluetoothState) "turn ON" else "turn OFF"} Bluetooth")
            .setPriority(NotificationCompat.PRIORITY_MAX)  // ✅ Changed to MAX
            .setCategory(NotificationCompat.CATEGORY_ALARM)  // ✅ Added category
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // ✅ Show on lockscreen
            .addAction(
                0,
                if (bluetoothState) "Turn Bluetooth ON" else "Turn Bluetooth OFF",
                pendingIntent
            )
            .setFullScreenIntent(pendingIntent, true)  // ✅ Full-screen intent
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(201, notification)
            Log.d(TAG, "✅ Bluetooth toggle notification sent with full-screen intent")
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
            Log.e(TAG, "❌ Failed to open Bluetooth settings: ${e.message}", e)
        }
    }

    private fun handleScript(context: Context, intent: Intent) {
        val scriptText = intent.getStringExtra("script_text") ?: ""
        Log.d(TAG, "📜 Executing script: ${scriptText.take(50)}...")

        if (scriptText.isBlank()) {
            Log.w(TAG, "⚠️ Script is empty")
            Toast.makeText(context, "Script is empty", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Script execution not yet implemented", Toast.LENGTH_LONG).show()
        Log.w(TAG, "⚠️ Script execution not implemented for security reasons")
    }
    /**
     * Handle BLOCK_APPS action
     */
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

    /**
     * Handle UNBLOCK_APPS action
     */
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

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH  // Keep HIGH for notifications
            ).apply {
                description = "Notifications from AutoFlow workflows"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)  // ✅ Allow notifications even in DND
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}
