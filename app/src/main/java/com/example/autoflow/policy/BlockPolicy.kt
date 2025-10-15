package com.example.autoflow.policy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autoflow.receiver.EmergencyUnblockReceiver

/**
 * BlockPolicy manages app blocking state and emergency unblock notifications
 */
object BlockPolicy {
    private const val TAG = "BlockPolicy"
    private const val PREFS = "block_policy_prefs"
    private const val KEY_ENABLED = "blocking_enabled"
    private const val KEY_PACKAGES = "blocked_packages_csv"
    private const val CHANNEL_ID = "emergency_unblock_channel"
    private const val NOTIFICATION_ID = 999

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setBlockingEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply()
        Log.d(TAG, "Blocking ${if (enabled) "enabled" else "disabled"}")
    }

    fun isBlockingEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ENABLED, false)

    fun setBlockedPackages(ctx: Context, pkgs: Set<String>) {
        prefs(ctx).edit().putString(KEY_PACKAGES, pkgs.joinToString(",")).apply()
        Log.d(TAG, "🚫 Updated blocked packages: ${pkgs.size} apps")

        // Show emergency notification when apps are blocked
        if (pkgs.isNotEmpty()) {
            showEmergencyUnblockNotification(ctx)
        }
    }

    fun getBlockedPackages(ctx: Context): Set<String> =
        prefs(ctx).getString(KEY_PACKAGES, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    fun clearBlockedPackages(ctx: Context) {
        prefs(ctx).edit().remove(KEY_PACKAGES).apply()
        Log.d(TAG, "🗑️ Cleared all blocked packages")

        // Cancel emergency notification when unblocked
        cancelEmergencyNotification(ctx)
    }

    fun addBlockedPackages(ctx: Context, packages: List<String>) {
        val current = getBlockedPackages(ctx).toMutableSet()
        current.addAll(packages)
        setBlockedPackages(ctx, current)
        Log.d(TAG, "➕ Added ${packages.size} apps to block list")
    }

    fun removeBlockedPackages(ctx: Context, packages: List<String>) {
        val current = getBlockedPackages(ctx).toMutableSet()
        current.removeAll(packages.toSet())
        setBlockedPackages(ctx, current)
        Log.d(TAG, "➖ Removed ${packages.size} apps from block list")
    }

    /**
     * Show emergency unblock notification with action button
     */
    fun showEmergencyUnblockNotification(context: Context) {
        createNotificationChannel(context)

        val unblockIntent = Intent(context, EmergencyUnblockReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            unblockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // ✅ Built-in Android icon
            .setContentTitle("🚨 Emergency Unblock Available") // ✅ Fixed - this is correct
            .setContentText("Tap to unblock all apps immediately")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_delete, // ✅ Built-in Android icon
                "UNBLOCK ALL",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d(TAG, "🚨 Emergency unblock notification shown")
    }

    /**
     * Cancel emergency unblock notification
     */
    fun cancelEmergencyNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "❌ Emergency notification cancelled")
    }

    /**
     * Create notification channel (required for Android O+)
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Unblock",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency unblock notification for blocked apps"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
