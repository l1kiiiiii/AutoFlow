package com.example.autoflow.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.autoflow.R
import com.example.autoflow.integrations.SoundModeManager
import com.example.autoflow.util.Constants

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "autoflow_notifications"
        private const val CHANNEL_NAME = "AutoFlow Alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val workflowId = intent.getLongExtra("workflow_id", -1L)
        val actionType = intent.getStringExtra("action_type") ?: Constants.ACTION_SEND_NOTIFICATION

        Log.d(TAG, "⏰ Alarm triggered for workflow: $workflowId")
        Log.d(TAG, "   Action type: $actionType")

        try {
            when (actionType) {
                Constants.ACTION_SEND_NOTIFICATION -> {
                    handleNotification(context, intent)
                }
                Constants.ACTION_SET_SOUND_MODE -> {
                    handleSoundMode(context, intent)
                }
                Constants.ACTION_TOGGLE_WIFI -> {
                    handleWiFiToggle(context, intent)
                }
                Constants.ACTION_RUN_SCRIPT -> {
                    handleScript(context, intent)
                }
                else -> {
                    Log.w(TAG, "⚠️ Unknown action type: $actionType")
                    Toast.makeText(context, "Unknown action: $actionType", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing action: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle notification action
     */
    private fun handleNotification(context: Context, intent: Intent) {
        val title = intent.getStringExtra("notification_title") ?: "AutoFlow"
        val message = intent.getStringExtra("notification_message") ?: "Trigger activated"

        Log.d(TAG, "📬 Sending notification: $title")

        // Create notification channel (required for Android 8.0+)
        createNotificationChannel(context)

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this icon exists!
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Show notification
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d(TAG, "✅ Notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Notification permission denied", e)
            Toast.makeText(context, "Notification permission required", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Handle sound mode action
     */
    private fun handleSoundMode(context: Context, intent: Intent) {
        val soundMode = intent.getStringExtra("sound_mode") ?: "Silent"

        Log.d(TAG, "🔊 Setting sound mode to: $soundMode")

        val soundModeManager = SoundModeManager(context)
        val success = soundModeManager.setSoundMode(soundMode)

        if (success) {
            Log.d(TAG, "✅ Sound mode changed to: $soundMode")
            Toast.makeText(context, "Sound mode: $soundMode", Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "❌ Failed to change sound mode")

            // If DND failed, check permission
            if (soundMode == "DND" && !soundModeManager.hasDNDPermission()) {
                Toast.makeText(context, "DND permission required", Toast.LENGTH_LONG).show()
                soundModeManager.openDNDSettings()
            } else {
                Toast.makeText(context, "Failed to set sound mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handle WiFi toggle action
     */
    private fun handleWiFiToggle(context: Context, intent: Intent) {
        val wifiStateStr = intent.getStringExtra("wifi_state") ?: "false"
        val wifiState = wifiStateStr.toBoolean()

        Log.d(TAG, "📶 WiFi toggle: ${if (wifiState) "ON" else "OFF"}")

        // Note: Direct WiFi control requires system permissions (Android 10+)
        // For Android 10+, open WiFi settings instead
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.w(TAG, "⚠️ WiFi control not available on Android 10+, opening settings")
            val wifiIntent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(wifiIntent)
            Toast.makeText(context, "Please toggle WiFi manually", Toast.LENGTH_LONG).show()
        } else {
            // Legacy WiFi control (Android 9 and below)
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
                        as android.net.wifi.WifiManager

                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = wifiState

                Log.d(TAG, "✅ WiFi ${if (wifiState) "enabled" else "disabled"}")
                Toast.makeText(
                    context,
                    "WiFi ${if (wifiState) "ON" else "OFF"}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "❌ WiFi toggle failed: ${e.message}", e)
                Toast.makeText(context, "WiFi control failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handle script execution action
     */
    private fun handleScript(context: Context, intent: Intent) {
        val scriptText = intent.getStringExtra("script_text") ?: ""

        Log.d(TAG, "📜 Executing script: ${scriptText.take(50)}...")

        if (scriptText.isBlank()) {
            Log.w(TAG, "⚠️ Script is empty")
            Toast.makeText(context, "Script is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // For security and compatibility, we'll just show the script
        // Actual execution would require root or shell permissions
        Toast.makeText(context, "Script execution not yet implemented", Toast.LENGTH_LONG).show()

        // TODO: Implement safe script execution
        // Options:
        // 1. Use Runtime.getRuntime().exec() with limited commands
        // 2. Use ProcessBuilder with whitelist
        // 3. Integrate with Termux API if installed

        Log.w(TAG, "⚠️ Script execution not implemented for security reasons")
    }

    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from AutoFlow workflows"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
