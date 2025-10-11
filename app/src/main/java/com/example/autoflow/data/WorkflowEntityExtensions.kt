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
 * Convert WorkflowEntity to Trigger object
 * Returns the appropriate sealed class subtype based on trigger type
 */
fun WorkflowEntity.toTrigger(): Trigger? {
    return try {
        if (triggerDetails.isBlank()) {
            Log.e(TAG, "❌ Empty trigger details")
            return null
        }

        val json = JSONObject(triggerDetails)
        val type = json.optString("type", "")
        val valueString = json.optString("value", "")

        if (type.isBlank() || valueString.isBlank()) {
            Log.e(TAG, "❌ Trigger type or value missing")
            return null
        }

        // Parse value JSON for specific trigger types
        val valueJson = try {
            JSONObject(valueString)
        } catch (e: Exception) {
            null
        }

        // Return appropriate sealed class subtype based on trigger type
        when (type) {
            "LOCATION" -> {
                if (valueJson == null) {
                    Log.e(TAG, "❌ Invalid location JSON")
                    return null
                }

                Trigger.LocationTrigger(
                    locationName = valueJson.optString("locationName", "Unknown"),
                    latitude = valueJson.optDouble("latitude", 0.0),
                    longitude = valueJson.optDouble("longitude", 0.0),
                    radius = valueJson.optDouble("radius", 100.0),
                    triggerOnEntry = valueJson.optBoolean("triggerOnEntry", true),
                    triggerOnExit = valueJson.optBoolean("triggerOnExit", false),
                    triggerOn = valueJson.optString("triggerOn", "both")
                )
            }

            "TIME" -> {
                if (valueJson == null) {
                    Log.e(TAG, "❌ Invalid time JSON")
                    return null
                }

                val daysArray = valueJson.optJSONArray("days")
                val days = mutableListOf<String>()
                if (daysArray != null) {
                    for (i in 0 until daysArray.length()) {
                        days.add(daysArray.getString(i))
                    }
                }

                Trigger.TimeTrigger(
                    time = valueJson.optString("time", ""),
                    days = days
                )
            }

            "BATTERY" -> {
                if (valueJson == null) {
                    Log.e(TAG, "❌ Invalid battery JSON")
                    return null
                }

                Trigger.BatteryTrigger(
                    level = valueJson.optInt("level", 20),
                    condition = valueJson.optString("condition", "below")
                )
            }

            else -> {
                Log.w(TAG, "⚠️ Unknown trigger type: $type, cannot create sealed class instance")
                null
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error parsing trigger: ${e.message}", e)
        null
    }
}
