package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.TriggerEvaluator
import com.example.autoflow.util.TriggerParser
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ✅ FINAL FIXED GeofenceReceiver - Uses correct method signatures
 * Prevents execution unless user is actually in the correct location
 */
class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🎯 Geofence event received")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Log.e(TAG, "❌ Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        Log.d(TAG, "🌍 Geofence transition: $geofenceTransition")
        Log.d(TAG, "🎯 Triggering geofences: ${triggeringGeofences?.size}")

        triggeringGeofences?.forEach { geofence ->
            val geofenceId = geofence.requestId
            Log.d(TAG, "📍 Processing geofence: $geofenceId, transition: $geofenceTransition")

            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d(TAG, "🚪 User ENTERED geofence: $geofenceId")
                    handleLocationTransition(context, geofenceId, "ENTER")
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d(TAG, "🚪 User EXITED geofence: $geofenceId")
                    handleLocationTransition(context, geofenceId, "EXIT")
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    Log.d(TAG, "🏠 User DWELLING in geofence: $geofenceId")
                    handleLocationTransition(context, geofenceId, "DWELL")
                }
            }
        }
    }

    /**
     * ✅ FIXED: Enhanced location transition with correct method calls
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

        Log.d(TAG, "🔄 Processing location transition for workflow $workflowId")

        receiverScope.launch {
            try {
                val repository = WorkflowRepository(AppDatabase.getDatabase(context).workflowDao())

                // ✅ Use correct callback-based method from your WorkflowRepository
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

                        // ✅ CRITICAL: Execute with location validation in a separate coroutine
                        receiverScope.launch {
                            try {
                                // ✅ CRITICAL: Validate location BEFORE execution using your existing methods
                                val shouldExecute = validateLocationBeforeExecution(context, workflow, transition)

                                if (!shouldExecute) {
                                    Log.w(TAG, "❌ EXECUTION BLOCKED - Location validation failed")
                                    Log.w(TAG, "   User is not in the correct location for this workflow")
                                    return@launch
                                }

                                Log.d(TAG, "✅ Location validated - executing workflow")

                                val success = ActionExecutor.executeWorkflow(context, workflow)



                                if (success) {
                                    Log.d(TAG, "🎉 Workflow executed successfully: ${workflow.workflowName}")
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
     * ✅ FIXED: Validate location using your existing TriggerEvaluator methods with correct signatures
     */
    /**
     * ✅ FIXED: Validate location using suspend version for real GPS checking
     */
    private suspend fun validateLocationBeforeExecution(
        context: Context,
        workflow: com.example.autoflow.data.WorkflowEntity,
        transition: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // ✅ Get location triggers from workflow
            val triggers = workflow.toTriggers()
            val locationTriggers = triggers.filter { it.type == "LOCATION" }

            if (locationTriggers.isEmpty()) {
                Log.d(TAG, "📍 No location triggers - allowing execution")
                return@withContext true
            }

            Log.d(TAG, "🔍 Validating ${locationTriggers.size} location trigger(s)")

            // ✅ CRITICAL: Manual location validation for each location trigger
            for (locationTrigger in locationTriggers) {
                val isLocationValid = TriggerEvaluator.validateCurrentLocationForTrigger(context, locationTrigger)

                if (!isLocationValid) {
                    Log.w(TAG, "❌ Location validation failed for trigger")
                    return@withContext false
                }
            }

            // ✅ For non-location triggers, use standard evaluation
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

            Log.d(TAG, "✅ All triggers validated successfully")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating location", e)
            return@withContext false
        }
    }

}
