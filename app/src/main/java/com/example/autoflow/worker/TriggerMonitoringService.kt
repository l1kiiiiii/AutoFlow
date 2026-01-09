package com.example.autoflow.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toActions
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.TriggerEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generic Trigger Monitoring Worker
 * ✅ Refactored to use WorkflowRepository
 */
class TriggerMonitoringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TriggerMonitoringWorker"
        const val KEY_WORKFLOW_ID = "workflow_id"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val workflowId = inputData.getLong(KEY_WORKFLOW_ID, -1L)
            if (workflowId == -1L) {
                Log.e(TAG, "❌ Invalid workflow ID")
                return@withContext Result.failure()
            }

            // ✅ FIX: Use Repository
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = WorkflowRepository(database.workflowDao())

            // Use the new suspend function
            val workflow = repository.getWorkflowById(workflowId)

            if (workflow == null || !workflow.isEnabled) {
                return@withContext Result.success()
            }

            // Get triggers and evaluate
            val triggers = workflow.toTriggers()
            val currentStates = TriggerEvaluator.buildCurrentStates(
                applicationContext,
                triggers
            )

            val shouldExecute = TriggerEvaluator.evaluateWorkflow(
                workflow,
                currentStates
            )

            if (shouldExecute) {
                val actions = workflow.toActions()
                actions.forEach { action ->
                    ActionExecutor.executeAction(applicationContext, action)
                }
                Log.d(TAG, "✅ Workflow executed: $workflowId")
            } else {
                Log.d(TAG, "⚠️ Trigger conditions not met for workflow: $workflowId")
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in worker", e)
            return@withContext Result.retry()
        }
    }
}