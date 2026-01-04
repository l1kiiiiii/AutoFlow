package com.example.autoflow.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toActions
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Time Trigger Worker - Executes actions at specific times
 * ✅ Refactored to use WorkflowRepository for consistency
 */
class TimeTriggerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TimeTriggerWorker"
        private const val TIME_WINDOW_MS = 60000L // 1 minute tolerance
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val targetTimeMillis = inputData.getLong(Constants.KEY_TIME_TRIGGER, -1L)
            val workflowId = inputData.getLong(Constants.KEY_WORKFLOW_ID, -1L)

            if (targetTimeMillis == -1L || workflowId == -1L) {
                Log.e(TAG, "❌ Invalid input data")
                return@withContext Result.failure()
            }

            val currentTime = System.currentTimeMillis()
            val timeDiff = abs(currentTime - targetTimeMillis)

            when {
                currentTime < targetTimeMillis -> {
                    Log.d(TAG, "⏱️ Target time not reached yet. Retrying.")
                    return@withContext Result.retry()
                }
                timeDiff > TIME_WINDOW_MS -> {
                    Log.d(TAG, "⚠️ Missed time window by ${timeDiff}ms")
                    return@withContext Result.failure()
                }
                else -> {
                    Log.d(TAG, "✅ Target time matched. Executing actions.")
                    return@withContext executeActions(workflowId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in TimeTriggerWorker", e)
            return@withContext Result.failure()
        }
    }

    private suspend fun executeActions(workflowId: Long): Result {
        return try {
            // ✅ FIX: Use Repository instead of direct DAO
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = WorkflowRepository(database.workflowDao())

            // Use the new suspend function
            val workflow = repository.getWorkflowById(workflowId)

            if (workflow == null) {
                Log.e(TAG, "❌ Workflow not found: $workflowId")
                return Result.failure()
            }

            if (!workflow.isEnabled) {
                Log.d(TAG, "⚠️ Workflow disabled: $workflowId")
                return Result.success()
            }

            val actions = workflow.toActions()
            if (actions.isEmpty()) {
                Log.e(TAG, "❌ No valid actions")
                return Result.failure()
            }

            var allSuccessful = true
            actions.forEach { action ->
                val success = ActionExecutor.executeAction(applicationContext, action)
                if (!success) allSuccessful = false
            }

            if (allSuccessful) {
                Log.d(TAG, "✅ All actions executed for workflow: $workflowId")
                Result.success()
            } else {
                Log.e(TAG, "⚠️ Some actions failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing actions", e)
            Result.retry()
        }
    }
}