package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.receiver.AlarmReceiver
import com.example.autoflow.util.Constants
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📍 Geofence event received")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "❌ Geofencing event is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "❌ Geofence error: $errorMessage (code: ${geofencingEvent.errorCode})")
            return
        }

        // Get the transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Get triggering geofences
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.e(TAG, "❌ No triggering geofences")
            return
        }

        // Handle each geofence
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                triggeringGeofences.forEach { geofence ->
                    Log.d(TAG, "✅ ENTERED geofence: ${geofence.requestId}")
                    handleGeofenceTransition(context, geofence.requestId, "enter")
                }
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                triggeringGeofences.forEach { geofence ->
                    Log.d(TAG, "✅ EXITED geofence: ${geofence.requestId}")
                    handleGeofenceTransition(context, geofence.requestId, "exit")
                }
            }

            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                triggeringGeofences.forEach { geofence ->
                    Log.d(TAG, "✅ DWELLING in geofence: ${geofence.requestId}")
                }
            }

            else -> {
                Log.w(TAG, "⚠️ Unknown geofence transition: $geofenceTransition")
            }
        }
    }

    /**
     * Handle geofence transition by executing workflow action
     */
    private fun handleGeofenceTransition(context: Context, geofenceId: String, transitionType: String) {
        // Extract workflow ID from geofence request ID
        // Format: "workflow_123"
        val workflowId = geofenceId.substringAfter("workflow_").toLongOrNull()

        if (workflowId == null) {
            Log.e(TAG, "❌ Invalid geofence ID format: $geofenceId")
            return
        }

        Log.d(TAG, "🎯 Processing workflow ID: $workflowId (transition: $transitionType)")

        // Query database asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ FIX: Use getDatabase() instead of getInstance()
                val database = AppDatabase.getDatabase(context)

                // ✅ FIX: Use getByIdSync() instead of getWorkflowById()
                val workflow = database.workflowDao().getByIdSync(workflowId)

                if (workflow == null) {
                    Log.e(TAG, "❌ Workflow $workflowId not found in database")
                    return@launch
                }

                // ✅ FIX: Use isEnabled() method instead of isEnabled property
                if (!workflow.isEnabled()) {
                    Log.w(TAG, "⚠️ Workflow $workflowId is disabled, skipping")
                    return@launch
                }

                // ✅ FIX: Use getTriggerDetails() method
                val triggerDetails = workflow.getTriggerDetails()
                val triggerJson = JSONObject(triggerDetails)
                val triggerOn = triggerJson.optString("triggerOn", "enter")

                // Only execute if transition type matches
                if (triggerOn != transitionType) {
                    Log.d(TAG, "⏭️ Skipping: workflow expects '$triggerOn' but got '$transitionType'")
                    return@launch
                }

                // ✅ FIX: Use getWorkflowName() method
                Log.d(TAG, "✅ Executing action for workflow: ${workflow.getWorkflowName()}")

                // Execute the action through AlarmReceiver
                val actionIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("workflow_id", workflowId)

                    // ✅ FIX: Use getActionDetails() method
                    val actionDetails = workflow.getActionDetails()
                    if (actionDetails.isNotEmpty()) {
                        val actionJson = JSONObject(actionDetails)
                        val actionType = actionJson.optString("type", Constants.ACTION_SEND_NOTIFICATION)
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
                }

                // Send broadcast to AlarmReceiver
                context.sendBroadcast(actionIntent)
                Log.d(TAG, "📤 Action broadcast sent for workflow $workflowId")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling geofence transition: ${e.message}", e)
            }
        }
    }
}
