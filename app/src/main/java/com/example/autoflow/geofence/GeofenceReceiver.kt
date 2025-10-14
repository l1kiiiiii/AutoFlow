package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.policy.BlockPolicy
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * GeofenceReceiver handles location-based workflow triggers.
 *
 * Features:
 * - Execute workflows when entering/exiting geofences
 * - Auto-unblock apps when leaving a location with entry trigger
 * - Support for "entry", "exit", and "both" trigger types
 */
class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📍 Geofence event received")

        // Parse geofencing event
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorCode = geofencingEvent?.errorCode ?: -1
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(errorCode)
            Log.e(TAG, "❌ Geofence error: $errorMessage (code: $errorCode)")
            return
        }

        // Determine transition type (enter/exit)
        val transitionType = when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "enter"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "exit"
            else -> {
                Log.w(TAG, "⚠️ Unhandled geofence transition: ${geofencingEvent.geofenceTransition}")
                return
            }
        }

        Log.d(TAG, "🚪 Geofence transition: $transitionType")

        // Keep receiver alive for async processing
        val pendingResult = goAsync()

        // Process each triggering geofence
        geofencingEvent.triggeringGeofences?.forEach { geofence ->
            val workflowId = geofence.requestId.substringAfter("workflow_").toLongOrNull()

            if (workflowId != null && workflowId != 0L) {
                Log.d(TAG, "✅ Processing workflow ID: $workflowId")
                handleGeofenceTransition(context, workflowId, transitionType, pendingResult)
            } else {
                Log.e(TAG, "❌ Invalid geofence request ID: ${geofence.requestId}")
            }
        }
    }

    /**
     * Handles geofence transition for a specific workflow.
     *
     * @param context Application context
     * @param workflowId The workflow to process
     * @param transitionType "enter" or "exit"
     * @param pendingResult PendingResult to finish when done
     */
    private fun handleGeofenceTransition(
        context: Context,
        workflowId: Long,
        transitionType: String,
        pendingResult: PendingResult
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch workflow from database
                val dao = AppDatabase.getDatabase(context).workflowDao()
                val workflow = dao.getByIdSync(workflowId)

                if (workflow == null) {
                    Log.e(TAG, "❌ Workflow $workflowId not found in database")
                    return@launch
                }

                if (!workflow.isEnabled) {
                    Log.w(TAG, "⚠️ Workflow $workflowId is disabled, skipping")
                    return@launch
                }

                // Extract location trigger from workflow
                val locationTrigger = workflow.toTriggers()
                    .filterIsInstance<com.example.autoflow.model.Trigger.LocationTrigger>()
                    .firstOrNull()

                if (locationTrigger == null) {
                    Log.e(TAG, "❌ No location trigger found for workflow $workflowId")
                    return@launch
                }

                // Determine if workflow should execute
                val triggerOn = locationTrigger.triggerOn.lowercase()
                val shouldExecute = when (triggerOn) {
                    "both" -> true
                    "entry", "enter" -> transitionType == "enter"
                    "exit" -> transitionType == "exit"
                    else -> false
                }

                Log.d(TAG, "Workflow: ${workflow.workflowName}, Trigger: $triggerOn, Transition: $transitionType")

                when {
                    // ✅ Execute workflow on matching transition
                    shouldExecute -> {
                        val action = if (transitionType == "enter") "ENTERING" else "EXITING"
                        Log.d(TAG, "✅ $action: ${workflow.workflowName}")
                        ActionExecutor.executeWorkflow(context, workflow)
                    }

                    // ✅ Auto-unblock on exit if entry trigger
                    !shouldExecute && transitionType == "exit" && (triggerOn == "entry" || triggerOn == "enter") -> {
                        Log.d(TAG, "🔓 AUTO-UNBLOCK on exit: ${workflow.workflowName}")
                        BlockPolicy.clearBlockedPackages(context)
                    }

                    // ⏭️ Skip execution
                    else -> {
                        Log.d(TAG, "⏭️ Skipping: workflow expects '$triggerOn' but got '$transitionType'")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling geofence for workflow $workflowId", e)
            } finally {
                // Always finish the pending result
                pendingResult.finish()
            }
        }
    }
}
