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
    var modeId: Long? = null, // NULL for custom workflows, ID for mode-based

    @ColumnInfo(name = "is_mode_workflow")
    var isModeWorkflow: Boolean = false
) {
    companion object {
        private const val TAG = "WorkflowEntity"

        /**
         * Create WorkflowEntity from triggers and actions
         * ✅ FIXED: No alarm scheduling here (moved to ViewModel)
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

                // Build triggers JSON
                val triggersJson = JSONArray()
                triggers.forEach { trigger ->
                    val triggerObj = JSONObject().apply {
                        put("type", trigger.type)
                        put("value", trigger.value)

                        when (trigger) {
                            is Trigger.TimeTrigger -> {
                                put("time", trigger.time)
                                put("days", JSONArray(trigger.days))
                            }
                            is Trigger.LocationTrigger -> {
                                put("locationName", trigger.locationName)
                                put("latitude", trigger.latitude)
                                put("longitude", trigger.longitude)
                                put("radius", trigger.radius)
                                put("triggerOnEntry", trigger.triggerOnEntry)
                                put("triggerOnExit", trigger.triggerOnExit)
                                put("triggerOn", trigger.triggerOn)
                            }
                            is Trigger.WiFiTrigger -> {
                                trigger.ssid?.let { put("ssid", it) }
                                put("state", trigger.state)
                            }
                            is Trigger.BluetoothTrigger -> {
                                put("deviceAddress", trigger.deviceAddress)
                                trigger.deviceName?.let { put("deviceName", it) }
                            }
                            is Trigger.BatteryTrigger -> {
                                put("level", trigger.level)
                                put("condition", trigger.condition)
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
                Log.d(TAG, "   Triggers JSON: ${entity.triggerDetails}")
                Log.d(TAG, "   Actions JSON: ${entity.actionDetails}")

                entity
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating WorkflowEntity", e)
                null
            }
        }
    }
}
