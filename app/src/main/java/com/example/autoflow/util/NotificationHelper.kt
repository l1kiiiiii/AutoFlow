// Updated NotificationHelper.java - Fixed version
package com.example.autoflow.util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.autoflow.MainActivity;

public class NotificationHelper {

    // Notification channel constants
    private static final String CHANNEL_ID_DEFAULT = "autoflow_default";
    private static final String CHANNEL_ID_SCRIPT = "autoflow_script";
    private static final String CHANNEL_ID_ERROR = "autoflow_error";
    private static final String CHANNEL_NAME_DEFAULT = "AutoFlow Notifications";
    private static final String CHANNEL_NAME_SCRIPT = "Script Notifications";
    private static final String CHANNEL_NAME_ERROR = "Error Notifications";

    // Notification ID counters
    private static int notificationIdCounter = 1000;

    /**
     * Initialize notification channels for different types of notifications
     */
    public static void createNotificationChannels(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) return;

            // Default channel for general notifications
            NotificationChannel defaultChannel = new NotificationChannel(
                    CHANNEL_ID_DEFAULT,
                    CHANNEL_NAME_DEFAULT,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            defaultChannel.setDescription("General AutoFlow notifications");
            defaultChannel.enableLights(true);
            defaultChannel.setLightColor(Color.BLUE);
            defaultChannel.enableVibration(true);
            notificationManager.createNotificationChannel(defaultChannel);

            // Script execution channel
            NotificationChannel scriptChannel = new NotificationChannel(
                    CHANNEL_ID_SCRIPT,
                    CHANNEL_NAME_SCRIPT,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            scriptChannel.setDescription("Notifications from script execution");
            scriptChannel.enableLights(true);
            scriptChannel.setLightColor(Color.GREEN);
            scriptChannel.enableVibration(false);
            notificationManager.createNotificationChannel(scriptChannel);

            // Error channel for high-priority error notifications
            NotificationChannel errorChannel = new NotificationChannel(
                    CHANNEL_ID_ERROR,
                    CHANNEL_NAME_ERROR,
                    NotificationManager.IMPORTANCE_HIGH
            );
            errorChannel.setDescription("Error notifications from AutoFlow");
            errorChannel.enableLights(true);
            errorChannel.setLightColor(Color.RED);
            errorChannel.enableVibration(true);
            errorChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
            notificationManager.createNotificationChannel(errorChannel);
        }
    }

    /**
     * Send a basic notification with title and message
     */
    public static void sendNotification(@NonNull Context context,
                                        @NonNull String title,
                                        @NonNull String message) {
        sendNotification(context, title, message, CHANNEL_ID_DEFAULT, NotificationCompat.PRIORITY_DEFAULT);
    }

    /**
     * Send a notification with custom priority
     */
    public static void sendNotification(@NonNull Context context,
                                        @NonNull String title,
                                        @NonNull String message,
                                        int priority) {
        String channelId = (priority == NotificationCompat.PRIORITY_HIGH) ? CHANNEL_ID_ERROR : CHANNEL_ID_DEFAULT;
        sendNotification(context, title, message, channelId, priority);
    }

    /**
     * Send a script-related notification
     */
    public static void sendScriptNotification(@NonNull Context context,
                                              @NonNull String title,
                                              @NonNull String message) {
        sendNotification(context, title, message, CHANNEL_ID_SCRIPT, NotificationCompat.PRIORITY_DEFAULT);
    }

    /**
     * Send an error notification
     */
    public static void sendErrorNotification(@NonNull Context context,
                                             @NonNull String title,
                                             @NonNull String message) {
        sendNotification(context, title, message, CHANNEL_ID_ERROR, NotificationCompat.PRIORITY_HIGH);
    }

