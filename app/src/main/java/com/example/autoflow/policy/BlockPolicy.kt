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
import com.example.autoflow.service.AppBlockService

/**
 * BlockPolicy manages app blocking state and emergency unblock notifications
 */
object  BlockPolicy {
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
    // ✅ ADD: Enhanced emergency unblock with context awareness
    fun emergencyUnblockWithContext(context: Context, reason: String = "Emergency") {
        val blockedApps = getBlockedPackages(context)
        val blockedCount = blockedApps.size

        if (blockedCount == 0) {
            Log.d(TAG, "⚠️ No apps currently blocked")
            return
        }

        Log.d(TAG, "🚨 EMERGENCY UNBLOCK: $reason")
        Log.d(TAG, "🚨 Unblocking $blockedCount apps immediately")

        // Clear everything immediately
        clearBlockedPackages(context)
        setBlockingEnabled(context, false)

        // Cancel emergency notification
        cancelEmergencyNotification(context)

        // Stop AppBlockService
        val serviceIntent = Intent(context, AppBlockService::class.java)
        context.stopService(serviceIntent)

        // Show success notification
        showEmergencyUnblockSuccess(context, blockedCount, reason)
    }

    // ✅ ADD: Success notification for emergency unblock
    private fun showEmergencyUnblockSuccess(context: Context, unlockedCount: Int, reason: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("✅ Emergency Unblock Complete")
            .setContentText("$unlockedCount apps unblocked. Full access restored. Reason: $reason")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)

        Log.d(TAG, "✅ Emergency unblock success notification shown")
    }

    // ✅ IMPROVED: Smart blocking with app categorization
    fun setSmartBlockedPackages(context: Context, categories: List<String>) {
        val allApps = when {
            categories.contains("social") -> getSocialApps()
            categories.contains("games") -> getGameApps()
            categories.contains("entertainment") -> getEntertainmentApps()
            categories.contains("shopping") -> getShoppingApps()
            categories.contains("all_distracting") -> getAllDistractingApps()
            else -> emptySet()
        }

        setBlockedPackages(context, allApps)
        Log.d(TAG, "🎯 Smart blocking activated for categories: $categories")
    }

    // ✅ ADD: Predefined app categories
    private fun getSocialApps() = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.snapchat.android",
        "com.tiktok.android",
        "com.whatsapp"
    )

    private fun getGameApps() = setOf(
        "com.supercell.clashofclans",
        "com.king.candycrushsaga",
        "com.mojang.minecraftpe",
        "com.activision.callofduty.shooter"
    )

    private fun getEntertainmentApps() = setOf(
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.spotify.music",
        "com.google.android.youtube"
    )

    private fun getShoppingApps() = setOf(
        "com.amazon.mshop.android.shopping",
        "in.amazon.mShop.android.shopping",
        "com.flipkart.android",
        "com.myntra.android"
    )

    private fun getAllDistractingApps() =
        getSocialApps() + getGameApps() + getEntertainmentApps() + getShoppingApps()


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
