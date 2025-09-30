package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.util.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e("AlarmReceiver", "❌ Context or Intent is null")
            return
        }

        val workflowId = intent.getLongExtra("workflow_id", -1)
        val title = intent.getStringExtra("notification_title") ?: "AutoFlow Alert"
        val message = intent.getStringExtra("notification_message") ?: "Time trigger activated"

        Log.d("AlarmReceiver", "🔔 Alarm received for workflow $workflowId")
        Log.d("AlarmReceiver", "📱 Sending notification: $title")

        // Send the notification
        NotificationHelper.sendNotification(context, title, message)

        Log.d("AlarmReceiver", "✅ Notification sent successfully")
    }
}
