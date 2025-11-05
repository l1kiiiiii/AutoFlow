package com.example.autoflow.data

import android.util.Log
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import org.json.JSONArray


private const val TAG = "WorkflowEntityExt"



/**
 * Get specific trigger data for TIME triggers
 */
fun WorkflowEntity.getTimeTriggerData(): List<Pair<String, List<String>>> {
    return try {
        val triggersArray = JSONArray(triggerDetails)
        val timeData = mutableListOf<Pair<String, List<String>>>()

        for (i in 0 until triggersArray.length()) {
            val json = triggersArray.getJSONObject(i)
            if (json.optString("type") == "TIME") {
                val time = json.optString("time", "")
                val daysArray = json.optJSONArray("days")
                val days = mutableListOf<String>()

                if (daysArray != null) {
                    for (j in 0 until daysArray.length()) {
                        days.add(daysArray.getString(j))
                    }
                }

                if (time.isNotEmpty()) {
                    timeData.add(Pair(time, days))
                }
            }
        }
        timeData
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing time trigger data: ${e.message}", e)
        emptyList()
    }
}

/**
 * Check if workflow has time-based triggers
 */
fun WorkflowEntity.hasTimeTrigger(): Boolean {
    return try {
        val triggersArray = JSONArray(triggerDetails)
        for (i in 0 until triggersArray.length()) {
            val json = triggersArray.getJSONObject(i)
            if (json.optString("type") == "TIME") {
                return true
            }
        }
        false
    } catch (e: Exception) {
        false
    }
}

/**
 * Get workflow description for display
 */
fun WorkflowEntity.getDescription(): String {
    return try {
        val actions = toActions()
        val triggers = toTriggers()

        val triggerDesc = when {
            triggers.isEmpty() -> "No triggers"
            triggers.size == 1 -> "1 trigger"
            else -> "${triggers.size} triggers"
        }

        val actionDesc = when {
            actions.isEmpty() -> "No actions"
            actions.size == 1 -> "1 action"
            else -> "${actions.size} actions"
        }

        "$triggerDesc, $actionDesc"
    } catch (e: Exception) {
        "Workflow"
    }
}