    /**
     * Core notification sending method
     */
    private static void sendNotification(@NonNull Context context,
                                         @NonNull String title,
                                         @NonNull String message,
                                         @NonNull String channelId,
                                         int priority) {
        try {
            // Create pending intent to open the app when notification is tapped
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE
                            : PendingIntent.FLAG_UPDATE_CURRENT
            );

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(getNotificationIcon(priority))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(priority)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true) // Remove notification when tapped
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message)); // Expandable text

            // Add sound for high priority notifications
            if (priority == NotificationCompat.PRIORITY_HIGH) {
                builder.setDefaults(Notification.DEFAULT_ALL);
            }

            // Send the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            int notificationId = getNextNotificationId();
            notificationManager.notify(notificationId, builder.build());

        } catch (SecurityException e) {
            // Handle case where notification permissions are denied
            android.util.Log.e("NotificationHelper", "Notification permission denied: " + e.getMessage());
        } catch (Exception e) {
            android.util.Log.e("NotificationHelper", "Error sending notification: " + e.getMessage());
        }
    }

    /**
     * Send a rich notification with custom actions - FIXED VERSION
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void sendRichNotification(@NonNull Context context,
                                            @NonNull String title,
                                            @NonNull String message,
                                            @NonNull String actionText,
                                            @NonNull PendingIntent actionIntent) {
        try {
            // Default app open intent
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE
                            : PendingIntent.FLAG_UPDATE_CURRENT
            );

            // Build notification with custom action - USING SYSTEM ICONS
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
                    .setSmallIcon(getNotificationIcon(NotificationCompat.PRIORITY_DEFAULT))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    // FIXED: Using system drawable instead of custom ic_action
                    .addAction(android.R.drawable.ic_media_play, actionText, actionIntent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(getNextNotificationId(), builder.build());

        } catch (Exception e) {
            Log.e("NotificationHelper", "Error sending rich notification: " + e.getMessage());
        }
    }

    /**
     * Send notification with multiple action buttons
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void sendActionNotification(@NonNull Context context,
                                              @NonNull String title,
                                              @NonNull String message,
                                              @NonNull String action1Text,
                                              @NonNull PendingIntent action1Intent,
                                              @NonNull String action2Text,
                                              @NonNull PendingIntent action2Intent) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE
                            : PendingIntent.FLAG_UPDATE_CURRENT
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
                    .setSmallIcon(getNotificationIcon(NotificationCompat.PRIORITY_DEFAULT))
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    // Using system drawables for action buttons
                    .addAction(android.R.drawable.ic_menu_call, action1Text, action1Intent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, action2Text, action2Intent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(getNextNotificationId(), builder.build());

        } catch (Exception e) {
            Log.e("NotificationHelper", "Error sending action notification: " + e.getMessage());
        }
    }

    /**
     * Cancel a specific notification
     */
    public static void cancelNotification(@NonNull Context context, int notificationId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(notificationId);
    }

    /**
     * Cancel all notifications from this app
     */
    public static void cancelAllNotifications(@NonNull Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }

    /**
     * Get appropriate notification icon based on priority - USING SYSTEM ICONS
     */
    private static int getNotificationIcon(int priority) {
        switch (priority) {
            case NotificationCompat.PRIORITY_HIGH:
            case NotificationCompat.PRIORITY_MAX:
                return android.R.drawable.stat_notify_error; // Error/warning icon
            case NotificationCompat.PRIORITY_LOW:
            case NotificationCompat.PRIORITY_MIN:
                return android.R.drawable.ic_dialog_info; // Info icon
            default:
                return android.R.drawable.ic_dialog_email; // Default notification icon
        }
    }

    /**
     * Generate unique notification IDs
     */
    private static synchronized int getNextNotificationId() {
        return notificationIdCounter++;
    }

    /**
     * Check if notifications are enabled for the app
     */
    public static boolean areNotificationsEnabled(@NonNull Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        return notificationManager.areNotificationsEnabled();
    }

    /**
     * Helper method to convert priority string to integer
     */
    public static int getPriorityFromString(@NonNull String priorityString) {
        switch (priorityString.toLowerCase()) {
            case "low":
                return NotificationCompat.PRIORITY_LOW;
            case "high":
                return NotificationCompat.PRIORITY_HIGH;
            case "max":
                return NotificationCompat.PRIORITY_MAX;
            case "min":
                return NotificationCompat.PRIORITY_MIN;
            default:
                return NotificationCompat.PRIORITY_DEFAULT;
        }
    }

    /**
     * Get system icon resource ID by name for flexibility
     */
    public static int getSystemIcon(@NonNull String iconName) {
        switch (iconName.toLowerCase()) {
            case "play":
                return android.R.drawable.ic_media_play;
            case "pause":
                return android.R.drawable.ic_media_pause;
            case "stop":
                return android.R.drawable.ic_menu_close_clear_cancel;
            case "call":
                return android.R.drawable.ic_menu_call;
            case "settings":
                return android.R.drawable.ic_menu_preferences;
            case "info":
                return android.R.drawable.ic_dialog_info;
            case "alert":
                return android.R.drawable.ic_dialog_alert;
            case "email":
                return android.R.drawable.ic_dialog_email;
            default:
                return android.R.drawable.ic_dialog_info;
        }
    }
}
