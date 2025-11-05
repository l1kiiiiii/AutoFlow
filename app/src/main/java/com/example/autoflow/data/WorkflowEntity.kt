package com.example.autoflow.data

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "workflow_name")
    var workflowName: String = "",

    @ColumnInfo(name = "trigger_details")
    var triggerDetails: String = "",

    @ColumnInfo(name = "action_details")
    var actionDetails: String = "",

    @ColumnInfo(name = "is_enabled")
    var isEnabled: Boolean = true,

    @ColumnInfo(name = "trigger_logic")
    var triggerLogic: String = "AND",

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "mode_id")
    var modeId: Long? = null,

    @ColumnInfo(name = "is_mode_workflow")
    var isModeWorkflow: Boolean = false
) {
    companion object {
        private const val TAG = "WorkflowEntity"

        fun fromTriggersAndActions(
            workflowName: String,
            isEnabled: Boolean,
            triggers: List<Trigger>,
            actions: List<Action>,
            triggerLogic: String = "AND"
        ): WorkflowEntity? {
            return try {
                Log.d(TAG, "🔨 Creating WorkflowEntity with ${triggers.size} triggers and ${actions.size} actions")

                val triggersJson = JSONArray()
                triggers.forEach { trigger ->
                    val triggerObj = JSONObject().apply {
                        put("type", trigger.type)
                        put("value", trigger.value)
                    }
                    triggersJson.put(triggerObj)
                }

                val actionsJson = JSONArray()
                actions.forEach { action ->
                    val actionObj = JSONObject().apply {
                        put("type", action.type)
                        action.value?.let { put("value", it) }
                        action.title?.let { put("title", it) }
                        action.message?.let { put("message", it) }
                        action.priority?.let { put("priority", it) }
                        action.duration?.let { put("duration", it) }
                    }
                    actionsJson.put(actionObj)
                }

                WorkflowEntity(
                    workflowName = workflowName,
                    triggerDetails = triggersJson.toString(),
                    actionDetails = actionsJson.toString(),
                    isEnabled = isEnabled,
                    triggerLogic = triggerLogic,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating WorkflowEntity", e)
                null
            }
        }
    }
}

// ✅ EXTENSION FUNCTIONS - FIXED FOR PROPER INSTANTIATION

/**
 * Convert WorkflowEntity to list of Triggers using specific trigger subclasses
 */
fun WorkflowEntity.toTriggers(): List<Trigger> {
    return try {
        val triggersList = mutableListOf<Trigger>()
        val jsonArray = JSONArray(this.triggerDetails)

        for (i in 0 until jsonArray.length()) {
            val triggerObj = jsonArray.getJSONObject(i)
            val triggerType = triggerObj.getString("type")
            val triggerValue = triggerObj.optString("value", triggerObj.toString())

            // Create appropriate trigger subclass based on type
            val trigger = when (triggerType.uppercase()) {
                "WIFI" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val ssid = if (valueJson.has("ssid")) valueJson.getString("ssid") else null
                        val state = if (valueJson.has("state")) valueJson.getString("state") else "CONNECTED"
                        Trigger.WiFiTrigger(ssid = ssid, state = state)
                    } catch (e: Exception) {
                        // Fallback for simple string values
                        Trigger.WiFiTrigger(ssid = triggerValue, state = "CONNECTED")
                    }
                }
                "BLUETOOTH" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val deviceAddress = valueJson.getString("deviceAddress")
                        val deviceName = if (valueJson.has("deviceName")) valueJson.getString("deviceName") else null
                        Trigger.BluetoothTrigger(deviceAddress = deviceAddress, deviceName = deviceName)
                    } catch (e: Exception) {
                        // Fallback for simple string values
                        Trigger.BluetoothTrigger(deviceAddress = triggerValue)
                    }
                }
                "LOCATION" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val locationName = valueJson.optString("locationName", "Unknown Location")
                        val latitude = valueJson.getDouble("latitude")
                        val longitude = valueJson.getDouble("longitude")
                        val radius = valueJson.optDouble("radius", 100.0)
                        val triggerOnEntry = valueJson.optBoolean("triggerOnEntry", true)
                        val triggerOnExit = valueJson.optBoolean("triggerOnExit", false)
                        val triggerOn = valueJson.optString("triggerOn", "both")

                        Trigger.LocationTrigger(
                            locationName = locationName,
                            latitude = latitude,
                            longitude = longitude,
                            radius = radius,
                            triggerOnEntry = triggerOnEntry,
                            triggerOnExit = triggerOnExit,
                            triggerOn = triggerOn
                        )
                    } catch (e: Exception) {
                        // Create a default location trigger if parsing fails
                        Trigger.LocationTrigger(
                            locationName = "Unknown Location",
                            latitude = 0.0,
                            longitude = 0.0,
                            radius = 100.0,
                            triggerOnEntry = true,
                            triggerOnExit = false
                        )
                    }
                }
                "TIME" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val time = valueJson.getString("time")
                        val daysJsonArray = valueJson.getJSONArray("days")
                        val days = mutableListOf<String>()
                        for (j in 0 until daysJsonArray.length()) {
                            days.add(daysJsonArray.getString(j))
                        }
                        Trigger.TimeTrigger(time = time, days = days)
                    } catch (e: Exception) {
                        // Fallback for simple time values
                        Trigger.TimeTrigger(time = triggerValue, days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"))
                    }
                }
                "BATTERY" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val level = valueJson.getInt("level")
                        val condition = valueJson.getString("condition")
                        Trigger.BatteryTrigger(level = level, condition = condition)
                    } catch (e: Exception) {
                        // Fallback for simple battery level values
                        val level = triggerValue.toIntOrNull() ?: 20
                        Trigger.BatteryTrigger(level = level, condition = "below")
                    }
                }
                "MANUAL" -> {
                    Trigger.ManualTrigger(actionType = triggerValue.ifEmpty { "quick_action" })
                }
                else -> {
                    // Default fallback - create a manual trigger
                    Log.w("WorkflowEntity", "⚠️ Unknown trigger type: $triggerType, creating manual trigger")
                    Trigger.ManualTrigger(actionType = "unknown_$triggerType")
                }
            }

            triggersList.add(trigger)
        }

        Log.d("WorkflowEntity", "✅ Converted to ${triggersList.size} triggers")
        triggersList
    } catch (e: Exception) {
        Log.e("WorkflowEntity", "❌ Error parsing triggers from: $triggerDetails", e)
        emptyList()
    }
}

