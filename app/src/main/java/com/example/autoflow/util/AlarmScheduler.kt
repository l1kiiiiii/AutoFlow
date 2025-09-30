package com.example.autoflow.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.autoflow.receiver.AlarmReceiver

object AlarmScheduler {

    fun scheduleNotification(
        context: Context,
        workflowId: Long,
        triggerTimeMillis: Long,
        notificationTitle: String,
        notificationMessage: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("workflow_id", workflowId)
            putExtra("notification_title", notificationTitle)
            putExtra("notification_message", notificationMessage)
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
                    Log.d("AlarmScheduler", "✅ Exact alarm scheduled for workflow $workflowId")
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
                Log.d("AlarmScheduler", "✅ Alarm scheduled for workflow $workflowId")
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "❌ Error scheduling alarm: ${e.message}")
        }
    }

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
        Log.d("AlarmScheduler", "Alarm cancelled for workflow $workflowId")
    }
}
