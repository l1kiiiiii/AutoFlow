package com.example.autoflow.data.mapper

import android.util.Log
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.domain.model.Action
import com.example.autoflow.domain.model.Trigger
import com.example.autoflow.domain.model.Workflow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mapper to convert between domain Workflow and data WorkflowEntity
 */
object WorkflowMapper {
    private const val TAG = "WorkflowMapper"
    
    /**
     * Convert domain Workflow to data WorkflowEntity
     */
    fun toEntity(workflow: Workflow): WorkflowEntity {
        // Build triggers JSON
        val triggersJsonArray = JSONArray()
        workflow.triggers.forEach { trigger ->
            val triggerJson = JSONObject().apply {
                put("type", trigger.type)
                put("value", trigger.value)
            }
            triggersJsonArray.put(triggerJson)
        }
        
        // Build actions JSON
        val actionsJsonArray = JSONArray()
        workflow.actions.forEach { action ->
            val actionJson = JSONObject().apply {
                put("type", action.type)
                action.value?.let { put("value", it) }
                action.title?.let { put("title", it) }
                action.message?.let { put("message", it) }
                action.priority?.let { put("priority", it) }
                action.duration?.let { put("duration", it) }
                action.scheduledUnblockTime?.let { put("scheduledUnblockTime", it) }
            }
            actionsJsonArray.put(actionJson)
        }
        
        return WorkflowEntity(
            id = workflow.id,
            workflowName = workflow.workflowName,
            triggerDetails = triggersJsonArray.toString(),
            actionDetails = actionsJsonArray.toString(),
            isEnabled = workflow.isEnabled,
            triggerLogic = workflow.triggerLogic,
            createdAt = workflow.createdAt,
            updatedAt = workflow.updatedAt,
            modeId = workflow.modeId,
            isModeWorkflow = workflow.isModeWorkflow
        )
    }
    
    /**
     * Convert data WorkflowEntity to domain Workflow
     */
    fun toDomain(entity: WorkflowEntity): Workflow {
        val triggers = parseTriggers(entity.triggerDetails)
        val actions = parseActions(entity.actionDetails)
        
        return Workflow(
            id = entity.id,
            workflowName = entity.workflowName,
            triggers = triggers,
            actions = actions,
            triggerLogic = entity.triggerLogic,
            isEnabled = entity.isEnabled,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            modeId = entity.modeId,
            isModeWorkflow = entity.isModeWorkflow
        )
    }
    
    /**
     * Parse triggers from JSON string
     */
    private fun parseTriggers(triggerDetails: String): List<Trigger> {
        val triggers = mutableListOf<Trigger>()
        try {
            val jsonArray = JSONArray(triggerDetails)
            for (i in 0 until jsonArray.length()) {
                val triggerObj = jsonArray.getJSONObject(i)
                val type = triggerObj.getString("type")
                val value = triggerObj.getString("value")
                
                triggers.add(createTriggerFromJson(type, value, triggerObj))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing triggers", e)
        }
        return triggers
    }
    
    /**
     * Create specific Trigger type from JSON
     */
    private fun createTriggerFromJson(type: String, value: String, json: JSONObject): Trigger {
        return when (type) {
            "LOCATION" -> {
                val locationName = json.optString("locationName", "Location")
                val lat = json.optDouble("latitude", 0.0)
                val lng = json.optDouble("longitude", 0.0)
                val radius = json.optDouble("radius", 100.0)
                val triggerOnEntry = json.optBoolean("triggerOnEntry", true)
                val triggerOnExit = json.optBoolean("triggerOnExit", false)
                val triggerOn = json.optString("triggerOn", "both")
                
                Trigger.LocationTrigger(locationName, lat, lng, radius, triggerOnEntry, triggerOnExit, triggerOn)
            }
            "WIFI" -> {
                val ssid = json.optString("ssid", null)
                val state = json.optString("state", "CONNECTED")
                Trigger.WiFiTrigger(ssid, state)
            }
            "BLUETOOTH" -> {
                val deviceAddress = json.optString("deviceAddress", "")
                val deviceName = json.optString("deviceName", null)
                Trigger.BluetoothTrigger(deviceAddress, deviceName)
            }
            "TIME" -> {
                val time = json.optString("time", "")
                val daysArray = json.optJSONArray("days")
                val days = mutableListOf<String>()
                if (daysArray != null) {
                    for (i in 0 until daysArray.length()) {
                        days.add(daysArray.getString(i))
                    }
                }
                Trigger.TimeTrigger(time, days)
            }
            "BATTERY" -> {
                val level = json.optInt("level", 50)
                val condition = json.optString("condition", "below")
                Trigger.BatteryTrigger(level, condition)
            }
            "MANUAL" -> {
                val actionType = json.optString("value", "quick_action")
                Trigger.ManualTrigger(actionType)
            }
            else -> Trigger.ManualTrigger("unknown")
        }
    }
    
    /**
     * Parse actions from JSON string
     */
    private fun parseActions(actionDetails: String): List<Action> {
        val actions = mutableListOf<Action>()
        try {
            val jsonArray = JSONArray(actionDetails)
            for (i in 0 until jsonArray.length()) {
                val actionObj = jsonArray.getJSONObject(i)
                val type = actionObj.optString("type", null)
                val value = actionObj.optString("value", null)
                val title = actionObj.optString("title", null)
                val message = actionObj.optString("message", null)
                val priority = actionObj.optString("priority", "Normal")
                val duration = if (actionObj.has("duration")) actionObj.getLong("duration") else null
                val scheduledUnblockTime = if (actionObj.has("scheduledUnblockTime")) 
                    actionObj.getLong("scheduledUnblockTime") else null
                
                actions.add(Action(type, title, message, priority, value, duration, scheduledUnblockTime))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing actions", e)
        }
        return actions
    }
}