/**
 * Convert WorkflowEntity to list of Actions using proper constructor
 */
fun WorkflowEntity.toActions(): List<Action> {
    return try {
        val actionsList = mutableListOf<Action>()
        val jsonArray = JSONArray(this.actionDetails)

        for (i in 0 until jsonArray.length()) {
            val actionObj = jsonArray.getJSONObject(i)

            // Extract all possible values
            val type = actionObj.optString("type", null)
            val value = actionObj.optString("value", null).takeIf { !it.isNullOrEmpty() }
            val title = actionObj.optString("title", null).takeIf { !it.isNullOrEmpty() }
            val message = actionObj.optString("message", null).takeIf { !it.isNullOrEmpty() }
            val priority = actionObj.optString("priority", null).takeIf { !it.isNullOrEmpty() }
            val duration = actionObj.optLong("duration", 0).takeIf { it > 0 }

            // Create action using the most appropriate constructor
            val action = when {
                // Full constructor - when we have notification-specific fields
                title != null && message != null && priority != null -> {
                    Action(type, title, message, priority, value, duration, null)
                }
                // Constructor with value and duration
                value != null && duration != null -> {
                    Action(type, value, duration)
                }
                // Constructor with just value
                value != null -> {
                    Action(type, value)
                }
                // Simple constructor with just type
                else -> {
                    Action(type)
                }
            }

            actionsList.add(action)
        }

        Log.d("WorkflowEntity", "✅ Converted to ${actionsList.size} actions")
        actionsList
    } catch (e: Exception) {
        Log.e("WorkflowEntity", "❌ Error parsing actions from: $actionDetails", e)
        emptyList()
    }
}

/**
 * Check if workflow has specific trigger type
 */
fun WorkflowEntity.hasTriggerType(triggerType: String): Boolean {
    return try {
        val jsonArray = JSONArray(this.triggerDetails)
        for (i in 0 until jsonArray.length()) {
            val triggerObj = jsonArray.getJSONObject(i)
            if (triggerObj.getString("type").equals(triggerType, ignoreCase = true)) {
                return true
            }
        }
        false
    } catch (e: Exception) {
        Log.e("WorkflowEntity", "❌ Error checking trigger type", e)
        false
    }
}

/**
 * Check if workflow has specific action type
 */
fun WorkflowEntity.hasActionType(actionType: String): Boolean {
    return try {
        val jsonArray = JSONArray(this.actionDetails)
        for (i in 0 until jsonArray.length()) {
            val actionObj = jsonArray.getJSONObject(i)
            if (actionObj.getString("type").equals(actionType, ignoreCase = true)) {
                return true
            }
        }
        false
    } catch (e: Exception) {
        Log.e("WorkflowEntity", "❌ Error checking action type", e)
        false
    }
}

/**
 * Get summary of triggers for display
 */
fun WorkflowEntity.getTriggerSummary(): String {
    return try {
        val triggersList = this.toTriggers()
        when {
            triggersList.isEmpty() -> "No triggers"
            triggersList.size == 1 -> triggersList.first().type
            else -> "${triggersList.first().type} + ${triggersList.size - 1} more"
        }
    } catch (e: Exception) {
        "Unknown triggers"
    }
}

/**
 * Get summary of actions for display
 */
fun WorkflowEntity.getActionSummary(): String {
    return try {
        val actionsList = this.toActions()
        when {
            actionsList.isEmpty() -> "No actions"
            actionsList.size == 1 -> actionsList.first().type ?: "Unknown"
            else -> "${actionsList.first().type ?: "Unknown"} + ${actionsList.size - 1} more"
        }
    } catch (e: Exception) {
        "Unknown actions"
    }
}
