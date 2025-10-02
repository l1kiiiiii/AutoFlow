package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.policy.BlockPolicy
import com.example.autoflow.util.NotificationHelper
import com.example.autoflow.util.RingerModeHelper
import com.example.autoflow.util.Constants
import org.json.JSONObject

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // First, validate the context and intent.
        if (context == null || intent == null) {
            Log.e("AlarmReceiver", "Context or Intent is null, cannot proceed.")
            return
        }

        // Use a 'when' statement to handle different actions
        when (intent.action) {
            Constants.ACTION_SET_SOUND_MODE -> {
                val mode = intent.getStringExtra("sound_mode") ?: Constants.SOUND_MODE_RING
                Log.d("AlarmReceiver", " Action received: Set sound mode to $mode")
                RingerModeHelper.setSoundMode(context, mode)
            }

            Constants.ACTION_BLOCK_APPS -> {
                val blockingData = intent.getStringExtra("blocking_data")
                if (blockingData == null) {
                    Log.e("AlarmReceiver", "Blocking data is null for ACTION_BLOCK_APPS")
                    return
                }

                Log.d("AlarmReceiver", " Setting up app blocking: $blockingData")
                try {
                    val json = JSONObject(blockingData)
                    val packages = json.getJSONArray("packages")
                    val packageList = mutableListOf<String>()
                    for (i in 0 until packages.length()) {
                        packageList.add(packages.getString(i))
                    }

                    // Store the list of packages to be blocked
                    BlockPolicy.setBlockedPackages(context, packageList)
                    Log.d("AlarmReceiver", " App blocking configured for ${packageList.size} apps")

                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error parsing app blocking data", e)
                }
            }
        }

        // General notification logic for alarms that carry a workflow ID.
        // This will run if the intent has a valid workflow ID, regardless of the action.
        if (intent.hasExtra("workflow_id")) {
            val workflowId = intent.getLongExtra("workflow_id", -1)
            if (workflowId != -1L) {
                val title = intent.getStringExtra("notification_title") ?: "AutoFlow Alert"
                val message = intent.getStringExtra("notification_message") ?: "Time trigger activated"

                Log.d("AlarmReceiver", " Alarm received for workflow $workflowId")
                Log.d("AlarmReceiver", " Sending notification: $title")

                NotificationHelper.sendNotification(context, title, message)

                Log.d("AlarmReceiver", " Notification sent successfully for workflow $workflowId")
            }
        }
    }
}
