package com.example.autoflow.util

import android.content.Context
import android.util.Log
import com.example.autoflow.integrations.PhoneStateManager
import com.example.autoflow.integrations.SoundModeManager
import com.example.autoflow.model.AppNotification
import com.example.autoflow.model.NotificationType
import com.example.autoflow.util.InAppNotificationManager.Companion.MAX_NOTIFICATIONS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * ✅ PERSISTENT: Manages notifications that stay until manually cleared
 */
class InAppNotificationManager private constructor(private val context: Context) {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _meetingModeActive = MutableStateFlow(false)
    val meetingModeActive: StateFlow<Boolean> = _meetingModeActive.asStateFlow()

    companion object {
        private const val TAG = "InAppNotificationManager"
        private const val MAX_NOTIFICATIONS = 100  // ✅ INCREASED: Keep more notifications

        @Volatile
        private var INSTANCE: InAppNotificationManager? = null

        fun getInstance(context: Context): InAppNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InAppNotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * ✅ PERSISTENT: Add notification that stays until manually cleared
     */
    fun addNotification(
        type: NotificationType,
        title: String,
        message: String,
        isClearable: Boolean = true,
        actionData: Map<String, String>? = null
    ) {
        val notification = AppNotification(
            type = type,
            title = title,
            message = message,
            isRead = false,  // ✅ Always start as unread
            isClearable = isClearable,
            actionData = actionData
        )

        val currentList = _notifications.value.toMutableList()
        currentList.add(0, notification) // Add to top

        // Keep only recent notifications
        if (currentList.size > MAX_NOTIFICATIONS) {
            currentList.removeAt(currentList.size - 1)
        }

        _notifications.value = currentList
        updateUnreadCount()

        Log.d(TAG, "➕ Added persistent notification: $title")
    }

    /**
     * Add task execution notification
     */
    fun addTaskExecution(workflowName: String, actionsCount: Int, success: Boolean) {
        addNotification(
            type = if (success) NotificationType.TASK_EXECUTED else NotificationType.ERROR,
            title = if (success) "✅ Task Executed" else "❌ Task Failed",
            message = if (success)
                "\"$workflowName\" executed $actionsCount actions successfully"
            else
                "\"$workflowName\" execution failed",
            isClearable = true,
            actionData = mapOf("workflow" to workflowName, "success" to success.toString())
        )
    }

    /**
     * Add SMS auto-reply notification
     */
    fun addSmsReply(phoneNumber: String, message: String, success: Boolean) {
        Log.d(TAG, "📝 SMS Reply: $phoneNumber - Success: $success - Message: $message")
        addNotification(
            type = if (success) NotificationType.SMS_SENT else NotificationType.ERROR,
            title = if (success) "📱 Auto-Reply Sent" else "❌ Auto-Reply Failed",
            message = if (success)
                "SMS sent to $phoneNumber: \"$message\""
            else
                "Failed to send SMS to $phoneNumber",
            isClearable = true,
            actionData = mapOf("phone" to phoneNumber, "message" to message)
        )
    }

    /**
     * ✅ PERSISTENT: Update meeting mode status
     */
    fun setMeetingMode(active: Boolean, workflowName: String? = null) {
        _meetingModeActive.value = active

        // Remove any existing meeting mode notification
        val currentList = _notifications.value.toMutableList()
        currentList.removeAll { it.type == NotificationType.MEETING_MODE }

        if (active) {
            // Add non-clearable meeting mode notification that persists
            val meetingNotification = AppNotification(
                type = NotificationType.MEETING_MODE,
                title = "🔇 Meeting Mode Active",
                message = workflowName?.let { "\"$it\" is running - Tap deactivate to stop" }
                    ?: "DND mode enabled - Tap deactivate to stop",
                isRead = false,  // ✅ Keep as unread until deactivated
                isClearable = false, // ✅ Cannot be cleared manually
                actionData = mapOf("workflow" to (workflowName ?: ""), "action" to "deactivate")
            )
            currentList.add(0, meetingNotification)
        }

        _notifications.value = currentList
        updateUnreadCount()

        Log.d(TAG, if (active) "🔇 Meeting mode activated (persistent)" else "🔔 Meeting mode deactivated")
    }

    /**
     * ✅ MANUAL: Mark notification as read (only when user explicitly interacts)
     */
    fun markAsRead(notificationId: String) {
        val currentList = _notifications.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == notificationId }

        if (index >= 0) {
            currentList[index] = currentList[index].copy(isRead = true)
            _notifications.value = currentList
            updateUnreadCount()
            Log.d(TAG, "📖 Notification marked as read: ${currentList[index].title}")
        }
    }

