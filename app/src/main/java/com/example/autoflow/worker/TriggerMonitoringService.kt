package com.example.autoflow.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TriggerMonitoringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "TriggerMonitoringWorker"
        const val KEY_WORKFLOW_ID = "workflow_id"
        const val KEY_TRIGGER_TYPE = "trigger_type"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val workflowId = inputData.getLong(KEY_WORKFLOW_ID, -1L)

            if (workflowId == -1L) {
                Log.e(TAG, "Invalid workflow ID")
                return@withContext Result.failure()
            }

            // Get workflow from database using correct method name
            val database = AppDatabase.getDatabase(applicationContext)
            val workflow = database.workflowDao().getByIdSync(workflowId) // FIXED: use getByIdSync()

            if (workflow == null) {
                Log.d(TAG, "Workflow not found: $workflowId")
                return@withContext Result.success()
            }

            if (!workflow.isEnabled) { // FIXED: use isEnabled (getter method)
                Log.d(TAG, "Workflow is disabled: $workflowId")
                return@withContext Result.success()
            }

            // Execute action
            val action = workflow.toAction() // This is correct - method exists in WorkflowEntity
            if (action != null) {
                val success = ActionExecutor.executeAction(applicationContext, action)

                if (success) {
                    Log.d(TAG, "Action executed successfully for workflow: $workflowId")
                    return@withContext Result.success()
                } else {
                    Log.e(TAG, "Action execution failed for workflow: $workflowId")
                    return@withContext Result.retry()
                }
            } else {
                Log.e(TAG, "No valid action found for workflow: $workflowId")
                return@withContext Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in worker", e)
            return@withContext Result.retry()
        }
    }
}
