package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.*

class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📍 Geofence event received")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e(TAG, "❌ Geofence error: ${geofencingEvent?.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        val transitionString = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> return
        }

        Log.d(TAG, "📍 Geofence transition: $transitionString")

        triggeringGeofences.forEach { geofence ->
            Log.d(TAG, "📍 Triggered geofence: ${geofence.requestId}")
            checkLocationTriggers(context, transitionString, geofence.requestId)
        }
    }

    private fun checkLocationTriggers(context: Context, transition: String, geofenceId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val workflows = database.workflowDao().getActiveWorkflows()

                workflows.forEach { workflow ->
                    val triggers = workflow.toTriggers()

                    triggers.forEach { trigger ->
                        if (trigger.type == "LOCATION" && matchesLocationTrigger(trigger.value, transition, geofenceId)) {
                            Log.d(TAG, "✅ Location trigger matched for workflow: ${workflow.workflowName}")

                            withContext(Dispatchers.Main) {
                                ActionExecutor.executeWorkflow(context, workflow)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking location triggers", e)
            }
        }
    }

    /**
     * ✅ Check if trigger value matches location transition
     */
    private fun matchesLocationTrigger(triggerValue: String, transition: String, geofenceId: String): Boolean {
        return try {
            val json = JSONObject(triggerValue)
            val locationName = json.optString("locationName", "")
            val triggerOn = json.optString("triggerOn", "both")
            val triggerOnEntry = json.optBoolean("triggerOnEntry", true)
            val triggerOnExit = json.optBoolean("triggerOnExit", true)

            // Check if geofence ID matches location name
            if (geofenceId != locationName) {
                return false
            }

            // Check transition type
            return when (transition) {
                "ENTER" -> triggerOnEntry || triggerOn == "both" || triggerOn == "enter"
                "EXIT" -> triggerOnExit || triggerOn == "both" || triggerOn == "exit"
                else -> false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing location trigger value: $triggerValue", e)
            false
        }
    }
}
