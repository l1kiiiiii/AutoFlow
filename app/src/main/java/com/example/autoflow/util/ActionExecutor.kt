package com.example.autoflow.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toActions
import com.example.autoflow.integrations.PhoneStateManager
import com.example.autoflow.model.Action
import com.example.autoflow.policy.BlockPolicy
import com.example.autoflow.receiver.AlarmReceiver

object ActionExecutor {

    private const val TAG = "ActionExecutor"
    private const val CHANNEL_ID_HIGH = "autoflow_high"
    private const val CHANNEL_ID_NORMAL = "autoflow_normal"
    private const val CHANNEL_ID_LOW = "autoflow_low"

    /**
     * ✅ Execute a single action
     */
    fun executeAction(context: Context, action: Action): Boolean {
        Log.d(TAG, "Executing action: ${action.type}")

        return try {
            when (action.type) {
                Constants.ACTION_SEND_NOTIFICATION -> {
                    val title = action.title ?: "AutoFlow"
                    val message = action.message ?: "Automation triggered"
                    val priority = action.priority ?: "Normal"
                    sendNotification(context, title, message, priority)
                }

                Constants.ACTION_BLOCK_APPS -> {
                    // ✅ FIXED: Don't send notification from blockApps()
                    // User should add a separate notification action if they want one
                    blockApps(
                        context = context,
                        packageNames = action.value ?: "",
                        durationMinutes = 0,
                        sendNotification = false  // ✅ Set to false to avoid duplicate
                    )
                }

                Constants.ACTION_UNBLOCK_APPS -> {
                    unblockApps(context)
                }

                Constants.ACTION_SET_SOUND_MODE -> {
                    val mode = action.value ?: "Normal"
                    setSoundMode(context, mode)
                }

                Constants.ACTION_TOGGLE_WIFI -> {
                    val state = action.value ?: "Toggle"
                    toggleWiFi(context, state)
                }

                Constants.ACTION_TOGGLE_BLUETOOTH -> {
                    val state = action.value ?: "Toggle"
                    toggleBluetooth(context, state)
                }

                Constants.ACTION_AUTO_REPLY_SMS -> {
                    executeAutoReplySms(context, action)
                }

                else -> {
                    Log.w(TAG, "Unknown action type: ${action.type}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action ${action.type}", e)
            false
        }
    }

    /**
     * ✅ Execute multiple actions from a workflow
     */
    fun executeWorkflow(context: Context, workflow: WorkflowEntity): Boolean {
        Log.d(TAG, "Executing workflow: ${workflow.workflowName}")

        // Get notification manager for tracking
        val notificationManager = InAppNotificationManager.getInstance(context)

        val actions = workflow.toActions()
        if (actions.isEmpty()) {
            Log.w(TAG, "No actions to execute")
            notificationManager.addTaskExecution(workflow.workflowName, 0, false)
            return false
        }

        var successCount = 0
        var totalActions = actions.size

        // Execute each action
        actions.forEach { action ->
            if (executeAction(context, action)) {
                successCount++
            }
        }

        Log.d(TAG, "Executed $successCount/$totalActions actions successfully")

        // Add task execution notification to bell icon
        notificationManager.addTaskExecution(
            workflowName = workflow.workflowName,
            actionsCount = successCount,
            success = successCount == totalActions
        )

        // Check if this is a meeting mode workflow and activate meeting mode in notifications
        val isMeetingModeWorkflow = workflow.actionDetails.contains("DND") ||
                workflow.actionDetails.contains("SET_SOUND_MODE") ||
                workflow.actionDetails.contains("Silent") ||
                workflow.workflowName.contains("meeting", ignoreCase = true) ||
                workflow.workflowName.contains("mode", ignoreCase = true)

        if (isMeetingModeWorkflow && successCount > 0) {
            notificationManager.setMeetingMode(true, workflow.workflowName)
            Log.d(TAG, "🔇 Meeting mode activated in notifications")
        }

        return successCount > 0
    }

    // ==================== ACTION IMPLEMENTATIONS ====================

    /**
     * ✅ Send notification with priority levels
     */
    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        priority: String = "Normal"
    ): Boolean {
        return try {
            createNotificationChannels(context)

            val channelId = when (priority.lowercase()) {
                "high" -> CHANNEL_ID_HIGH
                "low" -> CHANNEL_ID_LOW
                else -> CHANNEL_ID_NORMAL
            }

            val notificationPriority = when (priority.lowercase()) {
                "high" -> NotificationCompat.PRIORITY_HIGH
                "low" -> NotificationCompat.PRIORITY_LOW
                else -> NotificationCompat.PRIORITY_DEFAULT
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(notificationPriority)
                .setAutoCancel(true)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)

            Log.d(TAG, "Notification sent: $title")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            false
        }
    }

    /**
     * ✅ FIXED: Block apps with optional notification
     */
    private fun blockApps(
        context: Context,
        packageNames: String,
        durationMinutes: Int = 0,
        sendNotification: Boolean = false  // ✅ Optional notification
    ): Boolean {
        return try {
            if (packageNames.isBlank()) {
                Log.w(TAG, "No apps specified for blocking")
                return false
            }

            val appsToBlock = packageNames.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (appsToBlock.isEmpty()) {
                Log.w(TAG, "No valid apps to block")
                return false
            }

            Log.d(TAG, "🚫 Blocking ${appsToBlock.size} apps: $appsToBlock")

            // Update BlockPolicy
            BlockPolicy.setBlockedPackages(context, appsToBlock.toSet())
            BlockPolicy.setBlockingEnabled(context, true)

            // ✅ Only send notification if explicitly requested
            if (sendNotification) {
                if (durationMinutes > 0) {
                    scheduleAutoUnblock(context, durationMinutes)
                    sendNotification(
                        context,
                        "🚫 App Blocking Active",
                        "Blocking for $durationMinutes minutes",
                        "High"
                    )
                } else {
                    sendNotification(
                        context,
                        "🚫 App Blocking Active",
                        "Now blocking ${appsToBlock.size} app(s)",
                        "High"
                    )
                }
            }

            Log.d(TAG, "✅ App blocking enabled for: ${appsToBlock.joinToString()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error blocking apps", e)
            false
        }
    }

    /**
     * ✅ Unblock all apps
     */
    private fun unblockApps(context: Context): Boolean {
        return try {
            BlockPolicy.setBlockingEnabled(context, false)
            BlockPolicy.clearBlockedPackages(context)

            Log.d(TAG, "✅ All apps unblocked")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unblocking apps", e)
            false
        }
    }

    /**
     * ✅ Schedule auto-unblock after duration
     */
    private fun scheduleAutoUnblock(context: Context, durationMinutes: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.autoflow.UNBLOCK_APPS"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "⏰ Auto-unblock scheduled for $durationMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling auto-unblock", e)
        }
    }

