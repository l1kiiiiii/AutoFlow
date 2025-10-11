package com.example.autoflow.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toAction
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Time Trigger Worker - Executes actions at specific times
 * Uses CoroutineWorker for proper async handling
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
            // Get input data
            val targetTimeMillis = inputData.getLong(Constants.KEY_TIME_TRIGGER, -1L)
            val workflowId = inputData.getLong(Constants.KEY_WORKFLOW_ID, -1L)

            if (targetTimeMillis == -1L || workflowId == -1L) {
                Log.e(TAG, "❌ Invalid input data")
                return@withContext Result.failure()
            }

            // Check if time has arrived
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
                    Log.d(TAG, "✅ Target time matched. Executing action.")
                    return@withContext executeAction(workflowId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in TimeTriggerWorker", e)
            return@withContext Result.failure()
        }
    }

    private suspend fun executeAction(workflowId: Long): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val workflow = database.workflowDao().getByIdSync(workflowId)

            if (workflow == null) {
                Log.e(TAG, "❌ Workflow not found: $workflowId")
                return Result.failure()
            }

            if (!workflow.isEnabled) {
                Log.d(TAG, "⚠️ Workflow disabled: $workflowId")
                return Result.success()
            }

            val action = workflow.toAction()
            if (action == null) {
                Log.e(TAG, "❌ No valid action")
                return Result.failure()
            }

            val success = ActionExecutor.executeAction(applicationContext, action)
            if (success) {
                Log.d(TAG, "✅ Action executed for workflow: $workflowId")
                Result.success()
            } else {
                Log.e(TAG, "❌ Action execution failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing action", e)
            Result.retry()
        }
    }
}
