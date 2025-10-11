package com.example.autoflow.data

import android.util.Log
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import org.json.JSONObject

/**
 * Extension functions for WorkflowEntity
 * Parse JSON to Action and Trigger objects
 */

private const val TAG = "WorkflowEntityExt"

/**
 * Convert WorkflowEntity to Action object
 */
fun WorkflowEntity.toAction(): Action? {
    return try {
        if (actionDetails.isBlank()) {
            Log.e(TAG, "❌ Empty action details")
            return null
        }

        val json = JSONObject(actionDetails)
        val type = json.optString("type", "")

        if (type.isBlank()) {
            Log.e(TAG, "❌ Action type missing")
            return null
        }

        // ✅ FIXED: Create Action with correct constructor
        val action = Action(
            type = type,
            title = json.optString("title"),
            message = json.optString("message"),
            priority = json.optString("priority")
        )

        // ✅ Set value separately if it exists
        json.optString("value").takeIf { it.isNotEmpty() }?.let {
            action.value = it
        }

        action
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error parsing action: ${e.message}")
        null
    }
}

/**
 * Convert WorkflowEntity to Trigger object
 */
fun WorkflowEntity.toTrigger(): Trigger? {
    return try {
        if (triggerDetails.isBlank()) {
            Log.e(TAG, "❌ Empty trigger details")
            return null
        }

        val json = JSONObject(triggerDetails)
        val type = json.optString("type", "")
        val value = json.optString("value", "")

        if (type.isBlank() || value.isBlank()) {
            Log.e(TAG, "❌ Trigger type or value missing")
            return null
        }

        Trigger(
            type = type,
            value = value
        )
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error parsing trigger: ${e.message}")
        null
    }
}
