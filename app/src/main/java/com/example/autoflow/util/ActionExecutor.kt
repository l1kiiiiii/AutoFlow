package com.example.autoflow.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toActions
import com.example.autoflow.model.Action

object ActionExecutor {
    private const val TAG = "ActionExecutor"

    private const val CHANNEL_ID_URGENT = "autoflow_urgent"
    private const val CHANNEL_ID_HIGH = "autoflow_high"
    private const val CHANNEL_ID_NORMAL = "autoflow_normal"
    private const val CHANNEL_ID_LOW = "autoflow_low"

    fun executeAction(context: Context, action: Action): Boolean {
        return try {
            Log.d(TAG, "Executing action: ${action.type}")

            when (action.type) {
                Constants.ACTION_SEND_NOTIFICATION -> {
                    sendNotification(
                        context,
                        action.title ?: "AutoFlow",
                        action.message ?: "Automation triggered",
                        action.priority ?: "Normal"
                    )
                }
                Constants.ACTION_TOGGLE_WIFI -> {
                    toggleWifi(context, action.value ?: "Toggle")
                }
                Constants.ACTION_TOGGLE_BLUETOOTH -> {
                    toggleBluetooth(context, action.value ?: "Toggle")
                }
                Constants.ACTION_SET_SOUND_MODE -> {
                    setSoundMode(context, action.value ?: "Normal")
                }
                Constants.ACTION_RUN_SCRIPT -> {
                    runScript(context, action.value ?: "")
                }
                else -> {
                    Log.w(TAG, "Unknown action type: ${action.type}")
                    false
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action: ${action.type}", e)
            false
        }
    }

    private fun sendNotification(
        context: Context,
        title: String,
        message: String,
        priority: String
    ): Boolean {
        return try {
            createNotificationChannels(context)

            val channelId = when (priority) {
                "Urgent", "Max" -> CHANNEL_ID_URGENT
                "High" -> CHANNEL_ID_HIGH
                "Low" -> CHANNEL_ID_LOW
                else -> CHANNEL_ID_NORMAL
            }

            val notificationPriority = when (priority) {
                "Urgent", "Max" -> NotificationCompat.PRIORITY_MAX
                "High" -> NotificationCompat.PRIORITY_HIGH
                "Low" -> NotificationCompat.PRIORITY_LOW
                else -> NotificationCompat.PRIORITY_DEFAULT
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "Notification permission not granted")
                    return false
                }
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(notificationPriority)
                .setAutoCancel(true)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)

            Log.d(TAG, "Notification sent: $title")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            false
        }
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_URGENT,
                    "Urgent Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical automation alerts"
                    enableVibration(true)
                    enableLights(true)
                },
                NotificationChannel(
                    CHANNEL_ID_HIGH,
                    "High Priority",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_ID_NORMAL,
                    "Normal",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_ID_LOW,
                    "Low Priority",
                    NotificationManager.IMPORTANCE_LOW
                )
            )

            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    private fun toggleWifi(context: Context, action: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(panelIntent)
                return true
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            @Suppress("DEPRECATION")
            val enable = when (action) {
                "Turn ON" -> true
                "Turn OFF" -> false
                else -> !wifiManager.isWifiEnabled
            }

            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enable
            Log.d(TAG, "WiFi toggled: $enable")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling WiFi", e)
            false
        }
    }

    private fun toggleBluetooth(context: Context, action: String): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            }

            val enable = when (action) {
                "Turn ON" -> true
                "Turn OFF" -> false
                else -> !bluetoothAdapter.isEnabled
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                @Suppress("DEPRECATION")
                if (enable) bluetoothAdapter.enable() else bluetoothAdapter.disable()
            }

            Log.d(TAG, "Bluetooth action: $action")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling Bluetooth", e)
            false
        }
    }

    private fun setSoundMode(context: Context, mode: String): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            when (mode) {
                "Normal", Constants.SOUND_MODE_RING -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
                "Silent", Constants.SOUND_MODE_SILENT -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                "Vibrate", Constants.SOUND_MODE_VIBRATE -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                "Do Not Disturb" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!notificationManager.isNotificationPolicyAccessGranted) {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            return false
                        }
                        notificationManager.setInterruptionFilter(
                            NotificationManager.INTERRUPTION_FILTER_PRIORITY
                        )
                    }
                }
            }

            Log.d(TAG, "Sound mode set to: $mode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sound mode", e)
            false
        }
    }

    private fun runScript(context: Context, script: String): Boolean {
        return try {
            // Basic script execution - implement according to your needs
            Log.d(TAG, "Script execution: $script")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error running script", e)
            false
        }
    }
    /**
     * Execute multiple actions for a workflow
     */
    fun executeWorkflow(context: Context, workflow: WorkflowEntity): Boolean {
        val actions = workflow.toActions()

        if (actions.isEmpty()) {
            Log.w(TAG, "⚠️ No actions to execute for workflow ${workflow.id}")
            return false
        }

        Log.d(TAG, "🚀 Executing ${actions.size} actions for workflow: ${workflow.workflowName}")

        var allSuccessful = true
        actions.forEachIndexed { index, action ->
            val success = executeAction(context, action)
            Log.d(TAG, "Action ${index + 1}/${actions.size}: ${if (success) "✅" else "❌"}")
            if (!success) allSuccessful = false
        }

        return allSuccessful
    }

}
