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

// ✅ EXTENSION FUNCTIONS - ONLY DEFINE THESE ONCE HERE

/**
 * Convert WorkflowEntity to list of Triggers
 */
fun WorkflowEntity.toTriggers(): List<Trigger> {
    return try {
        val triggersList = mutableListOf<Trigger>()
        val jsonArray = JSONArray(this.triggerDetails)

        for (i in 0 until jsonArray.length()) {
            val triggerObj = jsonArray.getJSONObject(i)
            val trigger = Trigger(
                type = triggerObj.getString("type"),
                value = triggerObj.optString("value", triggerObj.toString())
            )
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
 * Convert WorkflowEntity to list of Actions
 */
fun WorkflowEntity.toActions(): List<Action> {
    return try {
        val actionsList = mutableListOf<Action>()
        val jsonArray = JSONArray(this.actionDetails)

        for (i in 0 until jsonArray.length()) {
            val actionObj = jsonArray.getJSONObject(i)
            val action = Action(
                type = actionObj.getString("type"),
                value = actionObj.optString("value", null),
                title = actionObj.optString("title", null),
                message = actionObj.optString("message", null),
                priority = actionObj.optString("priority", null),
                duration = actionObj.optLong("duration", 0).takeIf { it > 0 }
            )
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
            if (triggerObj.getString("type") == triggerType) {
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
            if (actionObj.getString("type") == actionType) {
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
            actionsList.size == 1 -> actionsList.first().type
            else -> "${actionsList.first().type} + ${actionsList.size - 1} more"
        }
    } catch (e: Exception) {
        "Unknown actions"
    }
}
