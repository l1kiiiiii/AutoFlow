package com.example.autoflow.data

import android.util.Log
import com.example.autoflow.domain.model.Action
import com.example.autoflow.domain.model.Trigger
import org.json.JSONArray


private const val TAG = "WorkflowEntityExt"

/**
 * Convert WorkflowEntity to list of Actions
 */
fun WorkflowEntity.toActions(): List<Action> {
    return try {
        if (actionDetails.isBlank()) {
            Log.e(TAG, "Empty action details")
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

                //  Set value if it exists
                json.optString("value").takeIf { it.isNotEmpty() }?.let { action.value = it }

                //   Set duration if it exists
                if (json.has("duration")) {
                    action.duration = json.optLong("duration")
                }

                actions.add(action)
            }
        }

        Log.d(TAG, "Parsed ${actions.size} actions")
        actions
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing actions: ${e.message}", e)
        emptyList()
    }
}

/**
 * Convert WorkflowEntity to list of Triggers
 */

fun WorkflowEntity.toTriggers(): List<Trigger> {
    return try {
        if (triggerDetails.isBlank()) {
            Log.e(TAG, "Empty trigger details")
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
                "WIFI" -> Trigger.WiFiTrigger(
                    ssid = json.optString("ssid", ""),
                    state = json.optString("state", "CONNECTED")
                )
                "BLUETOOTH" -> Trigger.BluetoothTrigger(
                    deviceAddress = json.optString("deviceAddress", ""),
                    deviceName = json.optString("deviceName")
                )
                "BATTERY" -> Trigger.BatteryTrigger(
                    level = json.optInt("level", 20),
                    condition = json.optString("condition", "below")
                )
                "MANUAL" -> Trigger.ManualTrigger(
                    actionType = json.optString("value", "quick_action")
                )
                else -> {
                    Log.w(TAG, "Unknown trigger type: $type")
                    null
                }
            }

            trigger?.let { triggers.add(it) }
        }

        Log.d(TAG, "Parsed ${triggers.size} triggers")
        triggers
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing triggers: ${e.message}", e)
        emptyList()
    }
}


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