    /**
     * ✅ MANUAL: Mark all notifications as read
     */
    fun markAllAsRead() {
        val currentList = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = currentList
        updateUnreadCount()
        Log.d(TAG, "📖 All notifications marked as read")
    }

    /**
     * ✅ MANUAL: Clear a notification (only if clearable)
     */
    fun clearNotification(notificationId: String): Boolean {
        val currentList = _notifications.value.toMutableList()
        val notification = currentList.find { it.id == notificationId }

        return if (notification?.isClearable == true) {
            currentList.removeAll { it.id == notificationId }
            _notifications.value = currentList
            updateUnreadCount()
            Log.d(TAG, "🗑️ Cleared notification: ${notification.title}")
            true
        } else {
            Log.w(TAG, "⚠️ Cannot clear non-clearable notification: ${notification?.title}")
            false
        }
    }

    /**
     * ✅ MANUAL: Clear all clearable notifications
     */
    fun clearAllClearable() {
        val currentList = _notifications.value.toMutableList()
        val originalSize = currentList.size

        // Only remove clearable notifications
        currentList.removeAll { it.isClearable }

        _notifications.value = currentList
        updateUnreadCount()

        val cleared = originalSize - currentList.size
        Log.d(TAG, "🗑️ Cleared $cleared notifications (kept ${currentList.size} non-clearable)")
    }

    /**
     * ✅ DEACTIVATE: Deactivate meeting mode and remove its notification
     */
    fun deactivateMeetingMode(): Boolean {
        return if (_meetingModeActive.value) {
            try {
                val soundModeManager = SoundModeManager(context)
                val success = soundModeManager.setSoundMode("Normal")

                if (success) {
                    // Remove meeting mode notification
                    setMeetingMode(false)

                    // ✅ ADD: Clear SharedPreferences flags
                    val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("auto_reply_enabled", false)
                        .putBoolean("manual_meeting_mode", false)
                        .apply()

                    // ✅ ADD: Stop phone monitoring
                    val phoneStateManager = PhoneStateManager.getInstance(context)
                    phoneStateManager.stopListening()

                    // ✅ ADD: Deactivation success notification
                    addNotification(
                        type = NotificationType.SUCCESS,
                        title = "🔔 Meeting Mode Deactivated",
                        message = "Sound mode restored. Auto-reply disabled.",
                        isClearable = true
                    )

                    Log.d(TAG, "🔔 Meeting mode deactivated successfully")
                    Log.d(TAG, "🚩 Set auto_reply_enabled = false")
                    Log.d(TAG, "🚩 Set manual_meeting_mode = false")
                    true
                } else {
                    Log.e(TAG, "❌ Failed to restore sound mode")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deactivating meeting mode", e)
                false
            }
        } else {
            Log.d(TAG, "⚠️ Meeting mode was not active")
            false
        }
    }

    /**
     * ✅ PERSISTENT: Count unread notifications accurately
     */
    private fun updateUnreadCount() {
        val unreadCount = _notifications.value.count { !it.isRead }
        _unreadCount.value = unreadCount
        Log.d(TAG, "📊 Unread count updated: $unreadCount")
    }

    fun cleanup() {
        INSTANCE = null
    }
}
