package com.example.autoflow.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toTriggers
import com.example.autoflow.domain.model.Trigger
import com.example.autoflow.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    private const val PREFS_NAME = "alarm_prefs"
    private const val KEY_ALARM_IDS = "alarm_ids"

    /**
     * Schedule all alarms for a workflow
     */
    fun scheduleWorkflow(context: Context, workflow: WorkflowEntity) {
        try {
            val triggers = workflow.toTriggers()
            val timeTriggers = triggers.filter { it.type == "TIME" }

            if (timeTriggers.isEmpty()) {
                Log.d(TAG, "⏰ No time triggers found for workflow: ${workflow.workflowName}")
                return
            }

            timeTriggers.forEachIndexed { index, trigger ->
                val timeData = TriggerParser.parseTimeData(trigger)
                if (timeData != null) {
                    val (time, days) = timeData
                    scheduleAlarmForTime(context, workflow, time, days, index)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling workflow: ${workflow.workflowName}", e)
        }
    }

    /**
     * Schedule a single alarm for a time trigger
     */
    private fun scheduleAlarmForTime(
        context: Context,
        workflow: WorkflowEntity,
        time: String,
        days: List<String>,
        index: Int
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val calendar = Calendar.getInstance()
            val timeParts = time.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            calendar.set(Calendar.SECOND, 0)

            // If time has passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("workflow_id", workflow.id)
                putExtra("workflow_name", workflow.workflowName)
                putExtra("trigger_time", time)
            }

            val requestCode = (workflow.id.toString() + index.toString()).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            Log.d(TAG, "⏰ Alarm scheduled for ${workflow.workflowName} at $time")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling alarm", e)
        }
    }

    /**
     * Cancel all alarms for a workflow
     * ✅ FIXED: Proper error handling
     */
    fun cancelWorkflowAlarms(context: Context, workflowId: Long) {
        // ✅ FIXED: Validate workflow ID
        if (workflowId <= 0) {
            Log.d(TAG, "🚫 Cancelling alarms for workflow ID: $workflowId (skipping - invalid ID)")
            return
        }

        Log.d(TAG, "🚫 Cancelling alarms for workflow ID: $workflowId")

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIds = getAlarmIds(context, workflowId)

            if (alarmIds.isEmpty()) {
                Log.d(TAG, "⚠️ No alarms found for workflow $workflowId")
                return
            }

            alarmIds.forEach { requestCode ->
                try {
                    val intent = Intent(context, AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    Log.d(TAG, "✅ Cancelled alarm with requestCode: $requestCode")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error cancelling alarm $requestCode", e)
                }
            }

            // Clear saved IDs
            clearAlarmIds(context, workflowId)
            Log.d(TAG, "✅ Cancelled ${alarmIds.size} alarms for workflow $workflowId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling workflow alarms", e)
        }
    }

    /**
     * Generate unique request code for alarm
     * ✅ FIXED: Better hash generation
     */
    private fun generateRequestCode(workflowId: Long, time: String): Int {
        return "$workflowId-$time".hashCode()
    }

    /**
     * Parse time string (HH:mm) to hour and minute
     */
    private fun parseTime(timeString: String): Pair<Int, Int>? {
        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                if (hour in 0..23 && minute in 0..59) {
                    Pair(hour, minute)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save alarm ID to shared preferences
     */
    private fun saveAlarmId(context: Context, workflowId: Long, requestCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "workflow_$workflowId"
        val existingIds = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        existingIds.add(requestCode.toString())
        prefs.edit().putStringSet(key, existingIds).apply()
    }

    /**
     * Get saved alarm IDs for a workflow
     * ✅ FIXED: Proper error handling for parsing
     */
    private fun getAlarmIds(context: Context, workflowId: Long): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "workflow_$workflowId"
        val idsSet = prefs.getStringSet(key, emptySet()) ?: emptySet()

        return idsSet.mapNotNull { idString ->
            try {
                idString.toInt()
            } catch (e: NumberFormatException) {
                Log.e(TAG, "❌ Failed to parse alarm ID: $idString", e)
                null
            }
        }
    }

    /**
     * Clear saved alarm IDs for a workflow
     */
    private fun clearAlarmIds(context: Context, workflowId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "workflow_$workflowId"
        prefs.edit().remove(key).apply()
    }

    /**
     * Clear all alarm IDs (for testing/debugging)
     */
    fun clearAllAlarmIds(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "🧹 Cleared all alarm IDs")
    }
}
