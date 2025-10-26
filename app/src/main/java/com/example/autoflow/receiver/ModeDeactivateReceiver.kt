package com.example.autoflow.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.integrations.SoundModeManager
import com.example.autoflow.util.Constants

class ModeDeactivateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ModeDeactivateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val modeName = intent.getStringExtra("mode_name") ?: "Mode"

        Log.d(TAG, "Deactivating $modeName")

        // Disable DND mode and restore normal interruptions
        val soundModeManager = SoundModeManager(context)
        soundModeManager.disableDNDMode()

        // Cancel the persistent notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(Constants.NOTIFICATION_ID_MODE_ACTIVE)

        Log.d(TAG, "$modeName deactivated")
    }
}
