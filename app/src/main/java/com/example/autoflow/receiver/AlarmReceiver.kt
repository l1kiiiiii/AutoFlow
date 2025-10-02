package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.util.NotificationHelper
import com.example.autoflow.util.RingerModeHelper
import com.example.autoflow.util.Constants

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e("AlarmReceiver", " Context or Intent is null")
            return
        }

        // 2. Get the action string directly from the intent object.
        val intentAction = intent.action

        // 3. Check if the intent's action matches your constant
        if (intentAction == Constants.ACTION_SET_SOUND_MODE) {
            // 4. Get the sound mode from the intent's extras.
            val mode = intent.getStringExtra("sound_mode") ?: Constants.SOUND_MODE_RING
            Log.d("AlarmReceiver", "🔊 Action received: Set sound mode to $mode")
            RingerModeHelper.setSoundMode(context, mode)
        }

        //notification logic
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
