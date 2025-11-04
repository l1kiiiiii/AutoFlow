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
import com.example.autoflow.receiver.AutoUnblockReceiver
import com.example.autoflow.service.AppBlockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * ✅ COMPLETELY FIXED ActionExecutor - Proper coroutine usage and modern Android compatibility
 */
class ActionExecutor private constructor() {

    companion object {
        private const val TAG = "ActionExecutor"
        private const val CHANNEL_ID_HIGH = "autoflow_high"
        private const val CHANNEL_ID_NORMAL = "autoflow_normal"
        private const val CHANNEL_ID_LOW = "autoflow_low"

        @Volatile
        private var INSTANCE: ActionExecutor? = null

        fun getInstance(): ActionExecutor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActionExecutor().also { INSTANCE = it }
            }
        }
    }

    /**
     * ✅ Execute a single action with proper coroutine scope
     */
    suspend fun executeAction(
        context: Context,
        action: Action,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        Log.d(TAG, "🎯 Executing action: ${action.type}")

        try {
            // ✅ FIXED: Handle extended actions directly here instead of calling non-existent class
            when (action.type) {
                // ✅ BRIGHTNESS ACTIONS
                Constants.ACTION_SET_BRIGHTNESS -> {
                    val percentage = action.value?.toIntOrNull() ?: 50
                    val brightnessManager = BrightnessManager.getInstance(context)
                    brightnessManager.setBrightnessByPercentage(percentage, scope)
                }

                Constants.ACTION_INCREASE_BRIGHTNESS -> {
                    val amount = action.value?.toIntOrNull() ?: 20
                    val brightnessManager = BrightnessManager.getInstance(context)
                    brightnessManager.increaseBrightness(amount, scope)
                }

                Constants.ACTION_DECREASE_BRIGHTNESS -> {
                    val amount = action.value?.toIntOrNull() ?: 20
                    val brightnessManager = BrightnessManager.getInstance(context)
                    brightnessManager.decreaseBrightness(amount, scope)
                }

                // ✅ VOLUME ACTIONS
                Constants.ACTION_SET_MEDIA_VOLUME -> {
                    val percentage = action.value?.toIntOrNull() ?: 50
                    val volumeManager = VolumeManager.getInstance(context)
                    volumeManager.setMediaVolume(percentage, false, scope)
                }

                Constants.ACTION_SET_RING_VOLUME -> {
                    val percentage = action.value?.toIntOrNull() ?: 50
                    val volumeManager = VolumeManager.getInstance(context)
                    volumeManager.setVolumeByPercentage("ring", percentage, false, scope)
                }

                Constants.ACTION_SET_NOTIFICATION_VOLUME -> {
                    val percentage = action.value?.toIntOrNull() ?: 50
                    val volumeManager = VolumeManager.getInstance(context)
                    volumeManager.setNotificationSoundVolume(percentage, false, scope)
                }

                Constants.ACTION_SET_ALARM_VOLUME -> {
                    val percentage = action.value?.toIntOrNull() ?: 80
                    val volumeManager = VolumeManager.getInstance(context)
                    volumeManager.setVolumeByPercentage("alarm", percentage, false, scope)
                }

                Constants.ACTION_SET_CALL_VOLUME -> {
                    val percentage = action.value?.toIntOrNull() ?: 70
                    val volumeManager = VolumeManager.getInstance(context)
                    volumeManager.setPhoneVolume(percentage, false, scope)
                }

                // ✅ EXISTING ACTIONS
                Constants.ACTION_SEND_NOTIFICATION -> {
                    val title = action.title ?: "AutoFlow"
                    val message = action.message ?: "Automation triggered"
                    val priority = action.priority ?: "Normal"
                    sendNotification(context, title, message, priority)
                }

                Constants.ACTION_BLOCK_APPS -> {
                    blockApps(
                        context = context,
                        packageNames = action.value ?: "",
                        durationMinutes = 0,
                        sendNotification = false,
                        scope = scope
                    )
                }

                Constants.ACTION_UNBLOCK_APPS -> {
                    unblockApps(context, scope = scope)
                }

                Constants.ACTION_SET_SOUND_MODE, "SET_SOUND_MODE" -> {
                    val mode = action.value ?: "Normal"
                    setSoundModeImproved(context, mode, scope)
                }

                Constants.ACTION_TOGGLE_WIFI -> {
                    val state = action.value ?: "Toggle"
                    toggleWiFiImproved(context, state, scope)
                }

                Constants.ACTION_TOGGLE_BLUETOOTH -> {
                    val state = action.value ?: "Toggle"
                    toggleBluetoothImproved(context, state, scope)
                }

                Constants.ACTION_AUTO_REPLY_SMS -> {
                    executeAutoReplySms(context, action, scope)
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
     * ✅ FIXED: Improved DND mode implementation with better UX
     */
    private suspend fun setSoundModeImproved(
        context: Context,
        mode: String,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            when (mode.lowercase()) {
                "dnd" -> {
                    // ✅ FIXED: Better DND implementation
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (notificationManager.isNotificationPolicyAccessGranted) {
                            // Set DND mode
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                        } else {
                            // Fallback to silent mode and request permission
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

                            sendNotification(
                                context,
                                "🔇 Silent Mode Active",
                                "DND permission required for full Do Not Disturb mode.",
                                "Normal"
                            )

                            // Open DND settings
                            scope.launch(Dispatchers.Main) {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to open DND settings", e)
                                }
                            }
                        }
                    } else {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    }
                    true
                }
                "silent" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    true
                }
                "vibrate" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    true
                }
                "normal" -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    true
                }
                else -> {
                    Log.w(TAG, "⚠️ Unknown sound mode: $mode, defaulting to normal")
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting sound mode", e)
            false
        }
    }

    /**
     * ✅ FIXED: Improved WiFi toggle with proper result tracking
     */
    private suspend fun toggleWiFiImproved(
        context: Context,
        state: String,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // ✅ FIXED: Modern Android handling with user feedback
                Log.i(TAG, "📶 Opening WiFi settings (Android 10+ restriction)")

                val intent = Intent(Settings.Panel.ACTION_WIFI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    context.startActivity(intent)

                    sendNotification(
                        context,
                        "📶 WiFi Settings Opened",
                        "Please manually ${state.lowercase()} WiFi in the settings panel.",
                        "Normal"
                    )
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to open WiFi settings", e)

                    // Fallback to general settings
                    val fallbackIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallbackIntent)

                    sendNotification(
                        context,
                        "📶 WiFi Settings",
                        "Opened general WiFi settings. Please ${state.lowercase()} WiFi manually.",
                        "Normal"
                    )
                    true
                }
            } else {
                // Legacy Android handling
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

                val success = when (state.lowercase()) {
                    "on" -> {
                        @Suppress("DEPRECATION")
                        wifiManager.isWifiEnabled = true
                        true
                    }
                    "off" -> {
                        @Suppress("DEPRECATION")
                        wifiManager.isWifiEnabled = false
                        true
                    }
                    "toggle" -> {
                        @Suppress("DEPRECATION")
                        wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                        true
                    }
                    else -> false
                }

                if (success) {
                    Log.d(TAG, "📶 WiFi ${state.lowercase()} completed (legacy)")
                    sendNotification(
                        context,
                        "📶 WiFi ${state.capitalize()}",
                        "WiFi has been ${state.lowercase()}.",
                        "Low"
                    )
                }

                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error toggling WiFi", e)
            false
        }
    }

    /**
     * ✅ FIXED: Improved Bluetooth toggle with proper result tracking
     */
    private suspend fun toggleBluetoothImproved(
        context: Context,
        state: String,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter == null) {
                Log.w(TAG, "📡 Bluetooth not supported on this device")
                sendNotification(
                    context,
                    "📡 Bluetooth Not Supported",
                    "This device doesn't support Bluetooth.",
                    "Normal"
                )
                return@withContext false
            }

            // Check permissions for modern Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "⚠️ BLUETOOTH_CONNECT permission not granted")

                    sendNotification(
                        context,
                        "📡 Bluetooth Permission Required",
                        "Bluetooth permission needed. Opening settings...",
                        "Normal"
                    )

                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)

                    return@withContext true
                }
            }

            // Handle different states
            when (state.lowercase()) {
                "on" -> {
                    if (!bluetoothAdapter.isEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)

                            sendNotification(
                                context,
                                "📡 Bluetooth Settings",
                                "Please enable Bluetooth in the settings.",
                                "Normal"
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            bluetoothAdapter.enable()

                            sendNotification(
                                context,
                                "📡 Bluetooth Enabling",
                                "Bluetooth is being enabled...",
                                "Low"
                            )
                        }
                    } else {
                        sendNotification(
                            context,
                            "📡 Bluetooth Already On",
                            "Bluetooth is already enabled.",
                            "Low"
                        )
                    }
                    true
                }

                "off" -> {
                    if (bluetoothAdapter.isEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)

                            sendNotification(
                                context,
                                "📡 Bluetooth Settings",
                                "Please disable Bluetooth in the settings.",
                                "Normal"
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            bluetoothAdapter.disable()

                            sendNotification(
                                context,
                                "📡 Bluetooth Disabling",
                                "Bluetooth is being disabled...",
                                "Low"
                            )
                        }
                    } else {
                        sendNotification(
                            context,
                            "📡 Bluetooth Already Off",
                            "Bluetooth is already disabled.",
                            "Low"
                        )
                    }
                    true
                }

                "toggle" -> {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)

                    val currentState = if (bluetoothAdapter.isEnabled) "enabled" else "disabled"
                    sendNotification(
                        context,
                        "📡 Bluetooth Settings",
                        "Bluetooth is currently $currentState. Please toggle it in the settings.",
                        "Normal"
                    )
                    true
                }

                else -> {
                    Log.w(TAG, "Unknown Bluetooth state: $state")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error toggling Bluetooth", e)
            false
        }
    }

    /**
     * ✅ Enhanced app blocking with proper coroutine usage
     */
    private suspend fun blockApps(
        context: Context,
        packageNames: String,
        durationMinutes: Int = 0,
        sendNotification: Boolean = true,
        scope: CoroutineScope,
        workflowId: Long = -1L // NEW: Track which workflow is blocking
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (packageNames.isBlank()) {
                Log.w(TAG, "No apps specified for blocking")
                return@withContext false
            }

            val appsToBlock = packageNames.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (appsToBlock.isEmpty()) {
                Log.w(TAG, "No valid apps to block")
                return@withContext false
            }

            Log.d(TAG, "🚫 Blocking ${appsToBlock.size} apps for workflow $workflowId")

            // ✅ FIXED: Use enhanced BlockPolicy with workflow tracking
            BlockPolicy.setBlockingEnabled(context, true)

            if (workflowId > 0) {
                BlockPolicy.blockAppsForWorkflow(context, workflowId, appsToBlock, "manual_trigger")
            } else {
                BlockPolicy.setBlockedPackages(context, appsToBlock.toSet())
            }

            scope.launch(Dispatchers.Main) {
                try {
                    val serviceIntent = Intent(context, AppBlockService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d(TAG, "✅ AppBlockService started")
                } catch (e: SecurityException) {
                    Log.w(TAG, "⚠️ Service failed (accessibility fallback): ${e.message}")
                }
            }

            if (sendNotification) {
                withContext(Dispatchers.Main) {
                    val durationText = if (durationMinutes > 0)
                        "for $durationMinutes minutes" else "until conditions change"

                    sendNotification(
                        context,
                        "🚫 ${appsToBlock.size} Apps Blocked",
                        "Blocked apps $durationText",
                        "High"
                    )
                }
            }

            if (durationMinutes > 0) {
                scheduleAutoUnblock(context, durationMinutes, appsToBlock, scope)
            }

            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error blocking apps", e)
            false
        }
    }

    /**
     * ✅ Enhanced app unblocking with proper coroutine usage
     */
    private suspend fun unblockApps(
        context: Context,
        packageNames: String? = null,
        scope: CoroutineScope,
        workflowId: Long = -1L, // NEW: Unblock specific workflow
        reason: String = "manual" // NEW: Reason for unblocking
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val appsToUnblock = when {
                workflowId > 0 -> {
                    // Unblock apps for specific workflow
                    BlockPolicy.unblockAppsForWorkflow(context, workflowId)
                }
                !packageNames.isNullOrBlank() -> {
                    // Unblock specific packages
                    packageNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
                else -> {
                    // Unblock all packages
                    BlockPolicy.getBlockedPackages(context).toList()
                }
            }

            if (appsToUnblock.isEmpty()) {
                Log.w(TAG, "No apps to unblock")
                return@withContext false
            }

            Log.d(TAG, "✅ Unblocking ${appsToUnblock.size} apps (reason: $reason)")

            if (packageNames.isNullOrBlank() && workflowId <= 0) {
                // Complete unblock
                BlockPolicy.clearAllBlocks(context)

                scope.launch(Dispatchers.Main) {
                    try {
                        val serviceIntent = Intent(context, AppBlockService::class.java)
                        context.stopService(serviceIntent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Service stop failed: ${e.message}")
                    }
                }
            } else if (workflowId > 0) {
                // Workflow-specific unblock (already handled above)
                val remainingBlocks = BlockPolicy.getBlockedPackages(context)
                if (remainingBlocks.isEmpty()) {
                    BlockPolicy.setBlockingEnabled(context, false)
                    scope.launch(Dispatchers.Main) {
                        try {
                            val serviceIntent = Intent(context, AppBlockService::class.java)
                            context.stopService(serviceIntent)
                        } catch (e: Exception) {
                            Log.w(TAG, "Service stop failed: ${e.message}")
                        }
                    }
                }
            } else {
                // Package-specific unblock
                BlockPolicy.removeBlockedPackages(context, appsToUnblock)

                if (BlockPolicy.getBlockedPackages(context).isEmpty()) {
                    BlockPolicy.setBlockingEnabled(context, false)
                    scope.launch(Dispatchers.Main) {
                        try {
                            val serviceIntent = Intent(context, AppBlockService::class.java)
                            context.stopService(serviceIntent)
                        } catch (e: Exception) {
                            Log.w(TAG, "Service stop failed: ${e.message}")
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                sendNotification(
                    context,
                    "✅ Apps Unblocked",
                    "Successfully unblocked ${appsToUnblock.size} apps ($reason)",
                    "Normal"
                )
            }

            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unblocking apps", e)
            false
        }
    }

    /**
     * ✅ Schedule auto-unblock with proper coroutine usage
     */
    private fun scheduleAutoUnblock(
        context: Context,
        durationMinutes: Int,
        apps: List<String>,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AutoUnblockReceiver::class.java).apply {
                    putExtra("apps_to_unblock", apps.joinToString(","))
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }

                Log.d(TAG, "⏰ Auto-unblock scheduled in $durationMinutes minutes")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to schedule auto-unblock", e)
            }
        }
    }

    private suspend fun sendNotification(
        context: Context,
        title: String,
        message: String,
        priority: String = "Normal"
    ): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
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

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(CHANNEL_ID_HIGH, "High Priority", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_ID_NORMAL, "Normal Priority", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_ID_LOW, "Low Priority", NotificationManager.IMPORTANCE_LOW)
            )

            notificationManager.createNotificationChannels(channels)
        }
    }

    private suspend fun executeAutoReplySms(
        context: Context,
        action: Action,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val isEnabled = action.value == "true"
            val message = action.message?.takeIf { it.isNotEmpty() }
                ?: "I'm currently in a meeting and will get back to you soon."

            val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(Constants.PREF_AUTO_REPLY_ENABLED, isEnabled)
                .putString(Constants.PREF_AUTO_REPLY_MESSAGE, message)
                .putBoolean(Constants.PREF_AUTO_REPLY_ONLY_IN_DND, true)
                .apply()

            Log.i(TAG, "✅ Auto-reply SMS ${if (isEnabled) "enabled" else "disabled"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing auto-reply SMS", e)
            false
        }
    }

    /**
     * ✅ Execute workflow actions
     */
    fun executeWorkflow(context: Context, workflow: WorkflowEntity): Boolean {
        return try {
            val actions = workflow.toActions()
            var successCount = 0

            actions.forEach { action ->
                Log.d(TAG, "🔧 Executing action: ${action.type} = ${action.value}")
                // For workflow execution, we don't need suspend context
                val success = when (action.type) {
                    Constants.ACTION_SEND_NOTIFICATION -> {
                        val title = action.title ?: "AutoFlow"
                        val message = action.message ?: "Automation triggered"
                        val priority = action.priority ?: "Normal"

                        try {
                            createNotificationChannels(context)
                            val channelId = when (priority.lowercase()) {
                                "high" -> CHANNEL_ID_HIGH
                                "low" -> CHANNEL_ID_LOW
                                else -> CHANNEL_ID_NORMAL
                            }

                            val notification = NotificationCompat.Builder(context, channelId)
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle(title)
                                .setContentText(message)
                                .setAutoCancel(true)
                                .build()

                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
                            true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending notification", e)
                            false
                        }
                    }

                    Constants.ACTION_SET_SOUND_MODE -> {
                        try {
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            val mode = action.value ?: "Normal"

                            when (mode.lowercase()) {
                                "silent" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                                "vibrate" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                                "normal" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                                "dnd" -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                        if (notificationManager.isNotificationPolicyAccessGranted) {
                                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                                        } else {
                                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                                        }
                                    } else {
                                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                                    }
                                }
                            }
                            true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting sound mode", e)
                            false
                        }
                    }

                    else -> {
                        Log.w(TAG, "Unsupported action type for sync execution: ${action.type}")
                        false
                    }
                }

                if (success) {
                    successCount++
                    Log.d(TAG, "✅ Action succeeded: ${action.type}")
                } else {
                    Log.e(TAG, "❌ Action failed: ${action.type}")
                }
            }

            Log.d(TAG, "🎉 Workflow execution complete: $successCount/${actions.size} actions succeeded")
            successCount > 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing workflow", e)
            false
        }
    }
}
