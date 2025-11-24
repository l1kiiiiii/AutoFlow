package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.TriggerEvaluator
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ✅ ZERO COOLDOWN VERSION - Instant re-execution on every entry
 * No delays, no throttling - executes immediately every time
 */
class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val PREFS_NAME = "geofence_states"
        private const val KEY_PREFIX_LOCATION_STATE = "location_state_"
    }

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🎯 ========= Geofence Event Received =========")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Log.e(TAG, "❌ Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        Log.d(TAG, "🌍 Transition type: ${transitionToString(geofenceTransition)}")
        Log.d(TAG, "📍 Triggering geofences: ${triggeringGeofences?.size}")

        triggeringGeofences?.forEach { geofence ->
            val geofenceId = geofence.requestId
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "Processing: $geofenceId")

            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d(TAG, "🚪 User ENTERED: $geofenceId")
                    handleLocationTransition(context, geofenceId, "ENTER")
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d(TAG, "🚪 User EXITED: $geofenceId")
                    handleLocationTransition(context, geofenceId, "EXIT")
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    Log.d(TAG, "🏠 User DWELLING: $geofenceId")
                    handleLocationTransition(context, geofenceId, "DWELL")
                }
            }
        }

        Log.d(TAG, "✅ ========= Event Processing Complete =========")
    }

    /**
     * ✅ INSTANT EXECUTION - No cooldown, no throttling
     */
    private fun handleLocationTransition(context: Context, geofenceId: String, transition: String) {
        if (!geofenceId.startsWith("workflow_")) {
            Log.w(TAG, "⚠️ Invalid geofence ID format: $geofenceId")
            return
        }

        val workflowId = geofenceId.substringAfter("workflow_").toLongOrNull()
        if (workflowId == null || workflowId <= 0) {
            Log.e(TAG, "❌ Invalid workflow ID from geofence: $geofenceId")
            return
        }

        receiverScope.launch {
            try {
                val prefs = getSharedPreferences(context)
                val currentState = getCurrentLocationState(prefs, workflowId)
                val previousState = getPreviousLocationState(prefs, workflowId)

                Log.d(TAG, "📊 State Info for workflow $workflowId:")
                Log.d(TAG, "   Previous: $previousState → Current: $currentState → New: $transition")

                // ✅ Update location state
                when (transition) {
                    "ENTER" -> {
                        updateLocationState(prefs, workflowId, "INSIDE")
                        Log.d(TAG, "✅ State updated: User is now INSIDE location")
                    }
                    "EXIT" -> {
                        updateLocationState(prefs, workflowId, "OUTSIDE")
                        Log.d(TAG, "✅ State updated: User is now OUTSIDE location")
                        Log.d(TAG, "♻️ Ready for instant re-execution on next entry")
                        return@launch // Don't execute on EXIT
                    }
                    "DWELL" -> {
                        updateLocationState(prefs, workflowId, "DWELLING")
                        Log.d(TAG, "✅ State updated: User is DWELLING in location")
                    }
                }

                // ✅ Check if we should execute based on state transition
                val shouldExecute = when (transition) {
                    "ENTER" -> {
                        // ✅ INSTANT EXECUTION: Execute on EVERY entry, regardless of timing
                        val isValidTransition = previousState == "OUTSIDE" || previousState == "UNKNOWN"
                        Log.d(TAG, "⚡ Entry detected - INSTANT EXECUTION ${if (isValidTransition) "ENABLED" else "SKIPPED"}")
                        isValidTransition
                    }
                    "EXIT" -> false
                    "DWELL" -> currentState == "INSIDE"
                    else -> false
                }

                if (!shouldExecute) {
                    Log.w(TAG, "⏸️ Execution skipped - invalid state transition")
                    return@launch
                }

                // ✅ NO COOLDOWN CHECK - Execute immediately every time

                // ✅ Load and validate workflow
                val repository = WorkflowRepository(AppDatabase.getDatabase(context).workflowDao())

                repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
                    override fun onWorkflowLoaded(workflow: com.example.autoflow.data.WorkflowEntity?) {
                        if (workflow == null) {
                            Log.e(TAG, "❌ Workflow $workflowId not found")
                            return
                        }

                        if (!workflow.isEnabled) {
                            Log.w(TAG, "⚠️ Workflow ${workflow.workflowName} is disabled")
                            return
                        }

                        Log.d(TAG, "📋 Workflow found: ${workflow.workflowName}")
                        Log.d(TAG, "⚡ INSTANT EXECUTION MODE - No delays")

                        // ✅ Execute immediately with GPS validation
                        receiverScope.launch {
                            try {
                                Log.d(TAG, "🔐 Starting validation and execution...")

                                // ✅ Real-time GPS location validation
                                val validationResult = validateLocationWithGPS(context, workflow, transition)

                                if (!validationResult) {
                                    Log.e(TAG, "❌ EXECUTION BLOCKED - GPS validation failed")
                                    Log.e(TAG, "   User is not actually in the trigger location")
                                    return@launch
                                }

                                Log.d(TAG, "✅ GPS validation passed")
                                Log.d(TAG, "⚡ EXECUTING INSTANTLY...")

                                // ✅ INSTANT EXECUTION
                                val startTime = System.currentTimeMillis()
                                val success = ActionExecutor.executeWorkflow(context, workflow)
                                val executionTime = System.currentTimeMillis() - startTime

                                if (success) {
                                    Log.d(TAG, "🎉 ⚡ INSTANT EXECUTION COMPLETE: ${workflow.workflowName}")
                                    Log.d(TAG, "   Execution time: ${executionTime}ms")
                                } else {
                                    Log.e(TAG, "❌ Workflow execution failed: ${workflow.workflowName}")
                                }

                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error executing workflow", e)
                            }
                        }
                    }

                    override fun onWorkflowError(error: String) {
                        Log.e(TAG, "❌ Error loading workflow $workflowId: $error")
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing location transition", e)
            }
        }
    }

    /**
     * ✅ Validate user location with real-time GPS check
     */
    private suspend fun validateLocationWithGPS(
        context: Context,
        workflow: com.example.autoflow.data.WorkflowEntity,
        transition: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🛰️ Performing GPS validation...")

            val triggers = workflow.toTriggers()
            val locationTriggers = triggers.filter { it.type == "LOCATION" }

            if (locationTriggers.isEmpty()) {
                Log.d(TAG, "📍 No location triggers - validation passed")
                return@withContext true
            }

            // ✅ Validate each location trigger with current GPS position
            for (trigger in locationTriggers) {
                val isLocationValid = TriggerEvaluator.validateCurrentLocationForTrigger(context, trigger)

                if (!isLocationValid) {
                    Log.w(TAG, "❌ GPS validation failed for trigger")
                    return@withContext false
                }
            }

            // ✅ Check non-location triggers
            val nonLocationTriggers = triggers.filter { it.type != "LOCATION" }
            if (nonLocationTriggers.isNotEmpty()) {
                val currentStates = TriggerEvaluator.buildCurrentStates(context, nonLocationTriggers)
                val nonLocationValid = TriggerEvaluator.evaluateTriggers(
                    triggers = nonLocationTriggers,
                    logic = workflow.triggerLogic,
                    currentStates = currentStates
                )

                if (!nonLocationValid) {
                    Log.w(TAG, "❌ Non-location triggers not satisfied")
                    return@withContext false
                }
            }

            Log.d(TAG, "✅ All triggers validated")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in GPS validation", e)
            return@withContext false
        }
    }

    // ========== STATE MANAGEMENT HELPERS (NO COOLDOWN) ==========

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getCurrentLocationState(prefs: SharedPreferences, workflowId: Long): String {
        return prefs.getString("${KEY_PREFIX_LOCATION_STATE}${workflowId}_current", "UNKNOWN") ?: "UNKNOWN"
    }

    private fun getPreviousLocationState(prefs: SharedPreferences, workflowId: Long): String {
        return prefs.getString("${KEY_PREFIX_LOCATION_STATE}${workflowId}_previous", "UNKNOWN") ?: "UNKNOWN"
    }

    private fun updateLocationState(prefs: SharedPreferences, workflowId: Long, newState: String) {
        val currentState = getCurrentLocationState(prefs, workflowId)
        prefs.edit().apply {
            putString("${KEY_PREFIX_LOCATION_STATE}${workflowId}_previous", currentState)
            putString("${KEY_PREFIX_LOCATION_STATE}${workflowId}_current", newState)
            apply()
        }
        Log.d(TAG, "💾 State saved: $currentState → $newState")
    }

    private fun transitionToString(transition: Int?): String {
        return when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
            else -> "UNKNOWN($transition)"
        }
    }
}
