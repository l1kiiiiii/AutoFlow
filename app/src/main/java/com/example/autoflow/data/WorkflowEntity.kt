package com.example.autoflow.data

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.util.Constants
import org.json.JSONObject

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,  // ✅ Kotlin Long (not java.lang.Long)

    @ColumnInfo(name = "workflow_name")
    var workflowName: String = "",  // ✅ Direct property, no getter/setter

    @ColumnInfo(name = "is_enabled")
    var isEnabled: Boolean = false,

    @ColumnInfo(name = "trigger_details")
    var triggerDetails: String = "",

    @ColumnInfo(name = "action_details")
    var actionDetails: String = ""
) {

    companion object {
        private const val TAG = "WorkflowEntity"

        fun fromTriggerAndAction(
            workflowName: String,
            isEnabled: Boolean,
            trigger: Trigger,
            action: Action
        ): WorkflowEntity? {
            return try {
                Log.d(TAG, "🔵 fromTriggerAndAction called")

                // Convert Trigger to JSON
                val triggerJson = JSONObject().apply {
                    put("type", trigger.type)
                    put("value", trigger.value)
                }

                // Convert Action to JSON
                val actionJson = JSONObject().apply {
                    put("type", action.type)
                    action.title?.let { put("title", it) }
                    action.message?.let { put("message", it) }
                    action.priority?.let { put("priority", it) }
                    action.value?.let { put("value", it) }
                }

                WorkflowEntity(
                    workflowName = workflowName,
                    isEnabled = isEnabled,
                    triggerDetails = triggerJson.toString(),
                    actionDetails = actionJson.toString()
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating WorkflowEntity", e)
                null
            }
        }
    }

    // Helper methods
    fun toTrigger(): Trigger? {
        if (triggerDetails.isBlank()) return null

        return try {
            val json = JSONObject(triggerDetails)
            val type = json.optString("type")

            if (type.isBlank()) {
                Log.e(TAG, "Trigger type is empty")
                return null
            }

            Trigger(
                id = json.optLong("id", 0L),
                workflowId = json.optLong("workflowId", 0L),
                type = type.trim(),
                value = json.optString("value", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing trigger", e)
            null
        }
    }

    fun toAction(): Action? {
        if (actionDetails.isBlank()) return null

        return try {
            val json = JSONObject(actionDetails)
            val type = json.optString("type")

            if (type.isBlank()) {
                Log.e(TAG, "Action type is empty")
                return null
            }

            when (type.trim()) {
                Constants.ACTION_SEND_NOTIFICATION -> Action(
                    type = type,
                    title = json.optString("title", "AutoFlow"),
                    message = json.optString("message", ""),
                    priority = json.optString("priority", "Normal")
                )
                else -> Action(type).apply {
                    value = json.optString("value", "")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing action", e)
            null
        }
    }
}
