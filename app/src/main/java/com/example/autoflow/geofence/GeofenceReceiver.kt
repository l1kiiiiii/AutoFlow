package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.policy.BlockPolicy
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.json.JSONObject
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toActions
import com.example.autoflow.util.TriggerParser
import com.example.autoflow.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ ENHANCED GeofenceReceiver with auto-unblocking
 */
class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

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
            Log.d(TAG, "Geofence triggered: $geofenceId, transition: $geofenceTransition")

            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    handleLocationEntry(context, geofenceId)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    // ✅ NEW: Handle location exit with auto-unblocking
                    handleLocationExit(context, geofenceId)
                }
            }
        }
    }

    private fun handleLocationEntry(context: Context, geofenceId: String) {
        if (geofenceId.startsWith("workflow_")) {
            val workflowId = geofenceId.substringAfter("workflow_").toLongOrNull()
            if (workflowId != null) {
                checkLocationTriggerForWorkflow(context, workflowId, "ENTER")
            }
        }
    }

    /**
     * ✅ NEW: Handle location exit with automatic app unblocking
     */
    private fun handleLocationExit(context: Context, geofenceId: String) {
        Log.d(TAG, "🚪 User exited geofence: $geofenceId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if any apps were blocked for this location
                val locationId = geofenceId.replace("workflow_", "")
                val unblockedApps = BlockPolicy.unblockAppsForLocation(context, locationId)

                if (unblockedApps.isNotEmpty()) {
                    Log.d(TAG, "✅ Auto-unblocked ${unblockedApps.size} apps due to location exit")
                }

                // Also check if workflow should be deactivated
                if (geofenceId.startsWith("workflow_")) {
                    val workflowId = geofenceId.substringAfter("workflow_").toLongOrNull()
                    if (workflowId != null) {
                        checkLocationExitTriggerForWorkflow(context, workflowId, "EXIT")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling location exit", e)
            }
        }
    }

    private fun checkLocationTriggerForWorkflow(
        context: Context,
        workflowId: Long,
        transition: String
    ) {
        val repository = WorkflowRepository(AppDatabase.getDatabase(context).workflowDao())

        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                workflow?.let { wf ->
                    if (wf.isEnabled) {
                        val triggers = wf.toTriggers()

                        triggers.forEach { trigger ->
                            if (trigger.type == Constants.TRIGGER_LOCATION) {
                                val locationData = TriggerParser.parseLocationData(trigger)
                                locationData?.let { data ->
                                    val shouldTrigger = when (transition) {
                                        "ENTER" -> data.triggerOnEntry
                                        "EXIT" -> data.triggerOnExit
                                        else -> false
                                    }

                                    if (shouldTrigger) {
                                        Log.d(TAG, "Location trigger fired for workflow: ${wf.workflowName} (transition: $transition)")

                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val actionExecutor = ActionExecutor.getInstance()
                                                actionExecutor.executeWorkflow(context, wf)

                                                // ✅ NEW: Track location-based blocking
                                                val actions = wf.toActions()
                                                val blockActions = actions.filter {
                                                    it.type == Constants.ACTION_BLOCK_APPS
                                                }

                                                blockActions.forEach { blockAction ->
                                                    val packages = blockAction.value?.split(",")
                                                        ?.map { it.trim() }
                                                        ?.filter { it.isNotEmpty() }
                                                        ?: emptyList()

                                                    if (packages.isNotEmpty()) {
                                                        val locationId = data.locationName ?: "location_${workflowId}"
                                                        BlockPolicy.blockAppsForLocation(
                                                            context,
                                                            locationId,
                                                            packages,
                                                            workflowId
                                                        )
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error executing workflow actions", e)
                                            }
                                        }
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
     * ✅ NEW: Handle location exit triggers and unblocking
     */
    private fun checkLocationExitTriggerForWorkflow(
        context: Context,
        workflowId: Long,
        transition: String
    ) {
        val repository = WorkflowRepository(AppDatabase.getDatabase(context).workflowDao())

        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                workflow?.let { wf ->
                    if (wf.isEnabled) {
                        val triggers = wf.toTriggers()

                        triggers.forEach { trigger ->
                            if (trigger.type == Constants.TRIGGER_LOCATION) {
                                val locationData = TriggerParser.parseLocationData(trigger)
                                locationData?.let { data ->
                                    if (data.triggerOnExit) {
                                        Log.d(TAG, "Location exit trigger - executing unblock actions for workflow: ${wf.workflowName}")

                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                // Execute unblock actions
                                                val actions = wf.toActions()
                                                val unblockActions = actions.filter {
                                                    it.type == Constants.ACTION_UNBLOCK_APPS
                                                }

                                                if (unblockActions.isNotEmpty()) {
                                                    val actionExecutor = ActionExecutor.getInstance()
                                                    actionExecutor.executeWorkflow(context, wf)
                                                } else {
                                                    // If no explicit unblock action, auto-unblock workflow apps
                                                    BlockPolicy.unblockAppsForWorkflow(context, workflowId)
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error executing location exit actions", e)
                                            }
                                        }
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
}
