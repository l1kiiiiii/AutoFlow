package com.example.autoflow.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autoflow.blocker.BlockActivity
import com.example.autoflow.policy.BlockPolicy
import android.content.pm.ServiceInfo
import com.example.autoflow.receiver.EmergencyUnblockReceiver

/**
 * Foreground service that monitors foreground apps and blocks them if in blocked list
 */
class AppBlockService : Service() {

    companion object {
        private const val TAG = "AppBlockService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "app_block_service"
        private const val CHECK_INTERVAL = 500L // Check every 500ms
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager
    private var isRunning = false

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "📱 AppBlockService created")

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Create foreground notification
        createNotificationChannel()
        val notification = createNotification()
        // startForeground(NOTIFICATION_ID, notification)//-isssue

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        isRunning = true
        startMonitoring()
    }

    private fun startMonitoring() {
        Log.d(TAG, "👀 Started monitoring foreground apps")

        handler.post(object : Runnable {
            override fun run() {
                if (isRunning && BlockPolicy.isBlockingEnabled(this@AppBlockService)) {
                    checkCurrentApp()
                    handler.postDelayed(this, CHECK_INTERVAL)
                } else {
                    Log.d(TAG, "⏸️ Monitoring paused (blocking disabled)")
                    handler.postDelayed(this, 2000) // Check again in 2 seconds
                }
            }
        })
    }

    // ✅ IMPROVED: More efficient app checking with throttling
    private val lastCheckTime = mutableMapOf<String, Long>()
    private val CHECK_THROTTLE_MS = 2000L // Don't check same app for 2 seconds

    private fun checkCurrentApp() {
        try {
            val currentTime = System.currentTimeMillis()
            val usageEvents = usageStatsManager.queryEvents(currentTime - 2000, currentTime)
            val event = UsageEvents.Event()

            var lastApp: String? = null
            var mostRecentTime = 0L

            // ✅ IMPROVED: Find the most recent app (more accurate)
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND && event.timeStamp > mostRecentTime) {
                    lastApp = event.packageName
                    mostRecentTime = event.timeStamp
                }
            }

            // Check if current app is blocked (ignore our own package)
            if (lastApp != null && lastApp != packageName) {
                // ✅ THROTTLING: Don't spam block screens for same app
                val lastCheck = lastCheckTime[lastApp] ?: 0
                if (currentTime - lastCheck < CHECK_THROTTLE_MS) {
                    return
                }

                val blockedApps = BlockPolicy.getBlockedPackages(this)

                if (blockedApps.contains(lastApp)) {
                    Log.d(TAG, "🚫 BLOCKING APP: $lastApp")
                    lastCheckTime[lastApp] = currentTime
                    showBlockScreen(lastApp)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking current app", e)
        }
    }

    // ✅ IMPROVED: Better notification with real-time count
    private fun createNotification(): Notification {
        val blockedApps = BlockPolicy.getBlockedPackages(this)
        val blockedCount = blockedApps.size
        val isActive = BlockPolicy.isBlockingEnabled(this)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isActive) "🚫 App Blocking Active" else "⏸️ App Blocking Paused")
            .setContentText("${blockedCount} ${if (blockedCount == 1) "app" else "apps"} blocked • Tap for emergency unblock")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Emergency Unblock",
                createEmergencyUnblockIntent()
            )
            .build()
    }

    // ✅ NEW: Emergency unblock from service notification
    private fun createEmergencyUnblockIntent(): PendingIntent {
        val intent = Intent(this, EmergencyUnblockReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    private fun showBlockScreen(packageName: String) {
        val intent = Intent(this, BlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("package_name", packageName)
            putExtra("app_name", getAppName(packageName))
        }
        startActivity(intent)
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Block Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps app blocking active in background"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🔄 Service restarted")
        return START_STICKY // Restart service if killed by system
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "❌ AppBlockService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
