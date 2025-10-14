package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.receiver.AlarmReceiver
import com.example.autoflow.util.Constants
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📍 Geofence event received")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent?.errorCode ?: -1)
            Log.e(TAG, "❌ Geofence error: $errorMessage")
            return
        }

        // Determine the transition type from the event
        val transitionType = when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "enter"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "exit"
            else -> null // We don't handle DWELL or other types
        }

        if (transitionType == null) {
            Log.w(TAG, "⚠️ Unhandled geofence transition: ${geofencingEvent.geofenceTransition}")
            return
        }
        val pendingResult = goAsync()

        // Process each geofence that triggered the event
        geofencingEvent.triggeringGeofences?.forEach { geofence ->
            val workflowId = geofence.requestId.substringAfter("workflow_").toLongOrNull()
            if (workflowId != null && workflowId != 0L) {
                Log.d(TAG, "✅ Geofence transition '$transitionType' for workflow ID: $workflowId")
                handleGeofenceTransition(context, workflowId, transitionType, pendingResult)
            } else {
                Log.e(TAG, "❌ Invalid geofence ID format: ${geofence.requestId}")
            }
        }
    }

    /**
     * Handles the geofence transition by checking the workflow's trigger conditions
     * and executing the action through AlarmReceiver.
     */
    private fun handleGeofenceTransition(
        context: Context,
        workflowId: Long,
        transitionType: String,
        pendingResult: PendingResult
    ) {
        // Use a coroutine to perform a quick database check off the main thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).workflowDao()
                // FIXED: Use getByIdSync instead of getWorkflowById
                val workflow = dao.getByIdSync(workflowId)

                if (workflow == null) {
                    Log.e(TAG, "❌ Workflow $workflowId not found in database")
                    return@launch
                }
                if (!workflow.isEnabled) {
                    Log.w(TAG, "⚠️ Workflow $workflowId is disabled, skipping.")
                    return@launch
                }

                // Find the specific location trigger within the workflow's trigger list
                val locationTrigger = workflow.toTriggers()
                    .filterIsInstance<com.example.autoflow.model.Trigger.LocationTrigger>()
                    .firstOrNull()

                if (locationTrigger == null) {
                    Log.e(TAG, "❌ No location trigger found for workflow $workflowId")
                    return@launch
                }

                // Check if the workflow should execute for this specific transition
                val shouldExecute = when (locationTrigger.triggerOn.lowercase()) {
                    "both" -> true
                    "enter" -> transitionType == "enter"
                    "exit" -> transitionType == "exit"
                    else -> false
                }

                if (shouldExecute) {
                    Log.d(TAG, "✅ Condition met. Executing action for workflow: ${workflow.workflowName}")

                    // FIXED: Execute action using AlarmReceiver (your existing architecture)
                    val actionArray = JSONArray(workflow.actionDetails)
                    if (actionArray.length() > 0) {
                        val actionJson = actionArray.getJSONObject(0)
                        val actionType = actionJson.optString("type", Constants.ACTION_SEND_NOTIFICATION)

                        val actionIntent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra(Constants.KEY_WORKFLOW_ID, workflowId)
                            putExtra("action_type", actionType)

                            when (actionType) {
                                Constants.ACTION_SEND_NOTIFICATION -> {
                                    putExtra("notification_title", actionJson.optString("title", "AutoFlow"))
                                    putExtra("notification_message", actionJson.optString("message", "Location trigger activated"))
                                }
                                Constants.ACTION_SET_SOUND_MODE -> {
                                    putExtra("sound_mode", actionJson.optString("value", "Silent"))
                                }
                                Constants.ACTION_TOGGLE_WIFI -> {
                                    val wifiState = actionJson.optString("value", "OFF").contains("ON", ignoreCase = true)
                                    putExtra("wifi_state", wifiState.toString())
                                }
                                Constants.ACTION_TOGGLE_BLUETOOTH -> {
                                    val btState = actionJson.optString("value", "OFF").contains("ON", ignoreCase = true)
                                    putExtra("bluetooth_state", btState.toString())
                                }
                            }
                        }

                        // Send broadcast to AlarmReceiver for execution
                        context.sendBroadcast(actionIntent)
                        Log.d(TAG, "📤 Action broadcast sent for workflow $workflowId")
                    }

                } else {
                    Log.d(TAG, "⏭️ Skipping execution: workflow expects '${locationTrigger.triggerOn}' but transition was '$transitionType'")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling geofence transition for workflow $workflowId", e)
            }
        }
    }
}
