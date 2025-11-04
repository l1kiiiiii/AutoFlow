package com.example.autoflow.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toActions
import com.example.autoflow.data.toTriggers
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.TriggerParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ✅ COMPLETELY FIXED Time Trigger Worker
 */
class TimeTriggerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TimeTriggerWorker"
        const val KEY_WORKFLOW_ID = "workflow_id"
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

            Log.d(TAG, "⏰ Processing time trigger for workflow: ${workflow.workflowName}")

            // Check if current time matches any time triggers
            val triggers = workflow.toTriggers()
            val timeTriggersMatched = triggers.any { trigger ->
                if (trigger.type == "TIME") {
                    val timeData = TriggerParser.parseTimeData(trigger)
                    timeData?.let { (targetTime, days) ->
                        isTimeMatch(targetTime, days)
                    } ?: false
                } else false
            }

            if (timeTriggersMatched) {
                // Execute workflow actions
                val actions = workflow.toActions()
                val actionExecutor = ActionExecutor.getInstance()

                actions.forEach { action ->
                    // ✅ FIXED: Use proper executeAction method with coroutine scope
                    actionExecutor.executeAction(
                        applicationContext,
                        action,
                        CoroutineScope(Dispatchers.IO)
                    )
                }

                Log.d(TAG, "✅ Time trigger executed for workflow: ${workflow.workflowName}")
            } else {
                Log.d(TAG, "⏭️ Time conditions not met for workflow: ${workflow.workflowName}")
            }

            return@withContext Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in time trigger worker", e)
            return@withContext Result.retry()
        }
    }

    private fun isTimeMatch(targetTime: String, days: List<String>): Boolean {
        return try {
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)

            val timeParts = targetTime.split(":")
            if (timeParts.size != 2) return false

            val targetHour = timeParts[0].toIntOrNull() ?: return false
            val targetMinute = timeParts[1].toIntOrNull() ?: return false

            // Check time match (within 1 minute tolerance)
            val timeMatches = currentHour == targetHour &&
                    kotlin.math.abs(currentMinute - targetMinute) <= 1

            // Check day match
            val dayMatches = if (days.isEmpty()) {
                true
            } else {
                val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val currentDayName = when (currentDayOfWeek) {
                    java.util.Calendar.SUNDAY -> "sunday"
                    java.util.Calendar.MONDAY -> "monday"
                    java.util.Calendar.TUESDAY -> "tuesday"
                    java.util.Calendar.WEDNESDAY -> "wednesday"
                    java.util.Calendar.THURSDAY -> "thursday"
                    java.util.Calendar.FRIDAY -> "friday"
                    java.util.Calendar.SATURDAY -> "saturday"
                    else -> ""
                }

                days.any { it.lowercase() == currentDayName }
            }

            timeMatches && dayMatches
        } catch (e: Exception) {
            Log.e(TAG, "Error checking time match", e)
            false
        }
    }
}
