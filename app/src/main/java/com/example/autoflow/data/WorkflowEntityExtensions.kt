package com.example.autoflow.data

import android.util.Log
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import org.json.JSONArray
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

        // Create Action with correct constructor
        val action = Action(
            type = type,
            title = json.optString("title"),
            message = json.optString("message"),
            priority = json.optString("priority")
        )

        // Set value separately if it exists
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
 * Convert WorkflowEntity to list of Triggers
 */
fun WorkflowEntity.toTriggers(): List<Trigger> {
    return try {
        if (triggerDetails.isBlank()) {
            Log.e(TAG, "❌ Empty trigger details")
            return emptyList()
        }

        val triggersArray = JSONArray(triggerDetails)
        val triggers = mutableListOf<Trigger>()

        for (i in 0 until triggersArray.length()) {
            val json = triggersArray.getJSONObject(i)
            val type = json.optString("type", "")

            val trigger = when (type) {
                "LOCATION" -> Trigger.LocationTrigger(
                    locationName = json.optString("locationName", "Unknown"),
                    latitude = json.optDouble("latitude", 0.0),
                    longitude = json.optDouble("longitude", 0.0),
                    radius = json.optDouble("radius", 100.0),
                    triggerOnEntry = json.optBoolean("triggerOnEntry", true),
                    triggerOnExit = json.optBoolean("triggerOnExit", false),
                    triggerOn = json.optString("triggerOn", "both")
                )
                "TIME" -> {
                    val daysArray = json.optJSONArray("days")
                    val days = mutableListOf<String>()
                    if (daysArray != null) {
                        for (j in 0 until daysArray.length()) {
                            days.add(daysArray.getString(j))
                        }
                    }
                    Trigger.TimeTrigger(
                        time = json.optString("time", ""),
                        days = days
                    )
                }
                "BATTERY" -> Trigger.BatteryTrigger(
                    level = json.optInt("level", 20),
                    condition = json.optString("condition", "below")
                )
                else -> {
                    Log.w(TAG, "⚠️ Unknown trigger type: $type")
                    null
                }
            }

            trigger?.let { triggers.add(it) }
        }

        triggers
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error parsing triggers: ${e.message}", e)
        emptyList()
    }
}

/**
 * Convert WorkflowEntity to list of Actions
 */
fun WorkflowEntity.toActions(): List<Action> {
    return try {
        if (actionDetails.isBlank()) {
            Log.e(TAG, "❌ Empty action details")
            return emptyList()
        }

        val actionsArray = JSONArray(actionDetails)
        val actions = mutableListOf<Action>()

        for (i in 0 until actionsArray.length()) {
            val json = actionsArray.getJSONObject(i)
            val type = json.optString("type", "")

            if (type.isNotBlank()) {
                val action = Action(
                    type = type,
                    title = json.optString("title"),
                    message = json.optString("message"),
                    priority = json.optString("priority")
                )

                json.optString("value").takeIf { it.isNotEmpty() }?.let {
                    action.value = it
                }

                actions.add(action)
            }
        }

        actions
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error parsing actions: ${e.message}", e)
        emptyList()
    }
}

