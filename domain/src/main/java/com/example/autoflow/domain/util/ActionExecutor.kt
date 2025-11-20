package com.example.autoflow.domain.util

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
import com.example.autoflow.domain.model.Action
import com.example.autoflow.model.NotificationType
import com.example.autoflow.policy.BlockPolicy
import com.example.autoflow.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

//  meeting mode state tracking
object MeetingModeTracker {
    private const val PREF_NAME = "meeting_mode_state"
    private const val KEY_IS_MEETING_ACTIVE = "is_meeting_active"
    private const val KEY_MEETING_START_TIME = "meeting_start_time"

    fun setMeetingModeActive(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_MEETING_ACTIVE, active)
            .putLong(KEY_MEETING_START_TIME, if (active) System.currentTimeMillis() else 0)
            .apply()

        Log.d("MeetingModeTracker", "🏢 Meeting mode: $active")
    }

    fun isMeetingModeActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_MEETING_ACTIVE, false)
    }
}

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
    /**
     * ✅ Enhanced sleep mode with time constraints and auto-deactivation
     */
    fun executeWorkflow(context: Context, workflow: WorkflowEntity): Boolean {
        Log.d(TAG, "Executing workflow: ${workflow.workflowName}")

        val notificationManager = InAppNotificationManager.getInstance(context)
        val actions = workflow.toActions()

        if (actions.isEmpty()) {
            Log.w(TAG, "No actions to execute")
            notificationManager.addTaskExecution(workflow.workflowName, 0, false)
            return false
        }

        var successCount = 0
        var totalActions = actions.size

        // ✅ ENHANCED: Handle sleep mode workflows with time constraints
        if (isSleepModeWorkflow(workflow)) {
            return handleSleepModeWorkflow(context, workflow, notificationManager)
        }

        // Execute normal workflow
        actions.forEach { action ->
            if (executeAction(context, action)) {
                successCount++
            }
        }

        Log.d(TAG, "Executed $successCount/$totalActions actions successfully")

        notificationManager.addTaskExecution(
            workflowName = workflow.workflowName,
            actionsCount = successCount,
            success = successCount == totalActions
        )

        // Smart meeting mode detection
        val isMeetingWorkflow = isActualMeetingWorkflow(workflow)
        if (isMeetingWorkflow && successCount > 0) {
            notificationManager.setMeetingMode(true, workflow.workflowName)
            Log.d(TAG, "🔇 Meeting mode activated for: ${workflow.workflowName}")
        } else {
            Log.d(TAG, "📋 Task executed without meeting mode: ${workflow.workflowName}")
        }

        return successCount > 0
    }

    /**
     * ✅ Handle sleep mode workflows with time-based activation/deactivation
     */
    private fun handleSleepModeWorkflow(
        context: Context,
        workflow: WorkflowEntity,
        notificationManager: InAppNotificationManager
    ): Boolean {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val isSleepTime = isCurrentlySleepTime(currentTime)

        return if (workflow.workflowName.contains("Start") || isSleepTime) {
            // Activate sleep mode
            executeSleepModeStart(context, workflow, notificationManager)
        } else {
            // Deactivate sleep mode
            executeSleepModeEnd(context, workflow, notificationManager)
        }
    }

    /**
     * ✅ Execute sleep mode start actions
     */
    private fun executeSleepModeStart(
        context: Context,
        workflow: WorkflowEntity,
        notificationManager: InAppNotificationManager
    ): Boolean {
        Log.d(TAG, "🌙 Starting sleep mode")

        var successCount = 0

        // 1. Set DND mode
        if (executeAction(context, Action.createSoundModeAction("DND"))) {
            successCount++
        }

        // 2. Lower brightness
        if (executeAction(context, Action("SET_BRIGHTNESS", "10"))) {
            successCount++
        }

        // 3. Block social apps
        val socialApps = "com.instagram.android,com.tiktok,com.facebook.katana,com.twitter.android"
        if (executeAction(context, Action("BLOCK_APPS", socialApps, 32400000L))) { // 9 hours
            successCount++
        }

        // 4. Schedule automatic wake up
        scheduleWakeUpAlarm(context, "07:00")
        successCount++

        // 5. Add sleep mode notification
        notificationManager.addNotification(
            type = NotificationType.INFO,
            title = "🌙 Sleep Mode Active",
            message = "Good night! Sleep mode active until 7:00 AM. Sweet dreams! 😴",
            isClearable = false
        )

        Log.d(TAG, "🌙 Sleep mode started with $successCount actions")
        return successCount > 0
    }

    /**
     * ✅ Execute sleep mode end actions
     */
    private fun executeSleepModeEnd(
        context: Context,
        workflow: WorkflowEntity,
        notificationManager: InAppNotificationManager
    ): Boolean {
        Log.d(TAG, "☀️ Ending sleep mode")

        var successCount = 0

        // 1. Restore normal sound mode
        if (executeAction(context, Action.createSoundModeAction("Normal"))) {
            successCount++
        }

        // 2. Restore brightness
        if (executeAction(context, Action("SET_BRIGHTNESS", "80"))) {
            successCount++
        }

        // 3. Unblock apps
        BlockPolicy.clearBlockedPackages(context)
        successCount++

        // 4. Add wake up notification
        notificationManager.addNotification(
            type = NotificationType.SUCCESS,
            title = "☀️ Good Morning!",
            message = "Sleep mode ended. Ready to start your day! 🌅",
            isClearable = true
        )

        Log.d(TAG, "☀️ Sleep mode ended with $successCount actions")
        return successCount > 0
    }

    /**
     * ✅ Check if workflow is sleep mode related
     */
    private fun isSleepModeWorkflow(workflow: WorkflowEntity): Boolean {
        val name = workflow.workflowName.lowercase()
        return name.contains("sleep") && (name.contains("start") || name.contains("end"))
    }

    /**
     * ✅ Check if current time is within sleep hours
     */
    private fun isCurrentlySleepTime(currentTime: String): Boolean {
        val sleepStart = "22:00"
        val sleepEnd = "07:00"

        return if (sleepStart > sleepEnd) {
            // Overnight period (22:00 to 07:00 next day)
            currentTime >= sleepStart || currentTime < sleepEnd
        } else {
            // Same day period
            currentTime >= sleepStart && currentTime < sleepEnd
        }
    }

    /**
     * ✅ Schedule automatic wake up alarm
     */
    private fun scheduleWakeUpAlarm(context: Context, wakeTime: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            val timeParts = wakeTime.split(":")
            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            set(Calendar.MINUTE, timeParts[1].toInt())
            set(Calendar.SECOND, 0)

            // If time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("workflow_name", "Sleep Mode End")
            putExtra("workflow_id", -999) // Special wake up alarm ID
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, -999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d(TAG, "⏰ Wake up alarm scheduled for $wakeTime")
    }

    /**
     * ✅ SMART: Determine if this is an ACTUAL meeting workflow
     */
    private fun isActualMeetingWorkflow(workflow: WorkflowEntity): Boolean {
        val workflowName = workflow.workflowName.lowercase()

        // ✅ ONLY activate meeting mode for workflows with these keywords
        val meetingKeywords = listOf(
            "meeting",
            "conference",
            "call",
            "presentation",
            "interview",
            "work meeting",
            "business"
        )

        // ✅ EXCLUDE common non-meeting workflows
        val excludeKeywords = listOf(
            "sleep",
            "class",
            "home",
            "study",
            "night",
            "bedtime",
            "morning",
            "work mode",  // General work, not meeting
            "focus"       // Focus time, not meeting
        )

        // Check if workflow should be excluded
        val shouldExclude = excludeKeywords.any { keyword ->
            workflowName.contains(keyword)
        }

        if (shouldExclude) {
            Log.d(TAG, "🚫 Excluding '${workflow.workflowName}' from meeting mode (excluded keyword)")
            return false
        }

        // Check if workflow contains meeting-related keywords
        val isMeeting = meetingKeywords.any { keyword ->
            workflowName.contains(keyword)
        }

        if (isMeeting) {
            Log.d(TAG, "✅ '${workflow.workflowName}' identified as meeting workflow")
            return true
        }

        Log.d(TAG, "📋 '${workflow.workflowName}' is regular task (not meeting)")
        return false
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
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            when (mode.lowercase()) {
                "silent" -> {
                    // ✅ SILENT MODE: Only mute ringer, keep notifications visible
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    Log.d(TAG, "📴 Silent mode activated - Sound muted, notifications still visible")
                }

                "dnd" -> {
                    //  DND MODE: Complete Do Not Disturb with notification blocking
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!notificationManager.isNotificationPolicyAccessGranted) {
                            Log.w(TAG, "🔕 DND permission not granted, opening settings")
                            // Open DND permission settings
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            return false
                        }

                        // Set complete DND mode
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT

                        // Block all notifications
                        val policy = NotificationManager.Policy(
                            0, // No calls allowed
                            0, // No messages allowed
                            NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA // Allow media only
                        )
                        notificationManager.setNotificationPolicy(policy)

                        // Enable DND mode
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                        }

                        Log.d(TAG, "🔕 Complete DND mode activated - Sound muted + notifications blocked")
                    } else {
                        // Fallback for older Android versions
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        Log.d(TAG, "🔕 DND mode activated (legacy)")
                    }
                }

                "vibrate" -> {
                    // ✅ VIBRATE MODE: Vibration only
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    Log.d(TAG, "📳 Vibrate mode activated")
                }

                "normal" -> {
                    // ✅ NORMAL MODE: Restore everything
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL

                    // Restore notifications if coming from DND
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (notificationManager.isNotificationPolicyAccessGranted) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                            }
                            Log.d(TAG, "🔊 Normal mode restored - Sound + notifications enabled")
                        }
                    } else {
                        Log.d(TAG, "🔊 Normal mode restored")
                    }
                }

                else -> {
                    Log.w(TAG, "⚠️ Unknown sound mode: $mode, defaulting to normal")
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting sound mode", e)
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
