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

        /**
         * ✅ FIXED: Create WorkflowEntity from simple trigger and action data
         */
        fun fromTriggersAndActions(
            workflowName: String,
            isEnabled: Boolean,
            triggers: List<Trigger>,
            actions: List<Action>,
            triggerLogic: String = "AND"
        ): WorkflowEntity? {
            return try {
                Log.d(
                    TAG,
                    "🔨 Creating WorkflowEntity with ${triggers.size} triggers and ${actions.size} actions"
                )

                // Build triggers JSON using base Trigger properties
                val triggersJson = JSONArray()
                triggers.forEach { trigger ->
                    val triggerObj = JSONObject().apply {
                        put("type", trigger.type)
                        put("value", trigger.value)

                        // Parse additional fields from value JSON if needed
                        if (trigger.value.startsWith("{")) {
                            try {
                                val valueJson = JSONObject(trigger.value)
                                val keys = valueJson.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    put(key, valueJson.get(key))
                                }
                            } catch (e: Exception) {
                                Log.w(
                                    TAG,
                                    "Could not parse trigger value as JSON: ${trigger.value}"
                                )
                            }
                        }
                    }
                    triggersJson.put(triggerObj)
                }

                // Build actions JSON
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

                val entity = WorkflowEntity(
                    workflowName = workflowName,
                    triggerDetails = triggersJson.toString(),
                    actionDetails = actionsJson.toString(),
                    isEnabled = isEnabled,
                    triggerLogic = triggerLogic,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                Log.d(TAG, "✅ WorkflowEntity created successfully")
                Log.d(TAG, " Triggers JSON: ${entity.triggerDetails}")
                Log.d(TAG, " Actions JSON: ${entity.actionDetails}")
                entity  // ← ONLY ONE RETURN STATEMENT NEEDED HERE

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating WorkflowEntity", e)
                return null  // ← AND ONE IN THE CATCH BLOCK
            }
        }
    }
}
