package com.example.autoflow.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.autoflow.data.AppDatabase
import kotlinx.coroutines.*
import android.provider.CallLog
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toActions
import com.example.autoflow.model.Action
import java.util.Calendar


class AutoReplyManager private constructor(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sentNumbers = mutableMapOf<String, Long>()

    companion object {
        private const val TAG = "AutoReplyManager"
        private const val PREF_NAME = "autoflow_autoreply"
        private const val KEY_AUTO_REPLY_ENABLED = "auto_reply_enabled"
        private const val KEY_AUTO_REPLY_MESSAGE = "auto_reply_message"
        private const val KEY_MEETING_MODE_ONLY = "meeting_mode_only"
        private const val KEY_LAST_REPLY_TIME = "last_reply_time"
        private const val REPLY_COOLDOWN_MS = 300000 // 5 minutes

        @Volatile
        private var INSTANCE: AutoReplyManager? = null

        fun getInstance(context: Context): AutoReplyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutoReplyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    suspend fun shouldAutoReply(): Boolean {
        try {
            // 1. Check if auto-reply is enabled
            if (!isAutoReplyEnabled()) {
                Log.d(TAG, "❌ Auto-reply is disabled")
                return false
            }

            // 2. Check if we're in meeting mode only setting
            if (isMeetingModeOnly()) {
                // 3. Check if we're currently in an active meeting mode workflow
                if (!isCurrentlyInMeetingMode()) {
                    Log.d(TAG, "❌ Meeting mode only enabled, but not currently in meeting mode")
                    return false
                }
            }

            // 4. Check DND status (additional condition)
            if (!isDndModeActive() && isMeetingModeOnly()) {
                Log.d(TAG, "❌ Meeting mode active but DND not enabled")
                return false
            }

            Log.d(TAG, "✅ Auto-reply conditions met")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking auto-reply conditions", e)
            return false
        }
    }
    private fun isDndModeActive(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val currentFilter = notificationManager.currentInterruptionFilter
                currentFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                        currentFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                // For older versions, assume DND is active if we're in this function
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking DND status", e)
            false
        }
    }
    private suspend fun isCurrentlyInMeetingMode(): Boolean {
        return try {
            val database = AppDatabase.getDatabase(context)
            val workflows = database.workflowDao().getAllWorkflowsSync()

            // Check if any meeting-related workflow is currently active
            val activeMeetingWorkflows = workflows.filter { workflow ->
                workflow.isEnabled && isMeetingModeWorkflow(workflow.workflowName, workflow.toActions())
            }

            // Additional check: Are we in a location/time that suggests meeting mode?
            val isInMeetingContext = checkMeetingContext(activeMeetingWorkflows)

            Log.d(TAG, "📅 Meeting mode workflows active: ${activeMeetingWorkflows.size}")
            Log.d(TAG, "📍 In meeting context: $isInMeetingContext")

            activeMeetingWorkflows.isNotEmpty() && isInMeetingContext

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking meeting mode status", e)
            false
        }
    }
    private fun isMeetingModeWorkflow(workflowName: String, actions: List<Action>): Boolean {
        val meetingKeywords = listOf("meeting", "conference", "office", "work", "presentation", "call")
        val nameContainsMeetingKeyword = meetingKeywords.any {
            workflowName.contains(it, ignoreCase = true)
        }

        // Check if workflow has DND/Silent actions (common in meeting workflows)
        val hasSoundModeAction = actions.any { action ->
            action.type == "SET_SOUND_MODE" &&
                    (action.value == "DND" || action.value == "Silent")
        }

        return nameContainsMeetingKeyword || hasSoundModeAction
    }
    private fun isInCooldown(phoneNumber: String): Boolean {
        val lastReplyTime = prefs.getLong("${KEY_LAST_REPLY_TIME}_$phoneNumber", 0)
        return System.currentTimeMillis() - lastReplyTime < REPLY_COOLDOWN_MS
    }

    private fun updateLastReplyTime(phoneNumber: String) {
        prefs.edit().putLong("${KEY_LAST_REPLY_TIME}_$phoneNumber", System.currentTimeMillis()).apply()
    }
    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "📤 SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send SMS to $phoneNumber", e)
            throw e
        }
    }
    private suspend fun checkMeetingContext(workflows: List<WorkflowEntity>): Boolean {
        // This would check current location against workflow triggers
        // and current time against time triggers
        // For now, simplified version:

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isBusinessHours = currentHour in 9..17 // 9 AM to 5 PM

        // You can enhance this with actual location checking
        return isBusinessHours
    }
    private fun hasRequiredPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )

        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showPermissionRequiredNotification() {
        try {
            NotificationHelper.sendNotification(
                context = context,
                title = "⚠️ Permissions Required",
                message = "AutoFlow needs SMS and Call Log permissions for auto-reply. Please enable in Settings.",
                priority = NotificationCompat.PRIORITY_HIGH,
                notificationId = 9999
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing permission notification", e)
        }
    }
    suspend fun handleMissedCall(phoneNumber: String) {
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "❌ Missing required permissions for auto-reply")
            showPermissionRequiredNotification()
            return
        }
        if (!shouldAutoReply()) {
            Log.d(TAG, "⏭️ Auto-reply conditions not met, skipping")
            return
        }

        // Cooldown check
        if (isInCooldown(phoneNumber)) {
            Log.d(TAG, "⏱️ Cooldown active for $phoneNumber, skipping")
            return
        }

        try {
            val message = getAutoReplyMessage()
            sendSms(phoneNumber, message)
            updateLastReplyTime(phoneNumber)

            // Log for debugging
            Log.d(TAG, "✅ Auto-reply sent to $phoneNumber: $message")

            // Show notification
            NotificationHelper.sendNotification(
                context = context,
                title = "📱 Auto-Reply Sent",
                message = "Replied to $phoneNumber with meeting message",
                priority = androidx.core.app.NotificationCompat.PRIORITY_LOW
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending auto-reply", e)
        }
    }fun getAutoReplyMessage(): String {
        return prefs.getString(KEY_AUTO_REPLY_MESSAGE,
            "I'm currently in a meeting and cannot take your call. I'll get back to you as soon as possible."
        ) ?: "I'm currently in a meeting. Will call you back soon."
    }

    fun setAutoReplyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REPLY_ENABLED, enabled).apply()
        Log.d(TAG, "🔄 Auto-reply enabled: $enabled")
    }

    fun isAutoReplyEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_REPLY_ENABLED, false)

    fun setMeetingModeOnly(meetingOnly: Boolean) {
        prefs.edit().putBoolean(KEY_MEETING_MODE_ONLY, meetingOnly).apply()
        Log.d(TAG, "🏢 Meeting mode only: $meetingOnly")
    }

    fun isMeetingModeOnly(): Boolean = prefs.getBoolean(KEY_MEETING_MODE_ONLY, true)

    fun setAutoReplyMessage(message: String) {
        prefs.edit().putString(KEY_AUTO_REPLY_MESSAGE, message).apply()
        Log.d(TAG, "💬 Auto-reply message updated: $message")
    }
    fun handleIncomingCall(phoneNumber: String?) {
        if (phoneNumber.isNullOrBlank()) {
            Log.d(TAG, "⚠️ Phone number not available, skipping auto-reply")
            return
        }

        Log.d(TAG, "📞 Processing incoming call from: $phoneNumber")

        coroutineScope.launch {
            try {
                if (shouldSendAutoReply(phoneNumber)) {
                    sendAutoReply(phoneNumber)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing auto-reply", e)
            }
        }
    }

    private suspend fun shouldSendAutoReply(phoneNumber: String): Boolean {
        // Check if we recently sent SMS to this number (cooldown)
        val lastSentTime = sentNumbers[phoneNumber] ?: 0L
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastSentTime < Constants.AUTO_REPLY_COOLDOWN_MS) {
            Log.d(TAG, "⏱️ Cooldown active for $phoneNumber, skipping")
            return false
        }

        // Check if auto-reply is enabled in settings
        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        val autoReplyEnabled = prefs.getBoolean(Constants.PREF_AUTO_REPLY_ENABLED, false)

        if (!autoReplyEnabled) {
            Log.d(TAG, "⚠️ Auto-reply disabled in settings")
            return false
        }

        // Check if we should only reply during DND/meeting mode
        val onlyInDnd = prefs.getBoolean(Constants.PREF_AUTO_REPLY_ONLY_IN_DND, true)

        if (onlyInDnd && !isInDndMode()) {
            Log.d(TAG, "⚠️ Not in DND mode, auto-reply restricted")
            return false
        }

        // Check if any "meeting mode" workflow is currently active
        if (onlyInDnd && !isMeetingModeActive()) {
            Log.d(TAG, "⚠️ No meeting mode workflow active")
            return false
        }

        return true
    }

    private suspend fun sendAutoReply(phoneNumber: String) {
        if (!hasSmsPermission()) {
            Log.e(TAG, "❌ SMS permission not granted")
            showPermissionRequiredNotification()
            return
        }

        // Get notification manager for tracking
        val notificationManager = InAppNotificationManager.getInstance(context)

        try {
            val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
            val message = prefs.getString(
                Constants.PREF_AUTO_REPLY_MESSAGE,
                Constants.DEFAULT_AUTO_REPLY_MESSAGE
            ) ?: Constants.DEFAULT_AUTO_REPLY_MESSAGE

            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            // Record that we sent SMS to this number
            sentNumbers[phoneNumber] = System.currentTimeMillis()

            Log.i(TAG, "✅ Auto-reply SMS sent to: $phoneNumber")

            // ✅ ADD: Track SMS success in bell notifications
            notificationManager.addSmsReply(phoneNumber, message, true)

            // Show standard notification that auto-reply was sent
            showAutoReplySentNotification(phoneNumber, message)

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SMS permission denied", e)
            // ✅ ADD: Track SMS failure in bell notifications
            notificationManager.addSmsReply(phoneNumber, "Permission denied", false)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send auto-reply SMS", e)
            // ✅ ADD: Track SMS failure in bell notifications
            notificationManager.addSmsReply(phoneNumber, "Send failed", false)
        }
    }
    
    private suspend fun isMeetingModeActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Checking if meeting mode is active...")

            // Method 1: Check current ringer mode (most reliable)
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentRingerMode = audioManager.ringerMode

            Log.d(TAG, "🔊 Current ringer mode: $currentRingerMode")
            Log.d(TAG, "   0=Silent, 1=Vibrate, 2=Normal")

            val isDndByRinger = currentRingerMode != AudioManager.RINGER_MODE_NORMAL

            if (isDndByRinger) {
                Log.d(TAG, "✅ Meeting mode ACTIVE (ringer mode indicates DND/Silent)")
                return@withContext true
            }

            // Method 2: Check notification manager DND status
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val dndFilter = notificationManager.currentInterruptionFilter

            Log.d(TAG, "🔕 DND filter: $dndFilter")
            Log.d(TAG, "   1=All, 2=Priority, 3=None, 4=Alarms")

            val isDndByNotification = dndFilter != NotificationManager.INTERRUPTION_FILTER_ALL

            if (isDndByNotification) {
                Log.d(TAG, "✅ Meeting mode ACTIVE (DND enabled)")
                return@withContext true
            }

            // Method 3: Check database for active workflows (as backup)
            try {
                val database = AppDatabase.getDatabase(context)
                val workflows = database.workflowDao().getAllEnabledSync()

                val meetingKeywords = listOf(
                    "meeting",
                    "conference",
                    "call",
                    "presentation",
                    "interview",
                    "work meeting",
                    "business"
                )
                val excludeKeywords = listOf(
                    "sleep",
                    "class",
                    "home",
                    "study",
                    "night",
                    "bedtime",
                    "driving",
                    "gym",
                    "work mode",
                    "focus"
                )

                val meetingWorkflows = workflows.filter { workflow ->
                    val workflowName = workflow.workflowName.lowercase()

                    val isExcluded = excludeKeywords.any { keyword ->
                        workflowName.contains(keyword)
                    }
                    val isMeeting = meetingKeywords.any { keyword ->
                        workflowName.contains(keyword)
                    }

                    // Only count if it's enabled, IS a meeting, and IS NOT excluded
                    workflow.isEnabled && isMeeting && !isExcluded
                }

                Log.d(TAG, "📊 Found ${meetingWorkflows.size} potential meeting workflows")
                meetingWorkflows.forEach { workflow ->
                    Log.d(TAG, "   - ${workflow.workflowName} (ID: ${workflow.id})")
                }

                if (meetingWorkflows.isNotEmpty()) {
                    Log.d(TAG, "✅ Meeting mode ACTIVE (meeting workflow enabled)")
                    return@withContext true
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking workflows", e)
            }

            // Method 4: Check SharedPreferences for manual meeting mode
            val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
            val manualMeetingMode = prefs.getBoolean("manual_meeting_mode", false)

            if (manualMeetingMode) {
                Log.d(TAG, "✅ Meeting mode ACTIVE (manually enabled)")
                return@withContext true
            }

            Log.d(TAG, "❌ Meeting mode NOT ACTIVE")
            return@withContext false

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking meeting mode", e)
            return@withContext false
        }
    }

    // Also update the isInDndMode method to be more reliable
    private fun isInDndMode(): Boolean {
        return try {
            Log.d(TAG, "🔍 Checking DND mode status...")

            // Check ringer mode
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = audioManager.ringerMode

            Log.d(TAG, "🔊 Ringer mode: $ringerMode (0=Silent, 1=Vibrate, 2=Normal)")

            val isDndByRinger = ringerMode != AudioManager.RINGER_MODE_NORMAL

            // Check notification DND
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val interruptionFilter = notificationManager.currentInterruptionFilter

            Log.d(TAG, "🔕 Interruption filter: $interruptionFilter (1=All, 2=Priority, 3=None, 4=Alarms)")

            val isDndByNotification = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

            val result = isDndByRinger || isDndByNotification

            Log.d(TAG, if (result) "✅ DND mode IS active" else "❌ DND mode NOT active")

            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking DND status", e)
            false
        }
    }

    // Add this method to your AutoReplyManager class
    fun handleUnknownIncomingCall() {
        Log.d(TAG, "🔥 handleUnknownIncomingCall called")
        Log.d(TAG, "   📱 Processing call from unknown/private/company number")

        coroutineScope.launch {
            try {
                if (shouldSendUniversalAutoReply()) {
                    sendUniversalAutoReply()
                    Log.d(TAG, "✅ Universal auto-reply processed")
                } else {
                    Log.d(TAG, "⚠️ Universal auto-reply not needed (not in meeting mode)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing universal auto-reply", e)
            }
        }
    }

    private suspend fun shouldSendUniversalAutoReply(): Boolean {
        // Check if auto-reply is enabled in settings
        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        val autoReplyEnabled = prefs.getBoolean("auto_reply_enabled", false)

        if (!autoReplyEnabled) {
            Log.d(TAG, "⚠️ Auto-reply disabled in settings")
            return false
        }

        // Check if we should only reply during DND/meeting mode
        val onlyInDnd = prefs.getBoolean("auto_reply_only_in_dnd", true)

        if (onlyInDnd) {
            // Only check if actual meeting mode is active, not DND status
            if (!isMeetingModeActive()) {
                Log.d(TAG, "⚠️ Meeting mode is not active, skipping auto-reply")
                return false
            }
            // If we are here, meeting mode is active, so proceed.
        }

        Log.d(TAG, "✅ Universal auto-reply conditions met")
        return true
    }

    private suspend fun sendUniversalAutoReply() {
        val notificationManager = InAppNotificationManager.getInstance(context)

        try {
            val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
            val message = prefs.getString(
                "auto_reply_message",
                "I'm in a meeting. Will call you back soon!"
            ) ?: "I'm in a meeting. Will call you back soon!"

            Log.i(TAG, "📱 Attempting to send auto-reply SMS...")
            Log.i(TAG, "   💬 Message: \"$message\"")

            // ✅ Try to get the caller's number from recent call logs
            val callerNumber = getLastIncomingCallNumber()

            if (!callerNumber.isNullOrBlank()) {
                Log.i(TAG, "📞 Found caller number: $callerNumber")

                // Send SMS to the actual caller
                if (hasSmsPermission()) {
                    try {
                        val smsManager = SmsManager.getDefault()
                        smsManager.sendTextMessage(callerNumber, null, message, null, null)

                        Log.i(TAG, "✅ SMS AUTO-REPLY SENT to: $callerNumber")

                        // ✅ ADD: Track SMS success in bell notifications
                        notificationManager.addSmsReply(callerNumber, message, true)

                        // Show notification to YOU that SMS was sent
                        showSmsSuccessNotification(callerNumber, message)

                        // Record that we sent SMS to this number
                        sentNumbers[callerNumber] = System.currentTimeMillis()

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to send SMS", e)
                        // ✅ ADD: Track SMS failure in bell notifications
                        notificationManager.addSmsReply(callerNumber, "Send failed", false)
                        showSmsFailureNotification(message)
                    }
                } else {
                    Log.e(TAG, "❌ SMS permission not granted")
                    // ✅ ADD: Track SMS failure in bell notifications
                    notificationManager.addSmsReply(callerNumber, "Permission denied", false)
                    showSmsPermissionNotification()
                }
            } else {
                Log.w(TAG, "⚠️ Could not determine caller number - showing notification only")
                // ✅ ADD: Track that call was received but no SMS possible
                notificationManager.addSmsReply("Unknown caller", "No number available", false)
                showUniversalCallNotification(message)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send universal auto-reply", e)
        }
    }
    private fun getLastIncomingCallNumber(): String? {
        return try {
            // Check if we have call log permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "⚠️ READ_CALL_LOG permission not granted")
                return null
            }

            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.TYPE,
                    android.provider.CallLog.Calls.DATE
                ),
                "${android.provider.CallLog.Calls.TYPE} = ?",
                arrayOf(android.provider.CallLog.Calls.INCOMING_TYPE.toString()),
                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 1"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                    val number = it.getString(numberIndex)

                    if (!number.isNullOrBlank()) {
                        Log.d(TAG, "📱 Found incoming call number: $number")
                        return number
                    }
                }
            }

            Log.d(TAG, "⚠️ No recent incoming call number found")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading call log", e)
            null
        }
    }

    /**
     * ✅ NEW: Show success notification when SMS is sent
     */
    private fun showSmsSuccessNotification(phoneNumber: String, message: String) {
        try {
            NotificationHelper.sendNotification(
                context = context,
                title = "✅ Auto-Reply SMS Sent",
                message = "Sent to $phoneNumber: \"$message\"",
                priority = NotificationCompat.PRIORITY_LOW,
                notificationId = (System.currentTimeMillis() % 10000).toInt()
            )
            Log.d(TAG, "✅ SMS success notification sent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing SMS success notification", e)
        }
    }

    /**
     * ✅ NEW: Show failure notification when SMS fails
     */
    private fun showSmsFailureNotification(message: String) {
        try {
            NotificationHelper.sendNotification(
                context = context,
                title = "❌ Auto-Reply Failed",
                message = "Could not send SMS: \"$message\"",
                priority = NotificationCompat.PRIORITY_DEFAULT,
                notificationId = (System.currentTimeMillis() % 10000).toInt()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing SMS failure notification", e)
        }
    }

    /**
     * ✅ NEW: Show permission notification
     */
    private fun showSmsPermissionNotification() {
        try {
            NotificationHelper.sendNotification(
                context = context,
                title = "⚠️ SMS Permission Required",
                message = "Enable SMS permission to send auto-reply messages",
                priority = NotificationCompat.PRIORITY_HIGH,
                notificationId = (System.currentTimeMillis() % 10000).toInt()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing permission notification", e)
        }
    }


    private fun showUniversalCallNotification(message: String) {
        try {
            NotificationHelper.sendNotification(
                context = context,
                title = "📞 Call During Meeting (No SMS)",
                message = "Someone called during meeting mode. Could not send auto-reply SMS (unknown number).",
                priority = NotificationCompat.PRIORITY_HIGH,
                notificationId = (System.currentTimeMillis() % 10000).toInt()
            )
            Log.d(TAG, "✅ Universal call notification sent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing universal notification", e)
        }
    }
    // Add this method to your AutoReplyManager class
    fun handleIncomingCallFromUnknown(context: Context) {
        Log.d(TAG, "🔥 handleIncomingCallFromUnknown called")

        coroutineScope.launch {
            try {
                if (shouldSendAutoReplyForUnknownNumber()) {
                    // Send notification instead of SMS (since we don't have a number)
                    showIncomingCallNotification()
                    Log.d(TAG, "✅ Handled incoming call from unknown number")
                } else {
                    Log.d(TAG, "⚠️ Auto-reply not needed for unknown caller")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing unknown caller", e)
            }
        }
    }

    private suspend fun shouldSendAutoReplyForUnknownNumber(): Boolean {
        // Check if auto-reply is enabled in settings
        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        val autoReplyEnabled = prefs.getBoolean(Constants.PREF_AUTO_REPLY_ENABLED, false)

        if (!autoReplyEnabled) {
            Log.d(TAG, "⚠️ Auto-reply disabled in settings")
            return false
        }

        // Check if we should only reply during DND/meeting mode
        val onlyInDnd = prefs.getBoolean(Constants.PREF_AUTO_REPLY_ONLY_IN_DND, true)

        if (onlyInDnd) {
            // Only check if actual meeting mode is active, not DND status
            if (!isMeetingModeActive()) {
                Log.d(TAG, "⚠️ Meeting mode is not active, skipping auto-reply")
                return false
            }
            // If we are here, meeting mode is active, so proceed.
        }

        return true
    }

    private fun showIncomingCallNotification() {
        try {
            NotificationHelper.sendNotification(
                context = context,
                title = "Incoming Call in Meeting Mode",
                message = "Someone called while you're in meeting mode. Consider calling them back later.",
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
            Log.d(TAG, "✅ Incoming call notification sent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification", e)
        }
    }


    private fun hasSmsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showAutoReplySentNotification(phoneNumber: String, message: String) {
        try {
            NotificationHelper.sendNotification(
                context = context,
                title = "Auto-reply sent",
                message = "Replied to $phoneNumber: \"$message\"",
                priority = NotificationCompat.PRIORITY_LOW, // ✅ FIXED: Use Int constant
                notificationId = (System.currentTimeMillis() % 10000).toInt()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification", e)
        }
    }


    fun cleanup() {
        coroutineScope.cancel()
        sentNumbers.clear()
        INSTANCE = null
    }
}
