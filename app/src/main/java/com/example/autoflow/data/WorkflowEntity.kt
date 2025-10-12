package com.example.autoflow.data

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.util.ActionExecutor
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "workflow_name")
    var workflowName: String = "",

    @ColumnInfo(name = "is_enabled")
    var isEnabled: Boolean = false,

    // ✅ CHANGED: Now stores array of triggers
    @ColumnInfo(name = "trigger_details")
    var triggerDetails: String = "[]",  // JSON array instead of single object

    // ✅ CHANGED: Now stores array of actions
    @ColumnInfo(name = "action_details")
    var actionDetails: String = "[]",   // JSON array instead of single object

    // ✅ NEW: Store trigger combination logic (AND/OR)
    @ColumnInfo(name = "trigger_logic")
    var triggerLogic: String = "AND"  // "AND" or "OR"
) {
    companion object {
        private const val TAG = "WorkflowEntity"

        /**
         * Create WorkflowEntity from multiple Triggers and Actions
         */
        fun fromTriggersAndActions(
            workflowName: String,
            isEnabled: Boolean,
            triggers: List<Trigger>,
            actions: List<Action>,
            triggerLogic: String = "AND"
        ): WorkflowEntity? {
            return try {
                Log.d(TAG, "🔨 Creating WorkflowEntity with ${triggers.size} triggers and ${actions.size} actions")

                // Build triggers JSON array
                val triggersJsonArray = JSONArray()
                triggers.forEach { trigger ->
                    val triggerJson = JSONObject().apply {
                        put("type", trigger.type)
                        put("value", trigger.value)
                        // Add trigger-specific fields based on type
                        when (trigger) {
                            is Trigger.LocationTrigger -> {
                                put("locationName", trigger.locationName)
                                put("latitude", trigger.latitude)
                                put("longitude", trigger.longitude)
                                put("radius", trigger.radius)
                                put("triggerOnEntry", trigger.triggerOnEntry)
                                put("triggerOnExit", trigger.triggerOnExit)
                                put("triggerOn", trigger.triggerOn)
                            }
                            is Trigger.TimeTrigger -> {
                                put("time", trigger.time)
                                val daysArray = JSONArray(trigger.days)
                                put("days", daysArray)
                            }
                            is Trigger.BatteryTrigger -> {
                                put("level", trigger.level)
                                put("condition", trigger.condition)
                            }
                            // ✅ FIXED: Add missing WiFi trigger case
                            is Trigger.WiFiTrigger -> {
                                trigger.ssid?.let { put("ssid", it) }
                                put("state", trigger.state)
                            }
                            // ✅ FIXED: Add missing Bluetooth trigger case
                            is Trigger.BluetoothTrigger -> {
                                put("deviceAddress", trigger.deviceAddress)
                                trigger.deviceName?.let { put("deviceName", it) }
                            }
                        }
                    }
                    triggersJsonArray.put(triggerJson)
                }

                // Build actions JSON array
                val actionsJsonArray = JSONArray()
                actions.forEach { action ->
                    val actionJson = JSONObject().apply {
                        put("type", action.type)
                        action.value?.let { put("value", it) }
                        action.title?.let { put("title", it) }
                        action.message?.let { put("message", it) }
                        action.priority?.let { put("priority", it) }
                    }
                    actionsJsonArray.put(actionJson)
                }

                val entity = WorkflowEntity(
                    workflowName = workflowName,
                    isEnabled = isEnabled,
                    triggerDetails = triggersJsonArray.toString(),
                    actionDetails = actionsJsonArray.toString(),
                    triggerLogic = triggerLogic
                )

                Log.d(TAG, "✅ WorkflowEntity created successfully")
                Log.d(TAG, "  Triggers JSON: $triggersJsonArray")
                Log.d(TAG, "  Actions JSON: $actionsJsonArray")
                entity
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating WorkflowEntity", e)
                null
            }
        }

        /**
         * Execute all actions for a workflow
         * ✅ FIXED: Added context parameter
         */
        fun executeWorkflow(context: Context, workflowEntity: WorkflowEntity): Boolean {
            val actions = workflowEntity.toActions()

            if (actions.isEmpty()) {
                Log.w(TAG, "⚠️ No actions to execute for workflow ${workflowEntity.id}")
                return false
            }

            Log.d(TAG, "🚀 Executing ${actions.size} actions for workflow: ${workflowEntity.workflowName}")

            var allSuccessful = true
            actions.forEachIndexed { index, action ->
                // ✅ FIXED: Pass context to executeAction
                val success = ActionExecutor.executeAction(context, action)
                Log.d(TAG, "Action ${index + 1}/${actions.size}: ${if (success) "✅" else "❌"}")
                if (!success) allSuccessful = false
            }

            return allSuccessful
        }
    }
}
