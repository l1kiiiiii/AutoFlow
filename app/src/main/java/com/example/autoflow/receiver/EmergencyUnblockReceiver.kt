package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.autoflow.policy.BlockPolicy

class EmergencyUnblockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BlockPolicy.clearBlockedPackages(context)
        Toast.makeText(context, "🚨 All apps unblocked!", Toast.LENGTH_LONG).show()

        // Cancel the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(999)
    }
}
