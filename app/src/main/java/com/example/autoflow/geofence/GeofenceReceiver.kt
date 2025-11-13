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
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *  GeofenceReceiver - Allows workflows to execute multiple times
 * Problem: Workflows were only executing once due to overly strict, redundant validation.
 * Solution: Removed faulty validation and added execution tracking with a cooldown period to allow re-triggering without spam.
 */
class GeofenceReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        // Constants for execution tracking
        private const val PREFS_NAME = "workflow_execution_tracker"
        private const val COOLDOWN_PERIOD_MS = 60_000L // 60 seconds cooldown. Adjust as needed.
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
            Log.d(TAG, "📍 Processing geofence: $geofenceId")

            // We typically only care about entering or dwelling
            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d(TAG, "🚪 User ENTERED geofence: $geofenceId")
                    handleLocationTransition(context, geofenceId, "ENTER")
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    Log.d(TAG, "🏠 User DWELLING in geofence: $geofenceId")
                    handleLocationTransition(context, geofenceId, "DWELL")
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d(TAG, "🚪 User EXITED geofence: $geofenceId")
                    // Usually, no action is needed on EXIT, but you could add logic here
                }
            }
        }
    }

    /**
     *  Handle location transition with execution tracking and cooldown
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

        // CHECK COOLDOWN: Prevent spam but allow re-execution
        if (!canExecuteWorkflow(context, workflowId)) {
            Log.w(TAG, "⏳ Workflow $workflowId is in cooldown period, skipping")
            return
        }

        Log.d(TAG, "🔄 Processing location transition for workflow $workflowId")

        receiverScope.launch {
            try {
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

                        receiverScope.launch {
                            try {
                                //  We trust the geofence event for location.
                                // We only need to validate non-location triggers.
                                val nonLocationTriggersValid = validateNonLocationTriggers(context, workflow)

                                if (!nonLocationTriggersValid) {
                                    Log.w(TAG, "❌ EXECUTION BLOCKED - Non-location triggers not met")
                                    return@launch
                                }

                                Log.d(TAG, "✅ Geofence event + non-location triggers validated - executing workflow")

                                // TRACK EXECUTION
                                recordWorkflowExecution(context, workflowId)

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
     * Check if workflow can be executed (cooldown check)
     */
    private fun canExecuteWorkflow(context: Context, workflowId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastExecutionKey = "last_execution_$workflowId"
        val lastExecutionTime = prefs.getLong(lastExecutionKey, 0L)
        val currentTime = System.currentTimeMillis()

        val timeSinceLastExecution = currentTime - lastExecutionTime
        val canExecute = timeSinceLastExecution >= COOLDOWN_PERIOD_MS

        if (!canExecute) {
            val remainingCooldown = (COOLDOWN_PERIOD_MS - timeSinceLastExecution) / 1000
            Log.d(TAG, "⏳ Cooldown: ${remainingCooldown}s remaining for workflow $workflowId")
        }

        return canExecute
    }

    /**
     * Record workflow execution time and count
     */
    private fun recordWorkflowExecution(context: Context, workflowId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        val countKey = "execution_count_$workflowId"
        val newCount = prefs.getInt(countKey, 0) + 1

        prefs.edit()
            .putLong("last_execution_$workflowId", currentTime)
            .putInt(countKey, newCount)
            .apply()

        Log.d(TAG, "📊 Workflow $workflowId executed. Total executions: $newCount")
    }

    /**
     * Validate only non-location triggers. Location is already validated by the geofence.
     */
    private suspend fun validateNonLocationTriggers(
        context: Context,
        workflow: com.example.autoflow.data.WorkflowEntity
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val triggers = workflow.toTriggers()
            val nonLocationTriggers = triggers.filter { it.type != "LOCATION" }

            if (nonLocationTriggers.isEmpty()) {
                Log.d(TAG, "✅ No non-location triggers - allowing execution")
                return@withContext true
            }

            Log.d(TAG, "🔍 Validating ${nonLocationTriggers.size} non-location trigger(s)")

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

            Log.d(TAG, "✅ All non-location triggers validated successfully")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating non-location triggers", e)
            return@withContext false
        }
    }
}