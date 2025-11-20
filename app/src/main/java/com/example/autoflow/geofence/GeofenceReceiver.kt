package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import com.example.autoflow.util.TriggerParser
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ Event-Driven Location Receiver
 * This is purely reactive. The OS wakes this up only when a boundary is crossed.
 * No polling, no rescheduling needed - geofences are NEVER_EXPIRE and persistent.
 */
class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // ✅ 1. CRITICAL: Keep process alive for async database work
        val pendingResult = goAsync()

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "❌ Geofencing event is null")
            pendingResult.finish()
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "❌ Geofencing error: ${geofencingEvent.errorCode}")
            pendingResult.finish()
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences.isNullOrEmpty()) {
            pendingResult.finish()
            return
        }

        // ✅ 2. Process in Background (IO Thread)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val workflowDao = database.workflowDao()

                triggeringGeofences.forEach { geofence ->
                    val geofenceId = geofence.requestId
                    Log.d(TAG, "📍 Geofence Event: $geofenceId (Transition: $geofenceTransition)")

                    // Extract Workflow ID from Geofence Request ID (Format: "workflow_{id}")
                    if (geofenceId.startsWith("workflow_")) {
                        val workflowId = geofenceId.substringAfter("workflow_").toLongOrNull()

                        if (workflowId != null) {
                            // Load LATEST data from DB (Source of Truth)
                            val workflow = workflowDao.getByIdSync(workflowId)

                            // ✅ 3. Check if workflow is STILL enabled (User might have disabled it)
                            if (workflow != null && workflow.isEnabled) {
                                processLocationTrigger(context, workflow, geofenceTransition)
                            } else {
                                Log.d(TAG, "⚠️ Ignoring event: Workflow $workflowId disabled or deleted")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing geofence event", e)
            } finally {
                // ✅ 4. Always release the wake lock
                pendingResult.finish()
            }
        }
    }

    private fun processLocationTrigger(
        context: Context,
        workflow: WorkflowEntity,
        transition: Int
    ) {
        val triggers = workflow.toTriggers()

        // Find the location trigger configuration
        val locationTrigger = triggers.find { it.type == Constants.TRIGGER_LOCATION } ?: return
        val config = TriggerParser.parseLocationData(locationTrigger) ?: return

        // ✅ 5. Precise Matching: Only execute if the Transition matches the Config
        val shouldExecute = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> config.triggerOnEntry
            Geofence.GEOFENCE_TRANSITION_EXIT -> config.triggerOnExit
            else -> false
        }

        if (shouldExecute) {
            val transitionName = if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) "ENTER" else "EXIT"
            Log.d(TAG, "✅ Conditions met! Executing '${workflow.workflowName}' on $transitionName")
            ActionExecutor.executeWorkflow(context, workflow)
        } else {
            Log.d(TAG, "⏭️ Event ignored: Transition mismatch (Config: Entry=${config.triggerOnEntry}, Exit=${config.triggerOnExit})")
        }
    }
}
