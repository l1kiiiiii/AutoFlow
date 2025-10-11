package com.example.autoflow.data

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import org.json.JSONObject

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "workflow_name")
    var workflowName: String = "",

    @ColumnInfo(name = "is_enabled")
    var isEnabled: Boolean = false,

    @ColumnInfo(name = "trigger_details")
    var triggerDetails: String = "",

    @ColumnInfo(name = "action_details")
    var actionDetails: String = ""
) {
    companion object {
        private const val TAG = "WorkflowEntity"

        /**
         * Create WorkflowEntity from Trigger and Action models
         */
        fun fromTriggerAndAction(
            workflowName: String,
            isEnabled: Boolean,
            trigger: Trigger,
            action: Action
        ): WorkflowEntity? {
            return try {
                Log.d(TAG, "🔨 Creating WorkflowEntity")
                Log.d(TAG, "  Name: $workflowName")
                Log.d(TAG, "  Trigger: ${trigger.type} = ${trigger.value}")
                Log.d(TAG, "  Action: ${action.type}")

                // Build trigger JSON
                val triggerJson = JSONObject().apply {
                    put("type", trigger.type)
                    put("value", trigger.value)
                    // ✅ REMOVED: trigger.details doesn't exist
                }

                // Build action JSON
                val actionJson = JSONObject().apply {
                    put("type", action.type)
                    action.value?.let { put("value", it) }
                    action.title?.let { put("title", it) }
                    action.message?.let { put("message", it) }
                    action.priority?.let { put("priority", it) }
                }

                val entity = WorkflowEntity(
                    workflowName = workflowName,
                    isEnabled = isEnabled,
                    triggerDetails = triggerJson.toString(),
                    actionDetails = actionJson.toString()
                )

                Log.d(TAG, "✅ WorkflowEntity created successfully")
                Log.d(TAG, "  Trigger JSON: $triggerJson")
                Log.d(TAG, "  Action JSON: $actionJson")

                entity
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating WorkflowEntity", e)
                null
            }
        }
    }
}
