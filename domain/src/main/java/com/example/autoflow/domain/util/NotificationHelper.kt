package com.example.autoflow.domain.util

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
 * Helper class for managing notifications
 * Handles notification channels, creation, and display
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val DEFAULT_NOTIFICATION_ID = 1000

    // Notification channels
    private const val CHANNEL_ID_DEFAULT = "autoflow_default"
    private const val CHANNEL_ID_HIGH_PRIORITY = "autoflow_high_priority"
    private const val CHANNEL_ID_LOW_PRIORITY = "autoflow_low_priority"
    private const val CHANNEL_ID_SCRIPT = "autoflow_script"
    private const val CHANNEL_ID_ERROR = "autoflow_error"

    private const val CHANNEL_NAME_DEFAULT = "AutoFlow Notifications"
    private const val CHANNEL_NAME_HIGH = "Important AutoFlow Alerts"
    private const val CHANNEL_NAME_LOW = "AutoFlow Updates"
    private const val CHANNEL_NAME_SCRIPT = "Script Notifications"
    private const val CHANNEL_NAME_ERROR = "Error Notifications"

    const val PRIORITY_LOW = NotificationCompat.PRIORITY_LOW
    const val PRIORITY_HIGH = NotificationCompat.PRIORITY_HIGH
    const val PRIORITY_DEFAULT = NotificationCompat.PRIORITY_DEFAULT

    /**
     * Create notification channels (Android 8.0+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as? NotificationManager ?: return

            // Default channel
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                CHANNEL_NAME_DEFAULT,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General AutoFlow notifications"
                enableVibration(true)
                enableLights(true)
            }

            // High priority channel
            val highPriorityChannel = NotificationChannel(
                CHANNEL_ID_HIGH_PRIORITY,
                CHANNEL_NAME_HIGH,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important AutoFlow alerts"
                enableVibration(true)
                enableLights(true)
            }

            // Low priority channel
            val lowPriorityChannel = NotificationChannel(
                CHANNEL_ID_LOW_PRIORITY,
                CHANNEL_NAME_LOW,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AutoFlow status updates"
                enableVibration(false)
                enableLights(false)
            }

            // Script notification channel
            val scriptChannel = NotificationChannel(
                CHANNEL_ID_SCRIPT,
                CHANNEL_NAME_SCRIPT,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from user scripts"
                enableVibration(true)
            }

            // Error notification channel
            val errorChannel = NotificationChannel(
                CHANNEL_ID_ERROR,
                CHANNEL_NAME_ERROR,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Error notifications"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, highPriorityChannel, lowPriorityChannel, scriptChannel, errorChannel)
            )

            Log.d(TAG, "✅ Notification channels created")
        }
    }

    /**
     * Send a simple notification - FIXED VERSION
     */
    fun sendNotification(
        context: Context,
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        notificationId: Int = DEFAULT_NOTIFICATION_ID
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "⚠️ Notification permission not granted")
            return
        }

        try {
            // ✅ FIXED: Use Int priority directly
            val channelId = getChannelIdForIntPriority(priority)

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority) // ✅ Use Int directly
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            // Add content intent to open app
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
     * Send notification with custom action - FIXED VERSION
     */
    fun sendNotificationWithAction(
        context: Context,
        title: String,
        message: String,
        actionTitle: String,
        actionIntent: PendingIntent,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT, // ✅ FIXED: Use Int type
        notificationId: Int = DEFAULT_NOTIFICATION_ID
    ) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "⚠️ Notification permission not granted")
            return
        }

        try {
            // ✅ FIXED: Use Int priority directly
            val channelId = getChannelIdForIntPriority(priority)

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority) // ✅ Use Int directly
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
     * Send script notification (for user scripts)
     */
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

    // ✅ NEW: Helper method for Int priority to channel mapping
    private fun getChannelIdForIntPriority(priority: Int): String {
        return when (priority) {
            NotificationCompat.PRIORITY_HIGH, NotificationCompat.PRIORITY_MAX -> CHANNEL_ID_HIGH_PRIORITY
            NotificationCompat.PRIORITY_LOW, NotificationCompat.PRIORITY_MIN -> CHANNEL_ID_LOW_PRIORITY
            else -> CHANNEL_ID_DEFAULT
        }
    }

    // ✅ KEEP: Original String-based method for backward compatibility
    private fun getChannelId(priority: String): String {
        return when (priority.uppercase()) {
            "HIGH" -> CHANNEL_ID_HIGH_PRIORITY
            "LOW" -> CHANNEL_ID_LOW_PRIORITY
            else -> CHANNEL_ID_DEFAULT
        }
    }

    // ✅ KEEP: String to Int conversion method
    private fun getPriorityInt(priority: String): Int {
        return when (priority.uppercase()) {
            "HIGH" -> NotificationCompat.PRIORITY_HIGH
            "LOW" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    /**
     * Send error notification
     */
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
     * Send progress notification
     */
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
     * Cancel a notification
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
            Log.d(TAG, "✅ Notification cancelled: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling notification", e)
        }
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
            Log.d(TAG, "✅ All notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling all notifications", e)
        }
    }

    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    //  PRIVATE HELPER METHODS 


    private fun getNotificationIcon(): Int {
        // Return your app's notification icon
        // Replace with actual resource ID
        return android.R.drawable.ic_dialog_info // Placeholder
    }

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
