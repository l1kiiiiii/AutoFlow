package com.example.autoflow.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.autoflow.receiver.AlarmReceiver

object AlarmScheduler {

    /**
     * Schedule an alarm for any action type
     * @param context Context
     * @param workflowId Unique workflow ID
     * @param triggerTimeMillis When to trigger (in milliseconds)
     * @param actionType Type of action (SEND_NOTIFICATION, SET_SOUND_MODE, TOGGLE_WIFI, etc.)
     * @param actionData Map of action-specific data
     */
    fun scheduleAlarm(
        context: Context,
        workflowId: Long,
        triggerTimeMillis: Long,
        actionType: String,
        actionData: Map<String, String> = emptyMap()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("workflow_id", workflowId)
            putExtra("action_type", actionType)

            // Add all action data as extras
            actionData.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            workflowId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "✅ Exact alarm scheduled for workflow $workflowId ($actionType)")
                } else {
                    Log.e("AlarmScheduler", "❌ Cannot schedule exact alarms - permission not granted")
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    Log.d("AlarmScheduler", "⚠️ Fallback: Inexact alarm scheduled")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "✅ Alarm scheduled for workflow $workflowId ($actionType)")
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "❌ Error scheduling alarm: ${e.message}", e)
        }
    }

    /**
     * BACKWARD COMPATIBLE: Schedule notification (existing method)
     * This keeps your existing code working without changes!
     */
    fun scheduleNotification(
        context: Context,
        workflowId: Long,
        triggerTimeMillis: Long,
        notificationTitle: String,
        notificationMessage: String
    ) {
        val actionData = mapOf(
            "notification_title" to notificationTitle,
            "notification_message" to notificationMessage
        )

        scheduleAlarm(
            context,
            workflowId,
            triggerTimeMillis,
            "SEND_NOTIFICATION",
            actionData
        )
    }

    /**
     * Schedule sound mode change
     */
    fun scheduleSoundMode(
        context: Context,
        workflowId: Long,
        triggerTimeMillis: Long,
        soundMode: String  // "Normal", "Silent", "Vibrate", "DND"
    ) {
        val actionData = mapOf(
            "sound_mode" to soundMode
        )

        scheduleAlarm(
            context,
            workflowId,
            triggerTimeMillis,
            "SET_SOUND_MODE",
            actionData
        )
    }

    /**
     * Schedule WiFi toggle
     */
    fun scheduleWiFiToggle(
        context: Context,
        workflowId: Long,
        triggerTimeMillis: Long,
        wifiState: Boolean  // true = turn on, false = turn off
    ) {
        val actionData = mapOf(
            "wifi_state" to wifiState.toString()
        )

        scheduleAlarm(
            context,
            workflowId,
            triggerTimeMillis,
            "TOGGLE_WIFI",
            actionData
        )
    }

    /**
     * Schedule script execution
     */
    fun scheduleScript(
        context: Context,
        workflowId: Long,
        triggerTimeMillis: Long,
        scriptText: String
    ) {
        val actionData = mapOf(
            "script_text" to scriptText
        )

        scheduleAlarm(
            context,
            workflowId,
            triggerTimeMillis,
            "RUN_SCRIPT",
            actionData
        )
    }

    /**
     * Cancel alarm for a workflow
     */
    fun cancelAlarm(context: Context, workflowId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            workflowId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d("AlarmScheduler", "✅ Alarm cancelled for workflow $workflowId")
    }
}
