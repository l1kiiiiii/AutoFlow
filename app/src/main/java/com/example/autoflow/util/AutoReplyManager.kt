package com.example.autoflow.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.autoflow.data.AppDatabase
import kotlinx.coroutines.*

class AutoReplyManager private constructor(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sentNumbers = mutableMapOf<String, Long>()

    companion object {
        private const val TAG = "AutoReplyManager"

        @Volatile
        private var INSTANCE: AutoReplyManager? = null

        fun getInstance(context: Context): AutoReplyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutoReplyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
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
            return
        }

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

            // Show notification that auto-reply was sent
            showAutoReplySentNotification(phoneNumber, message)

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SMS permission denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send auto-reply SMS", e)
        }
    }

    private fun isInDndMode(): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Check if Do Not Disturb is enabled
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL ||
                    // Or if ringer mode is silent/vibrate
                    audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking DND status", e)
            false
        }
    }

    private suspend fun isMeetingModeActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val workflows = database.workflowDao().getAllEnabledSync()

            // Check if any workflow has "meeting" in name or sets sound mode to DND/Silent
            workflows.any { workflow ->
                val isMeetingWorkflow = workflow.workflowName.contains("meeting", ignoreCase = true) ||
                        workflow.workflowName.contains("dnd", ignoreCase = true) ||
                        workflow.workflowName.contains("silent", ignoreCase = true)

                val hasDndAction = workflow.actionDetails.contains("DND") ||
                        workflow.actionDetails.contains("Silent") ||
                        workflow.actionDetails.contains("SET_SOUND_MODE")

                isMeetingWorkflow || hasDndAction
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking meeting mode workflows", e)
            false
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