    /**
     * ✅ Set sound mode (Silent/Vibrate/Normal)
     */
    private fun setSoundMode(context: Context, mode: String): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Check Do Not Disturb permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!notificationManager.isNotificationPolicyAccessGranted) {
                    Log.w(TAG, "⚠️ Do Not Disturb permission not granted")
                    return false
                }
            }

            val ringerMode = when (mode.lowercase()) {
                "silent" -> AudioManager.RINGER_MODE_SILENT
                "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                "normal" -> AudioManager.RINGER_MODE_NORMAL
                else -> AudioManager.RINGER_MODE_NORMAL
            }

            audioManager.ringerMode = ringerMode
            Log.d(TAG, "🔊 Sound mode set to: $mode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sound mode", e)
            false
        }
    }

    /**
     * ✅ Toggle WiFi (requires CHANGE_WIFI_STATE permission)
     */
    private fun toggleWiFi(context: Context, state: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ doesn't allow programmatic WiFi control
                Log.w(TAG, "⚠️ WiFi control not available on Android 10+")

                // Open WiFi settings instead
                val intent = Intent(Settings.Panel.ACTION_WIFI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            when (state.lowercase()) {
                "on" -> {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = true
                    Log.d(TAG, "📶 WiFi enabled")
                }
                "off" -> {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = false
                    Log.d(TAG, "📶 WiFi disabled")
                }
                "toggle" -> {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                    Log.d(TAG, "📶 WiFi toggled")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling WiFi", e)
            false
        }
    }

    /**
     * ✅ Toggle Bluetooth
     */
    private fun toggleBluetooth(context: Context, state: String): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            if (bluetoothAdapter == null) {
                Log.w(TAG, "Bluetooth not supported")
                return false
            }

            // Check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "⚠️ BLUETOOTH_CONNECT permission not granted")
                    return false
                }
            }

            when (state.lowercase()) {
                "on" -> {
                    if (!bluetoothAdapter.isEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Open Bluetooth settings
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } else {
                            @Suppress("DEPRECATION")
                            bluetoothAdapter.enable()
                        }
                    }
                    Log.d(TAG, "📡 Bluetooth enabled")
                }
                "off" -> {
                    if (bluetoothAdapter.isEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Open Bluetooth settings
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } else {
                            @Suppress("DEPRECATION")
                            bluetoothAdapter.disable()
                        }
                    }
                    Log.d(TAG, "📡 Bluetooth disabled")
                }
                "toggle" -> {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.d(TAG, "📡 Bluetooth settings opened")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling Bluetooth", e)
            false
        }
    }

    /**
     * ✅ Create notification channels
     */
    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // High priority channel
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "High Priority",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications"
            }

            // Normal priority channel
            val normalChannel = NotificationChannel(
                CHANNEL_ID_NORMAL,
                "Normal Priority",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Normal priority notifications"
            }

            // Low priority channel
            val lowChannel = NotificationChannel(
                CHANNEL_ID_LOW,
                "Low Priority",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Low priority notifications"
            }

            notificationManager.createNotificationChannels(listOf(highChannel, normalChannel, lowChannel))
        }
    }


    // Execute auto-reply SMS action
    private fun executeAutoReplySms(context: Context, action: Action): Boolean {
        Log.d(TAG, "📱 Executing AUTO_REPLY_SMS action")

        val enabled = action.value?.toBoolean() ?: true
        val message = action.message ?: Constants.DEFAULT_AUTO_REPLY_MESSAGE

        // Save auto-reply settings to SharedPreferences
        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(Constants.PREF_AUTO_REPLY_ENABLED, enabled)
            .putString(Constants.PREF_AUTO_REPLY_MESSAGE, message)
            .putBoolean(Constants.PREF_AUTO_REPLY_ONLY_IN_DND, true)
            .apply()

        Log.i(TAG, "✅ Auto-reply SMS ${if (enabled) "enabled" else "disabled"}")
        Log.i(TAG, "   Message: \"$message\"")
        Log.i(TAG, "   Only in DND: true")

        return true
    }
}
