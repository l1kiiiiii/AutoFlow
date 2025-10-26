package com.example.autoflow.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * ✅ ENHANCED NotificationHelper - Complete Version
 * Combines existing functionality with Meeting Mode features
 * Handles notification channels, creation, and display with backward compatibility
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val DEFAULT_NOTIFICATION_ID = 1000

    // ✅ EXISTING + NEW notification channels (merged)
    private const val CHANNEL_ID_DEFAULT = "autoflow_default"
    private const val CHANNEL_ID_HIGH_PRIORITY = "autoflow_high_priority"
    private const val CHANNEL_ID_LOW_PRIORITY = "autoflow_low_priority"
    private const val CHANNEL_ID_SCRIPT = "autoflow_script"
    private const val CHANNEL_ID_ERROR = "autoflow_error"

    // ✅ NEW Meeting Mode channels
    private const val CHANNEL_GENERAL = "autoflow_general"
    private const val CHANNEL_AUTO_REPLY = "autoflow_auto_reply"
    private const val CHANNEL_MEETING_MODE = "autoflow_meeting_mode"

    // Channel names
    private const val CHANNEL_NAME_DEFAULT = "AutoFlow Notifications"
    private const val CHANNEL_NAME_HIGH = "Important AutoFlow Alerts"
    private const val CHANNEL_NAME_LOW = "AutoFlow Updates"
    private const val CHANNEL_NAME_SCRIPT = "Script Notifications"
    private const val CHANNEL_NAME_ERROR = "Error Notifications"

    // ✅ COMPATIBLE Priority constants
    const val PRIORITY_LOW = NotificationCompat.PRIORITY_LOW
    const val PRIORITY_HIGH = NotificationCompat.PRIORITY_HIGH
    const val PRIORITY_DEFAULT = NotificationCompat.PRIORITY_DEFAULT

    /**
     * ✅ ENHANCED: Create all notification channels (both existing + new Meeting Mode)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val channels = listOf(
                // ✅ EXISTING channels (enhanced)
                NotificationChannel(
                    CHANNEL_ID_DEFAULT,
                    CHANNEL_NAME_DEFAULT,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General AutoFlow notifications"
                    enableVibration(true)
                    enableLights(true)
                },

                NotificationChannel(
                    CHANNEL_ID_HIGH_PRIORITY,
                    CHANNEL_NAME_HIGH,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Important AutoFlow alerts"
                    enableVibration(true)
                    enableLights(true)
                },

                NotificationChannel(
                    CHANNEL_ID_LOW_PRIORITY,
                    CHANNEL_NAME_LOW,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "AutoFlow status updates"
                    enableVibration(false)
                    enableLights(false)
                },

                NotificationChannel(
                    CHANNEL_ID_SCRIPT,
                    CHANNEL_NAME_SCRIPT,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications from user scripts"
                    enableVibration(true)
                },

                NotificationChannel(
                    CHANNEL_ID_ERROR,
                    CHANNEL_NAME_ERROR,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Error notifications"
                    enableVibration(true)
                    enableLights(true)
                },

                // ✅ NEW Meeting Mode channels
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General AutoFlow notifications (Meeting Mode compatible)"
                },

                NotificationChannel(
                    CHANNEL_AUTO_REPLY,
                    "Auto-Reply Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications when auto-reply SMS is sent"
                },

                NotificationChannel(
                    CHANNEL_MEETING_MODE,
                    "Meeting Mode",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Meeting mode status notifications"
                }
            )

            notificationManager.createNotificationChannels(channels)
            Log.d(TAG, "✅ All notification channels created (${channels.size} channels)")
        }
    }

    /**
     * ✅ PRIMARY: Enhanced sendNotification method with full compatibility
     * Supports both existing and Meeting Mode calls
     */
    @JvmStatic
    @JvmOverloads
    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        channelId: String? = null // ✅ Optional for backward compatibility
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "⚠️ Notification permission not granted")
            return
        }

        try {
            // ✅ Smart channel selection
            val targetChannelId = channelId ?: getChannelIdForIntPriority(priority)

            val builder = NotificationCompat.Builder(context, targetChannelId)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            // ✅ Add content intent to open app
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(pendingIntent)
            }

            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
                Log.d(TAG, "✅ Notification sent: $title")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ SecurityException: Missing notification permission", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification", e)
        }
    }

    /**
     * ✅ EXISTING: Enhanced notification with custom action
     */
    @JvmStatic
    fun sendNotificationWithAction(
        context: Context,
        title: String,
        message: String,
        actionTitle: String,
        actionIntent: PendingIntent,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        notificationId: Int = DEFAULT_NOTIFICATION_ID
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "⚠️ Notification permission not granted")
            return
        }

        try {
            val channelId = getChannelIdForIntPriority(priority)

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .addAction(0, actionTitle, actionIntent)

            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
                Log.d(TAG, "✅ Notification with action sent: $title")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ SecurityException: Missing notification permission", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification", e)
        }
    }

    /**
     * ✅ EXISTING: Script notification (for user scripts)
     */
    @JvmStatic
    fun sendScriptNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "⚠️ Notification permission not granted")
            return
        }

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID_SCRIPT)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
                Log.d(TAG, "✅ Script notification sent: $title")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ SecurityException: Missing notification permission", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending script notification", e)
        }
    }

    /**
     * ✅ EXISTING: Send error notification
     */
    @JvmStatic
    fun sendErrorNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "⚠️ Notification permission not granted")
            return
        }

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID_ERROR)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
                Log.d(TAG, "✅ Error notification sent: $title")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ SecurityException: Missing notification permission", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending error notification", e)
        }
    }

    /**
     * ✅ EXISTING: Send progress notification
     */
    @JvmStatic
    fun sendProgressNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int,
        maxProgress: Int = 100,
        notificationId: Int = DEFAULT_NOTIFICATION_ID
    ) {
        if (!hasNotificationPermission(context)) return

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(maxProgress, progress, false)
                .setOngoing(true)

            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ SecurityException: Missing notification permission", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending progress notification", e)
        }
    }

    /**
     * ✅ NEW: Meeting Mode specific notifications
     */
    @JvmStatic
    fun sendMeetingModeNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        sendNotification(
            context = context,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH,
            notificationId = notificationId,
            channelId = CHANNEL_MEETING_MODE
        )
    }

    /**
     * ✅ NEW: Auto-reply specific notifications
     */
    @JvmStatic
    fun sendAutoReplyNotification(
        context: Context,
        phoneNumber: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        sendNotification(
            context = context,
            title = "📱 Auto-Reply Sent",
            message = "Replied to $phoneNumber: $message",
            priority = NotificationCompat.PRIORITY_LOW,
            notificationId = notificationId,
            channelId = CHANNEL_AUTO_REPLY
        )
    }

    /**
     * ✅ EXISTING: Cancel a notification
     */
    @JvmStatic
    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
            Log.d(TAG, "✅ Notification cancelled: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling notification", e)
        }
    }

    /**
     * ✅ EXISTING: Cancel all notifications
     */
    @JvmStatic
    fun cancelAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
            Log.d(TAG, "✅ All notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling all notifications", e)
        }
    }

    /**
     * ✅ EXISTING: Check if notifications are enabled
     */
    @JvmStatic
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // ✅ PRIVATE HELPER METHODS

    /**
     * ✅ NEW: Smart channel selection for Int priority
     */
    private fun getChannelIdForIntPriority(priority: Int): String {
        return when (priority) {
            NotificationCompat.PRIORITY_HIGH, NotificationCompat.PRIORITY_MAX -> CHANNEL_ID_HIGH_PRIORITY
            NotificationCompat.PRIORITY_LOW, NotificationCompat.PRIORITY_MIN -> CHANNEL_ID_LOW_PRIORITY
            else -> CHANNEL_ID_DEFAULT
        }
    }

    /**
     * ✅ EXISTING: String priority support (backward compatibility)
     */
    private fun getChannelId(priority: String): String {
        return when (priority.uppercase()) {
            "HIGH" -> CHANNEL_ID_HIGH_PRIORITY
            "LOW" -> CHANNEL_ID_LOW_PRIORITY
            else -> CHANNEL_ID_DEFAULT
        }
    }

    /**
     * ✅ EXISTING: String to Int priority conversion
     */
    private fun getPriorityInt(priority: String): Int {
        return when (priority.uppercase()) {
            "HIGH" -> NotificationCompat.PRIORITY_HIGH
            "LOW" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    /**
     * ✅ EXISTING: Get notification icon
     */
    private fun getNotificationIcon(): Int {
        // You can replace with your app's actual icon resource
        return android.R.drawable.ic_dialog_info // Placeholder
    }

    /**
     * ✅ EXISTING: Check notification permission
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
