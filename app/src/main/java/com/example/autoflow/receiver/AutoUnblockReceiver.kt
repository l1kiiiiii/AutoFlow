package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ COMPLETELY FIXED AutoUnblockReceiver - Handles automatic app unblocking
 */
class AutoUnblockReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoUnblockReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔓 Auto-unblock alarm received")

        val appsToUnblock = intent.getStringExtra("apps_to_unblock") ?: ""

        if (appsToUnblock.isBlank()) {
            Log.w(TAG, "⚠️ No apps specified for unblocking")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val actionExecutor = ActionExecutor.getInstance()

                // ✅ FIXED: Use proper unblockApps method with coroutine scope
                val success = unblockApps(context, appsToUnblock, CoroutineScope(Dispatchers.IO))

                if (success) {
                    Log.d(TAG, "✅ Successfully unblocked apps: $appsToUnblock")

                    // Send notification about unblocking
                    sendNotification(
                        context,
                        "🔓 Apps Unblocked",
                        "Scheduled app blocking has ended.",
                        "Normal"
                    )
                } else {
                    Log.e(TAG, "❌ Failed to unblock apps: $appsToUnblock")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in auto-unblock receiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * ✅ FIXED: Unblock apps implementation
     */
    private suspend fun unblockApps(
        context: Context,
        packageNames: String,
        scope: CoroutineScope
    ): Boolean {
        return try {
            val actionExecutor = ActionExecutor.getInstance()

            // Create an unblock action
            val unblockAction = com.example.autoflow.model.Action(
                type = com.example.autoflow.util.Constants.ACTION_UNBLOCK_APPS,
                value = packageNames
            )

            // Execute the unblock action
            actionExecutor.executeAction(context, unblockAction, scope)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unblocking apps", e)
            false
        }
    }

    /**
     * ✅ FIXED: Send notification implementation
     */
    private suspend fun sendNotification(
        context: Context,
        title: String,
        message: String,
        priority: String
    ): Boolean {
        return try {
            val actionExecutor = ActionExecutor.getInstance()

            // Create a notification action
            val notificationAction = com.example.autoflow.model.Action(
                type = com.example.autoflow.util.Constants.ACTION_SEND_NOTIFICATION,
                title = title,
                message = message,
                priority = priority
            )

            // Execute the notification action
            actionExecutor.executeAction(context, notificationAction, CoroutineScope(Dispatchers.Main))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification", e)
            false
        }
    }
}
