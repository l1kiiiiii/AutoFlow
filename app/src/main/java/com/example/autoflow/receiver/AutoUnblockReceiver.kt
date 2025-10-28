
package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.util.ActionExecutor

class AutoUnblockReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoUnblockReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appsToUnblock = intent.getStringExtra("apps_to_unblock")

        if (!appsToUnblock.isNullOrEmpty()) {
            Log.d(TAG, "⏰ Auto-unblock triggered for apps: $appsToUnblock")

            val success = ActionExecutor.unblockApps(context, appsToUnblock)

            if (success) {
                ActionExecutor.sendNotification(
                    context,
                    "⏰ Auto-Unblock Complete",
                    "Time-based blocking period ended",
                    "Normal"
                )
            }
        }
    }
}
