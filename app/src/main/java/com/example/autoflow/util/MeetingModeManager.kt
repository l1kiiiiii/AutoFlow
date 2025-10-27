package com.example.autoflow.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.MeetingModeEntity
import com.example.autoflow.integrations.PhoneStateManager
import com.example.autoflow.receiver.MeetingModeReceiver
import kotlinx.coroutines.*
import java.util.*

class MeetingModeManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MeetingModeManager"
        private const val MEETING_MODE_REQUEST_CODE = 1001
        private const val END_MEETING_REQUEST_CODE = 1002

        @Volatile
        private var INSTANCE: MeetingModeManager? = null

        fun getInstance(context: Context): MeetingModeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeetingModeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val database = AppDatabase.getDatabase(context)
    private val meetingModeDao = database.meetingModeDao()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // State tracking
    private var previousRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var previousDndState: Boolean = false
    private var activeMeetingMode: MeetingModeEntity? = null

    /**
     * Start a meeting mode immediately
     */
    fun startMeetingModeImmediate(
        name: String = "Meeting Mode",
        endType: String = "MANUAL",
        durationMinutes: Int? = null,
        autoReplyMessage: String = "I'm currently in a meeting and will get back to you shortly.",
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        Log.d(TAG, "🚀 Starting immediate meeting mode: $name")

        scope.launch {
            try {
                // Check if any meeting mode is already active
                val existing = meetingModeDao.getActiveMeetingMode()
                if (existing != null) {
                    callback?.invoke(false, "Meeting mode '${existing.name}' is already active")
                    return@launch
                }

                // Create new meeting mode
                val meetingMode = MeetingModeEntity.createImmediate(
                    name = name,
                    endType = endType,
                    durationMinutes = durationMinutes,
                    autoReplyMessage = autoReplyMessage
                )

                // Save to database
                val id = meetingModeDao.insert(meetingMode)
                meetingMode.id = id

                // Start meeting mode
                startMeetingModeInternal(meetingMode, callback)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting immediate meeting mode", e)
                callback?.invoke(false, "Error starting meeting mode: ${e.message}")
            }
        }
    }

    /**
     * Schedule a meeting mode for later
     */
    fun scheduleMeetingMode(
        name: String = "Scheduled Meeting",
        startTime: Long,
        endType: String = "DURATION",
        durationMinutes: Int? = 60,
        endTime: Long? = null,
        autoReplyMessage: String = "I'm currently in a meeting and will get back to you shortly.",
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        Log.d(TAG, "📅 Scheduling meeting mode: $name for ${Date(startTime)}")

        scope.launch {
            try {
                // Validate start time
                if (startTime <= System.currentTimeMillis()) {
                    callback?.invoke(false, "Start time must be in the future")
                    return@launch
                }

                // Create scheduled meeting mode
                val meetingMode = MeetingModeEntity.createScheduled(
                    name = name,
                    startTime = startTime,
                    endType = endType,
                    durationMinutes = durationMinutes,
                    endTime = endTime,
                    autoReplyMessage = autoReplyMessage
                )

                // Save to database
                val id = meetingModeDao.insert(meetingMode)
                meetingMode.id = id

                // Schedule alarm for start time
                scheduleStartAlarm(meetingMode)

                // If has end time, schedule end alarm too
                val effectiveEndTime = meetingMode.getEffectiveEndTime()
                if (effectiveEndTime != null) {
                    scheduleEndAlarm(meetingMode, effectiveEndTime)
                }

                Log.d(TAG, "✅ Meeting mode scheduled successfully")
                callback?.invoke(true, "Meeting mode scheduled for ${formatTime(startTime)}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error scheduling meeting mode", e)
                callback?.invoke(false, "Error scheduling meeting mode: ${e.message}")
            }
        }
    }

    /**
     * Stop active meeting mode
     */
    fun stopMeetingMode(callback: ((Boolean, String) -> Unit)? = null) {
        Log.d(TAG, "🛑 Stopping meeting mode")

        scope.launch {
            try {
                val activeMeeting = meetingModeDao.getActiveMeetingMode()
                if (activeMeeting == null) {
                    callback?.invoke(false, "No active meeting mode found")
                    return@launch
                }

                // Stop meeting mode
                stopMeetingModeInternal(activeMeeting, callback)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error stopping meeting mode", e)
                callback?.invoke(false, "Error stopping meeting mode: ${e.message}")
            }
        }
    }

    /**
     * Get current active meeting mode
     */
    fun getActiveMeetingMode(callback: (MeetingModeEntity?) -> Unit) {
        scope.launch {
            try {
                val activeMeeting = meetingModeDao.getActiveMeetingMode()

                // Check if expired
                if (activeMeeting?.isExpired() == true) {
                    Log.d(TAG, "⏰ Active meeting mode expired, stopping automatically")
                    stopMeetingModeInternal(activeMeeting)
                    callback(null)
                } else {
                    callback(activeMeeting)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting active meeting mode", e)
                callback(null)
            }
        }
    }

    /**
     * Internal method to start meeting mode
     */
    private suspend fun startMeetingModeInternal(
        meetingMode: MeetingModeEntity,
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        try {
            Log.d(TAG, "🎯 Starting meeting mode internally: ${meetingMode.name}")

            // Save current state
            saveCurrentState()

            // Update meeting mode as active
            meetingMode.isActive = true
            meetingMode.lastStarted = System.currentTimeMillis()
            meetingModeDao.update(meetingMode)

            // Apply meeting mode settings
            if (meetingMode.dndEnabled) {
                activateDND()
            }

            if (meetingMode.autoReplyEnabled) {
                activateAutoReply(meetingMode.autoReplyMessage)
            }

            if (meetingMode.appBlockingEnabled) {
                activateAppBlocking(meetingMode.getBlockedAppsList())
            }

            // Schedule end if needed
            val endTime = meetingMode.getEffectiveEndTime()
            if (endTime != null) {
                scheduleEndAlarm(meetingMode, endTime)
                Log.d(TAG, "⏰ Scheduled automatic end for ${Date(endTime)}")
            }

            activeMeetingMode = meetingMode

            Log.d(TAG, "✅ Meeting mode '${meetingMode.name}' started successfully")
            callback?.invoke(true, "Meeting mode '${meetingMode.name}' started")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in startMeetingModeInternal", e)
            callback?.invoke(false, "Failed to start meeting mode: ${e.message}")
        }
    }

    /**
     * Internal method to stop meeting mode
     */
    private suspend fun stopMeetingModeInternal(
        meetingMode: MeetingModeEntity,
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        try {
            Log.d(TAG, "🎯 Stopping meeting mode internally: ${meetingMode.name}")

            // Update meeting mode as inactive
            meetingMode.isActive = false
            meetingMode.lastEnded = System.currentTimeMillis()
            meetingModeDao.update(meetingMode)

            // Restore previous state
            restorePreviousState()

            // Deactivate features
            deactivateAutoReply()
            deactivateAppBlocking()

            // Cancel any pending alarms
            cancelAlarms(meetingMode)

            activeMeetingMode = null

            Log.d(TAG, "✅ Meeting mode '${meetingMode.name}' stopped successfully")
            callback?.invoke(true, "Meeting mode '${meetingMode.name}' stopped")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in stopMeetingModeInternal", e)
            callback?.invoke(false, "Failed to stop meeting mode: ${e.message}")
        }
    }

    /**
     * Save current device state
     */
    private fun saveCurrentState() {
        try {
            previousRingerMode = audioManager.ringerMode

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                previousDndState = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            }

            // Save to preferences for persistence
            val prefs = context.getSharedPreferences("meeting_mode_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("previous_ringer_mode", previousRingerMode)
                .putBoolean("previous_dnd_state", previousDndState)
                .apply()

            Log.d(TAG, "💾 Saved current state - Ringer: $previousRingerMode, DND: $previousDndState")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving current state", e)
        }
    }

    /**
     * Restore previous device state
     */
    private fun restorePreviousState() {
        try {
            // Restore from preferences if needed
            val prefs = context.getSharedPreferences("meeting_mode_prefs", Context.MODE_PRIVATE)
            val savedRingerMode = prefs.getInt("previous_ringer_mode", AudioManager.RINGER_MODE_NORMAL)
            val savedDndState = prefs.getBoolean("previous_dnd_state", false)

            // Restore ringer mode
            audioManager.ringerMode = savedRingerMode

            // Restore DND state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && notificationManager.isNotificationPolicyAccessGranted) {
                val targetFilter = if (savedDndState) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
                notificationManager.setInterruptionFilter(targetFilter)
            }

            Log.d(TAG, "🔄 Restored previous state - Ringer: $savedRingerMode, DND: $savedDndState")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error restoring previous state", e)
        }
    }

    /**
     * Activate Do Not Disturb mode
     */
    private fun activateDND() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                Log.d(TAG, "🔕 DND activated successfully")
            } else {
                // Fallback to silent mode
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                Log.d(TAG, "🔕 Silent mode activated (DND permission not available)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error activating DND", e)
        }
    }

    /**
     * Activate auto-reply for SMS/calls
     */
    private fun activateAutoReply(message: String) {
        try {
            val phoneStateManager = PhoneStateManager.getInstance(context)
            phoneStateManager.setAutoReplyMessage(message)
            phoneStateManager.startListening()
            Log.d(TAG, "📱 Auto-reply activated with message: $message")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error activating auto-reply", e)
        }
    }

    /**
     * Deactivate auto-reply
     */
    private fun deactivateAutoReply() {
        try {
            val phoneStateManager = PhoneStateManager.getInstance(context)
            phoneStateManager.stopListening()
            Log.d(TAG, "📱 Auto-reply deactivated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deactivating auto-reply", e)
        }
    }

    /**
     * Activate app blocking
     */
    private fun activateAppBlocking(blockedApps: List<String>) {
        try {
            if (blockedApps.isEmpty()) return

            // Implementation depends on your existing AppBlocker
            // This is a placeholder for app blocking logic
            Log.d(TAG, "🚫 App blocking activated for ${blockedApps.size} apps")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error activating app blocking", e)
        }
    }

    /**
     * Deactivate app blocking
     */
    private fun deactivateAppBlocking() {
        try {
            // Implementation depends on your existing AppBlocker
            Log.d(TAG, "✅ App blocking deactivated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deactivating app blocking", e)
        }
    }

    /**
     * Schedule alarm to start meeting mode
     */
    private fun scheduleStartAlarm(meetingMode: MeetingModeEntity) {
        val startTime = meetingMode.scheduledStartTime ?: return

        val intent = Intent(context, MeetingModeReceiver::class.java).apply {
            action = "START_MEETING_MODE"
            putExtra("meeting_mode_id", meetingMode.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MEETING_MODE_REQUEST_CODE + meetingMode.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, startTime, pendingIntent)
        }

        Log.d(TAG, "⏰ Scheduled start alarm for ${Date(startTime)}")
    }

    /**
     * Schedule alarm to end meeting mode
     */
    private fun scheduleEndAlarm(meetingMode: MeetingModeEntity, endTime: Long) {
        val intent = Intent(context, MeetingModeReceiver::class.java).apply {
            action = "END_MEETING_MODE"
            putExtra("meeting_mode_id", meetingMode.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            END_MEETING_REQUEST_CODE + meetingMode.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
        }

        Log.d(TAG, "⏰ Scheduled end alarm for ${Date(endTime)}")
    }

    /**
     * Cancel all alarms for meeting mode
     */
    private fun cancelAlarms(meetingMode: MeetingModeEntity) {
        // Cancel start alarm
        val startIntent = Intent(context, MeetingModeReceiver::class.java)
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            MEETING_MODE_REQUEST_CODE + meetingMode.id.toInt(),
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        startPendingIntent?.let { alarmManager.cancel(it) }

        // Cancel end alarm
        val endIntent = Intent(context, MeetingModeReceiver::class.java)
        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            END_MEETING_REQUEST_CODE + meetingMode.id.toInt(),
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        endPendingIntent?.let { alarmManager.cancel(it) }

        Log.d(TAG, "🚫 Cancelled alarms for meeting mode: ${meetingMode.name}")
    }

    /**
     * Check for expired meeting modes
     */
    fun checkExpiredMeetingModes() {
        scope.launch {
            try {
                val activeMeeting = meetingModeDao.getActiveMeetingMode()
                if (activeMeeting?.isExpired() == true) {
                    Log.d(TAG, "⏰ Meeting mode expired, stopping automatically")
                    stopMeetingModeInternal(activeMeeting)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking expired meeting modes", e)
            }
        }
    }

    /**
     * Format time for display
     */
    private fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        return java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(date)
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            scope.cancel()
            Log.d(TAG, "🧹 MeetingModeManager cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during cleanup", e)
        }
    }
}
