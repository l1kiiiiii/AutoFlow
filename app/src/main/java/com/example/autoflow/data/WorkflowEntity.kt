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

// ✅ EXTENSION FUNCTIONS - FIXED NULL SAFETY AND CONSTRUCTOR

fun WorkflowEntity.toTriggers(): List<Trigger> {
    return try {
        val triggersList = mutableListOf<Trigger>()
        val jsonArray = JSONArray(this.triggerDetails)

        for (i in 0 until jsonArray.length()) {
            val triggerObj = jsonArray.getJSONObject(i)
            val triggerType = triggerObj.getString("type")
            val triggerValue = triggerObj.optString("value", "")

            val trigger = when (triggerType.uppercase()) {
                "WIFI" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val ssid = valueJson.optString("ssid", "")
                        val state = valueJson.optString("state", "CONNECTED")
                        Trigger.WiFiTrigger(ssid = ssid, state = state)
                    } catch (e: Exception) {
                        Trigger.WiFiTrigger(ssid = triggerValue, state = "CONNECTED")
                    }
                }
                "BLUETOOTH" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val deviceAddress = valueJson.optString("deviceAddress", "")
                        val deviceName = valueJson.optString("deviceName", "")
                        Trigger.BluetoothTrigger(deviceAddress = deviceAddress, deviceName = deviceName)
                    } catch (e: Exception) {
                        Trigger.BluetoothTrigger(deviceAddress = triggerValue)
                    }
                }
                "LOCATION" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val locationName = valueJson.optString("locationName", "Unknown Location")
                        val latitude = valueJson.optDouble("latitude", 0.0)
                        val longitude = valueJson.optDouble("longitude", 0.0)
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
                        Trigger.LocationTrigger(
                            locationName = "Unknown",
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
                        val time = valueJson.optString("time", "00:00")
                        val daysJsonArray = valueJson.optJSONArray("days")
                        val days = mutableListOf<String>()
                        if (daysJsonArray != null) {
                            for (j in 0 until daysJsonArray.length()) {
                                days.add(daysJsonArray.getString(j))
                            }
                        }
                        Trigger.TimeTrigger(time = time, days = days)
                    } catch (e: Exception) {
                        Trigger.TimeTrigger(time = triggerValue, days = emptyList())
                    }
                }
                "BATTERY" -> {
                    try {
                        val valueJson = JSONObject(triggerValue)
                        val level = valueJson.optInt("level", 20)
                        val condition = valueJson.optString("condition", "below")
                        Trigger.BatteryTrigger(level = level, condition = condition)
                    } catch (e: Exception) {
                        Trigger.BatteryTrigger(level = 20, condition = "below")
                    }
                }
                "MANUAL" -> {
                    Trigger.ManualTrigger(actionType = triggerValue.ifEmpty { "quick_action" })
                }
                else -> {
                    Trigger.ManualTrigger(actionType = "unknown")
                }
            }
            triggersList.add(trigger)
        }
        triggersList
    } catch (e: Exception) {
        Log.e("WorkflowEntity", "❌ Error parsing triggers", e)
        emptyList()
    }
}

fun WorkflowEntity.toActions(): List<Action> {
    return try {
        val actionsList = mutableListOf<Action>()
        val jsonArray = JSONArray(this.actionDetails)

        for (i in 0 until jsonArray.length()) {
            val actionObj = jsonArray.getJSONObject(i)
            val type = actionObj.getString("type")

            // Handle all optional fields safely
            val value = actionObj.optString("value", "")
            val title = actionObj.optString("title", "")
            val message = actionObj.optString("message", "")
            val priority = actionObj.optString("priority", "Normal")
            val duration = actionObj.optLong("duration", 0)

            // Added explicit null for scheduledUnblockTime to match signature if needed
            val action = Action(
                type = type,
                title = title,
                message = message,
                priority = priority,
                value = value,
                duration = duration,
                scheduledUnblockTime = null
            )
            actionsList.add(action)
        }
        actionsList
    } catch (e: Exception) {
        Log.e("WorkflowEntity", "❌ Error parsing actions", e)
        emptyList()
    }
}