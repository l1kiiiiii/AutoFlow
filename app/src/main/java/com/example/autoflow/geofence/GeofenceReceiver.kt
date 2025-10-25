package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.json.JSONObject
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.util.TriggerParser
import com.example.autoflow.util.Constants


class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    // In GeofenceReceiver.kt around lines 65-70
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        triggeringGeofences?.forEach { geofence ->
            val geofenceId = geofence.requestId
            Log.d(TAG, "Geofence triggered: $geofenceId")

            // FIXED: Extract workflow ID from geofence ID
            if (geofenceId.startsWith("workflow_")) {
                val workflowId = geofenceId.substringAfter("workflow_").toLongOrNull()
                if (workflowId != null) {
                    // Load workflow and check location triggers
                    checkLocationTriggerForWorkflow(context, workflowId, geofenceTransition)
                }
            }
        }
    }

    private fun checkLocationTriggerForWorkflow(
        context: Context,
        workflowId: Long,
        geofenceTransition: Int?
    ) {
        // Get workflow from database
        val repository = WorkflowRepository(AppDatabase.getDatabase(context).workflowDao())

        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                workflow?.let { wf ->
                    if (wf.isEnabled) {
                        // Parse triggers and find location triggers
                        val triggers = wf.toTriggers()
                        triggers.forEach { trigger ->
                            if (trigger.type == Constants.TRIGGER_LOCATION) {
                                val locationData = TriggerParser.parseLocationData(trigger)
                                locationData?.let { data ->
                                    // Check if transition matches trigger configuration
                                    val shouldTrigger = when (geofenceTransition) {
                                        Geofence.GEOFENCE_TRANSITION_ENTER -> data.triggerOnEntry
                                        Geofence.GEOFENCE_TRANSITION_EXIT -> data.triggerOnExit
                                        else -> false
                                    }

                                    if (shouldTrigger) {
                                        Log.d(TAG, "Location trigger fired for workflow ${wf.workflowName}")
                                        // Execute workflow actions
                                        ActionExecutor.executeWorkflow(context, wf)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onWorkflowError(error: String) {
                Log.e(TAG, "Error loading workflow $workflowId: $error")
            }
        })
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
