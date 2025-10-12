package com.example.autoflow.util

import android.content.Context
import android.util.Log
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toTriggers
import com.example.autoflow.model.Trigger

/**
 * TriggerEvaluator - Evaluates multiple triggers with AND/OR logic
 * Used to determine if a workflow should execute based on trigger states
 */
object TriggerEvaluator {

    private const val TAG = "TriggerEvaluator"

    // Logic operators
    const val LOGIC_AND = "AND"
    const val LOGIC_OR = "OR"

    /**
     * Evaluate multiple triggers based on logic operator
     * @param triggers List of triggers to evaluate
     * @param logic Logic operator ("AND" or "OR")
     * @param currentStates Map of trigger type to current state (true = triggered)
     * @return true if workflow should execute based on trigger logic
     */
    fun evaluateTriggers(
        triggers: List<Trigger>,
        logic: String,
        currentStates: Map<String, Boolean>
    ): Boolean {
        if (triggers.isEmpty()) {
            Log.w(TAG, "⚠️ No triggers to evaluate")
            return false
        }

        Log.d(TAG, "🔍 Evaluating ${triggers.size} triggers with logic: $logic")
        Log.d(TAG, "Current states: $currentStates")

        return when (logic.uppercase()) {
            LOGIC_AND -> evaluateAND(triggers, currentStates)
            LOGIC_OR -> evaluateOR(triggers, currentStates)
            else -> {
                Log.e(TAG, "❌ Unknown logic operator: $logic")
                false
            }
        }
    }

    /**
     * Evaluate with AND logic - ALL triggers must be true
     */
    private fun evaluateAND(
        triggers: List<Trigger>,
        currentStates: Map<String, Boolean>
    ): Boolean {
        val result = triggers.all { trigger ->
            val state = currentStates[trigger.type] ?: false
            Log.d(TAG, "  Trigger ${trigger.type}: $state")
            state
        }

        Log.d(TAG, "AND evaluation result: $result")
        return result
    }

    /**
     * Evaluate with OR logic - ANY trigger can be true
     */
    private fun evaluateOR(
        triggers: List<Trigger>,
        currentStates: Map<String, Boolean>
    ): Boolean {
        val result = triggers.any { trigger ->
            val state = currentStates[trigger.type] ?: false
            Log.d(TAG, "  Trigger ${trigger.type}: $state")
            state
        }

        Log.d(TAG, "OR evaluation result: $result")
        return result
    }

    /**
     * Evaluate a workflow entity with its stored triggers and logic
     * @param workflow The workflow entity containing triggers
     * @param currentStates Map of current trigger states
     * @return true if workflow should execute
     */
    fun evaluateWorkflow(
        workflow: WorkflowEntity,
        currentStates: Map<String, Boolean>
    ): Boolean {
        if (!workflow.isEnabled) {
            Log.d(TAG, "⚠️ Workflow ${workflow.id} is disabled")
            return false
        }

        val triggers = workflow.toTriggers()
        if (triggers.isEmpty()) {
            Log.w(TAG, "⚠️ Workflow ${workflow.id} has no triggers")
            return false
        }

        val logic = workflow.triggerLogic
        return evaluateTriggers(triggers, logic, currentStates)
    }

    /**
     * Get trigger state for a specific trigger type
     * @param context Application context
     * @param trigger The trigger to check
     * @return true if trigger condition is met
     */
    fun checkTriggerState(context: Context, trigger: Trigger): Boolean {
        return when (trigger) {
            is Trigger.TimeTrigger -> checkTimeTrigger(trigger)
            is Trigger.LocationTrigger -> checkLocationTrigger(context, trigger)
            is Trigger.WiFiTrigger -> checkWiFiTrigger(context, trigger)
            is Trigger.BluetoothTrigger -> checkBluetoothTrigger(context, trigger)
            is Trigger.BatteryTrigger -> checkBatteryTrigger(context, trigger)
            else -> {
                Log.w(TAG, "⚠️ Unknown trigger type: ${trigger.type}")
                false
            }
        }
    }

    /**
     * Build current states map for all triggers in a workflow
     * @param context Application context
     * @param triggers List of triggers to check
     * @return Map of trigger type to current state
     */
    fun buildCurrentStates(
        context: Context,
        triggers: List<Trigger>
    ): Map<String, Boolean> {
        val states = mutableMapOf<String, Boolean>()

        triggers.forEach { trigger ->
            val state = checkTriggerState(context, trigger)
            states[trigger.type] = state
            Log.d(TAG, "Trigger ${trigger.type} state: $state")
        }

        return states
    }

    // Helper methods for checking individual trigger types

    private fun checkTimeTrigger(trigger: Trigger.TimeTrigger): Boolean {
        // Time trigger logic - check if current time matches
        val currentTime = System.currentTimeMillis()
        val targetTime = trigger.time.toLongOrNull() ?: return false

        // Check if within time window (e.g., 1 minute)
        val diff = Math.abs(currentTime - targetTime)
        return diff <= Constants.TIME_WINDOW_MS
    }

    private fun checkLocationTrigger(
        context: Context,
        trigger: Trigger.LocationTrigger
    ): Boolean {
        // Location trigger logic - check if in geofence
        // This would typically be handled by GeofenceManager
        // Return false here as geofences trigger via broadcast receiver
        return false
    }

    private fun checkWiFiTrigger(
        context: Context,
        trigger: Trigger.WiFiTrigger
    ): Boolean {
        // WiFi trigger logic - check WiFi state
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager

        return when (trigger.value.uppercase()) {
            Constants.WIFI_STATE_ON -> wifiManager?.isWifiEnabled == true
            Constants.WIFI_STATE_OFF -> wifiManager?.isWifiEnabled == false
            else -> false
        }
    }

    private fun checkBluetoothTrigger(
        context: Context,
        trigger: Trigger.BluetoothTrigger
    ): Boolean {
        // Bluetooth trigger logic - check if device is in range
        // This would typically be handled by BLEManager
        return false
    }

    private fun checkBatteryTrigger(
        context: Context,
        trigger: Trigger.BatteryTrigger
    ): Boolean {
        // Battery trigger logic - check battery level
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE)
                as? android.os.BatteryManager

        val currentLevel = batteryManager?.getIntProperty(
            android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
        ) ?: return false

        return when (trigger.condition) {
            "below" -> currentLevel < trigger.level
            "above" -> currentLevel > trigger.level
            "equals" -> currentLevel == trigger.level
            else -> false
        }
    }

    /**
     * Get a human-readable description of trigger evaluation
     */
    fun getEvaluationDescription(
        triggers: List<Trigger>,
        logic: String,
        currentStates: Map<String, Boolean>
    ): String {
        val triggerDescriptions = triggers.map { trigger ->
            val state = currentStates[trigger.type] ?: false
            val stateEmoji = if (state) "✅" else "❌"
            "$stateEmoji ${trigger.type}"
        }

        val operator = if (logic == LOGIC_AND) "AND" else "OR"
        return triggerDescriptions.joinToString(" $operator ")
    }
}
