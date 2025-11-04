package com.example.autoflow.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toActions
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.TriggerEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ✅ COMPLETELY FIXED Generic Trigger Monitoring Worker
 * Monitors and executes workflow actions
 */
class TriggerMonitoringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TriggerMonitoringWorker"
        const val KEY_WORKFLOW_ID = "workflow_id"
        const val KEY_TRIGGER_TYPE = "trigger_type"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val workflowId = inputData.getLong(KEY_WORKFLOW_ID, -1L)

            if (workflowId == -1L) {
                Log.e(TAG, "❌ Invalid workflow ID")
                return@withContext Result.failure()
            }

            // Get workflow from database
            val database = AppDatabase.getDatabase(applicationContext)
            val workflow = database.workflowDao().getByIdSync(workflowId)

            if (workflow == null || !workflow.isEnabled) {
                return@withContext Result.success()
            }

            // ✅ FIXED: Get all triggers and build current states
            val triggers = workflow.toTriggers()
            val triggerEvaluator = TriggerEvaluator.getInstance()

            val currentStates = triggerEvaluator.buildCurrentStates(
                applicationContext,
                triggers,
                CoroutineScope(Dispatchers.IO)
            )

            // ✅ FIXED: Evaluate with trigger logic
            val shouldExecute = triggerEvaluator.evaluateTriggers(
                triggers,
                workflow.triggerLogic,
                currentStates,
                CoroutineScope(Dispatchers.IO)
            )

            if (shouldExecute) {
                // Execute all actions
                val actions = workflow.toActions()
                val actionExecutor = ActionExecutor.getInstance()

                actions.forEach { action ->
                    // ✅ FIXED: Use proper executeAction method with scope
                    actionExecutor.executeAction(
                        applicationContext,
                        action,
                        CoroutineScope(Dispatchers.IO)
                    )
                }

                Log.d(TAG, "✅ Workflow executed: $workflowId")
            } else {
                Log.d(TAG, "⏭️ Trigger conditions not met for workflow: $workflowId")
            }

            return@withContext Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in worker", e)
            return@withContext Result.retry()
        }
    }
}
