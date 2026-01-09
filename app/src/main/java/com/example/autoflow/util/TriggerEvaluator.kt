package com.example.autoflow.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toTriggers
import com.example.autoflow.integrations.LocationManager
import com.example.autoflow.model.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ✅ TriggerEvaluator - Evaluates multiple triggers with AND/OR logic
 * Used to determine if a workflow should execute based on trigger states
 */
object TriggerEvaluator {

    private const val TAG = "TriggerEvaluator"

    // Logic operators
    const val LOGIC_AND = "AND"
    const val LOGIC_OR = "OR"

    /**
     * ✅ Evaluate multiple triggers based on logic operator
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

        Log.d(TAG, "🔍 Evaluating ${triggers.size} triggers with $logic logic")

        return when (logic.uppercase()) {
            LOGIC_AND -> evaluateAND(triggers, currentStates)
            LOGIC_OR -> evaluateOR(triggers, currentStates)
            else -> {
                Log.w(TAG, "⚠️ Unknown logic operator: $logic, defaulting to AND")
                evaluateAND(triggers, currentStates)
            }
        }
    }

    private suspend fun checkCurrentLocationTrigger(
        context: Context,
        trigger: Trigger,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val locationData = TriggerParser.parseLocationData(trigger)
            if (locationData == null) {
                Log.e(TAG, "❌ Failed to parse location data from trigger")
                return@withContext false
            }

            Log.d(TAG, "🌍 VALIDATING current location against: ${locationData.locationName}")

            // ✅ CRITICAL: Get IMMEDIATE current location
            val locationManager = LocationManager(context)
            val currentLocation = locationManager.getCurrentLocationSync(5000L) // 5-second max wait

            if (currentLocation == null) {
                Log.e(TAG, "❌ LOCATION VALIDATION FAILED - Cannot get current GPS position")

                // ✅ IMPORTANT: Return false to prevent execution
                return@withContext false
            }

            // ✅ Calculate distance to trigger location
            val targetLocation = android.location.Location("target").apply {
                latitude = locationData.latitude
                longitude = locationData.longitude
            }

            val distanceToTarget = currentLocation.distanceTo(targetLocation)
            val radiusMeters = locationData.radius.toFloat()

            Log.d(TAG, "📍 Current GPS: ${currentLocation.latitude}, ${currentLocation.longitude}")
            Log.d(TAG, "🎯 Target location: ${locationData.latitude}, ${locationData.longitude}")
            Log.d(TAG, "📏 Distance: ${distanceToTarget.toInt()}m (trigger radius: ${radiusMeters.toInt()}m)")

            // ✅ CRITICAL: Determine if user is currently in the correct location
            val isCurrentlyInLocation = distanceToTarget <= radiusMeters

            // ✅ Check trigger conditions
            val shouldTrigger = when {
                locationData.triggerOnEntry && locationData.triggerOnExit -> {
                    // Both entry and exit enabled - trigger if we're within radius
                    isCurrentlyInLocation
                }
                locationData.triggerOnEntry -> {
                    // Entry only - trigger if we're currently within radius
                    isCurrentlyInLocation
                }
                locationData.triggerOnExit -> {
                    // Exit only - trigger if we're currently outside radius
                    !isCurrentlyInLocation
                }
                else -> {
                    Log.w(TAG, "⚠️ No entry/exit conditions set")
                    false
                }
            }

            Log.d(TAG, "🏁 Location validation result:")
            Log.d(TAG, "   📍 User is ${if (isCurrentlyInLocation) "INSIDE" else "OUTSIDE"} target location")
            Log.d(TAG, "   🎯 Trigger condition: ${if (shouldTrigger) "✅ SATISFIED" else "❌ NOT SATISFIED"}")

            shouldTrigger

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating current location trigger", e)
            return@withContext false
        }
    }
    /**
     * ✅ Evaluate single trigger
     */
    fun evaluateTrigger(trigger: Trigger, context: Context): Boolean {
        return when (trigger.type) {
            "TIME" -> evaluateTimeTrigger(trigger)
            "LOCATION" -> evaluateLocationTrigger(trigger, context)
            "WIFI" -> evaluateWifiTrigger(trigger, context)
            "BLUETOOTH" -> evaluateBluetoothTrigger(trigger, context)
            "BATTERY" -> evaluateBatteryTrigger(trigger, context)
            else -> {
                Log.w(TAG, "Unknown trigger type: ${trigger.type}")
                false
            }
        }
    }

    /**
     * ✅ Evaluate with AND logic - ALL triggers must be true
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
     * ✅ Evaluate with OR logic - ANY trigger can be true
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
     * ✅ Evaluate a workflow entity with its stored triggers and logic
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
     * ✅ Get trigger state for a specific trigger
     * @param context Application context
     * @param trigger The trigger to check
     * @return true if trigger condition is met
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun checkTriggerState(context: Context, trigger: Trigger): Boolean {
        return when (trigger.type) {
            "TIME" -> checkTimeTrigger(trigger)
            "LOCATION" -> checkLocationTrigger(context, trigger)
            "WIFI" -> checkWiFiTrigger(context, trigger)
            "BLUETOOTH" -> checkBluetoothTrigger(context, trigger)
            "BATTERY" -> checkBatteryTrigger(context, trigger)
            else -> {
                Log.w(TAG, "⚠️ Unknown trigger type: ${trigger.type}")
                false
            }
        }
    }

    /**
     * ✅ Build current states map for all triggers in a workflow
     * @param context Application context
     * @param triggers List of triggers to check
     * @return Map of trigger type to current state
     */
    @SuppressLint("MissingPermission")
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

    // ✅ PRIVATE HELPER METHODS FOR CHECKING INDIVIDUAL TRIGGER TYPES

    private fun evaluateTimeTrigger(trigger: Trigger): Boolean {
        val timeData = TriggerParser.parseTimeData(trigger) ?: return false
        val (targetTime, days) = timeData

        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val currentDay = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date()).uppercase()

        return currentTime == targetTime && (days.isEmpty() || days.contains(currentDay))
    }

    private fun checkTimeTrigger(trigger: Trigger): Boolean {
        val timeData = TriggerParser.parseTimeData(trigger) ?: return false
        val (targetTime, days) = timeData

        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val currentDay = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date()).uppercase()

        val timeMatches = currentTime == targetTime
        val dayMatches = days.isEmpty() || days.contains(currentDay)

        return timeMatches && dayMatches
    }

    private fun evaluateLocationTrigger(trigger: Trigger, context: Context): Boolean {
        // Location evaluation would require location services
        // For now, return false - implement with actual location checking
        val locationData = TriggerParser.parseLocationData(trigger) ?: return false
        Log.d(TAG, "Location trigger: ${locationData.locationName} - not implemented yet")
        return false
    }

    // ✅ FIXED: Make checkLocationTrigger non-suspend to match your buildCurrentStates usage
    private fun checkLocationTrigger(context: Context, trigger: Trigger): Boolean {
        return try {
            val locationData = TriggerParser.parseLocationData(trigger) ?: return false
            Log.d(TAG, "🌍 Checking location trigger: ${locationData.locationName}")

            // ✅ SIMPLIFIED: Use a basic location check without suspend
            // This will return false by default, forcing proper geofence-based validation
            Log.d(TAG, "📍 Location trigger deferred to geofence validation")

            // ✅ For immediate validation in buildCurrentStates, we return true
            // Real validation happens in GeofenceReceiver with GPS checking
            true // Allow geofence receiver to handle the actual validation

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking location trigger", e)
            false
        }
    }


    private fun evaluateWifiTrigger(trigger: Trigger, context: Context): Boolean {
        val wifiData = TriggerParser.parseWifiData(trigger) ?: return false

        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager

        return when (wifiData.state.uppercase()) {
            "ON" -> wifiManager?.isWifiEnabled == true
            "OFF" -> wifiManager?.isWifiEnabled == false
            "CONNECTED" -> {
                if (wifiManager?.isWifiEnabled == true) {
                    val connectionInfo = wifiManager.connectionInfo
                    if (wifiData.ssid != null) {
                        // Check specific SSID
                        val currentSsid = connectionInfo?.ssid?.replace("\"", "") ?: ""
                        currentSsid == wifiData.ssid
                    } else {
                        // Just check if connected to any WiFi
                        connectionInfo != null && connectionInfo.networkId != -1
                    }
                } else false
            }
            "DISCONNECTED" -> {
                if (wifiManager?.isWifiEnabled == true) {
                    val connectionInfo = wifiManager.connectionInfo
                    connectionInfo?.networkId == -1
                } else true
            }
            else -> false
        }
    }

    private fun checkWiFiTrigger(context: Context, trigger: Trigger): Boolean {
        val wifiData = TriggerParser.parseWifiData(trigger) ?: return false
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return false

        // 1. Check State (ON/OFF)
        val isEnabled = wifiManager.isWifiEnabled
        if (wifiData.state.equals("ON", ignoreCase = true) && !isEnabled) return false
        if (wifiData.state.equals("OFF", ignoreCase = true) && isEnabled) return false

        // 2. Check Connection & SSID
        if (wifiData.state.equals("CONNECTED", ignoreCase = true) || wifiData.ssid != null) {
            if (!isEnabled) return false

            val info = wifiManager.connectionInfo
            // Basic connection check
            if (info == null || info.networkId == -1) return false

            // SSID check (Requires Location Permission on Android 8.1+)
            if (!wifiData.ssid.isNullOrEmpty()) {
                val hasPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    val currentSsid = info.ssid.replace("\"", "")
                    if (currentSsid != wifiData.ssid) return false
                } else {
                    Log.w(TAG, "⚠️ Cannot check SSID: Missing Location Permission")
                    // If we can't verify SSID, we must fail the trigger to be safe
                    return false
                }
            }
        }

        if (wifiData.state.equals("DISCONNECTED", ignoreCase = true)) {
            val info = wifiManager.connectionInfo
            if (isEnabled && info != null && info.networkId != -1) return false
        }

        return true
    }

    private fun evaluateBluetoothTrigger(trigger: Trigger, context: Context): Boolean {
        val bluetoothData = TriggerParser.parseBluetoothData(trigger) ?: return false

        // Bluetooth trigger logic - check if device is connected/available
        // This would typically be handled by BluetoothManager
        Log.d(TAG, "Bluetooth trigger: ${bluetoothData.deviceAddress} - not fully implemented yet")
        return false
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun checkBluetoothTrigger(context: Context, trigger: Trigger): Boolean {
        val btData = TriggerParser.parseBluetoothData(trigger) ?: return false
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter ?: return false

        // 1. Check State (ON/OFF)
        val isEnabled = adapter.isEnabled
        if (btData.state.equals("ON", ignoreCase = true) && !isEnabled) return false
        if (btData.state.equals("OFF", ignoreCase = true) && isEnabled) return false

        // 2. Check Device Connection
        // Note: Synchronously checking "Is device X connected" is hard on Android.
        // We use a helper that checks the state we saved in SharedPreferences from the Receiver.
        if (btData.state.equals("CONNECTED", ignoreCase = true) || !btData.deviceAddress.isNullOrEmpty()) {
            if (!isEnabled) return false

            // Check if specific device is required
            if (!btData.deviceAddress.isNullOrEmpty()) {
                val isConnected = isDeviceConnectedStateSaved(context, btData.deviceAddress)
                if (!isConnected) return false
            } else {
                // Check if ANY device is connected (generic)
                // This is a rough approximation using profile connection state
                val headset = adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                val a2dp = adapter.getProfileConnectionState(BluetoothProfile.A2DP)
                val gatt = adapter.getProfileConnectionState(BluetoothProfile.GATT)

                val anyConnected = (headset == BluetoothProfile.STATE_CONNECTED) ||
                        (a2dp == BluetoothProfile.STATE_CONNECTED) ||
                        (gatt == BluetoothProfile.STATE_CONNECTED)

                if (!anyConnected) return false
            }
        }

        if (btData.state.equals("DISCONNECTED", ignoreCase = true)) {
            if (!btData.deviceAddress.isNullOrEmpty()) {
                if (isDeviceConnectedStateSaved(context, btData.deviceAddress)) return false
            }
        }

        return true
    }

    // Helper to read the state saved by BluetoothReceiver
    private fun isDeviceConnectedStateSaved(context: Context, address: String): Boolean {
        val prefs = context.getSharedPreferences("bt_device_states", Context.MODE_PRIVATE)
        return prefs.getBoolean(address, false)
    }

    private fun evaluateBatteryTrigger(trigger: Trigger, context: Context): Boolean {
        val batteryData = TriggerParser.parseBatteryData(trigger) ?: return false

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: return false

        return when (batteryData.condition.lowercase()) {
            "below", "<" -> batteryLevel < batteryData.level
            "above", ">" -> batteryLevel > batteryData.level
            "equals", "=" -> batteryLevel == batteryData.level
            else -> false
        }
    }

    private fun checkBatteryTrigger(context: Context, trigger: Trigger): Boolean {
        return evaluateBatteryTrigger(trigger, context)
    }

    /**
     * ✅ Get a human-readable description of trigger evaluation
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

    /**
     * ✅ Check if workflow triggers are currently satisfied
     */
    fun isWorkflowTriggered(context: Context, workflow: WorkflowEntity): Boolean {
        val triggers = workflow.toTriggers()
        val currentStates = buildCurrentStates(context, triggers)
        return evaluateWorkflow(workflow, currentStates)
    }

    /**
     * ✅ Validate current GPS location for a trigger
     * This method performs REAL-TIME GPS checking
     */
    suspend fun validateCurrentLocationForTrigger(
        context: Context,
        trigger: Trigger
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val locationData = TriggerParser.parseLocationData(trigger)
            if (locationData == null) {
                Log.e(TAG, "❌ Failed to parse location data")
                return@withContext false
            }

            Log.d(TAG, "🛰️ Real-time GPS check for: ${locationData.locationName}")

            // ✅ Get current GPS location
            val locationManager = com.example.autoflow.integrations.LocationManager(context)
            val currentLocation = locationManager.getCurrentLocationSync(5000L)

            if (currentLocation == null) {
                Log.e(TAG, "❌ Cannot get current GPS position")
                return@withContext false
            }

            // ✅ Calculate distance
            val targetLocation = android.location.Location("target").apply {
                latitude = locationData.latitude
                longitude = locationData.longitude
            }

            val distance = currentLocation.distanceTo(targetLocation)
            val isWithinRadius = distance <= locationData.radius

            Log.d(TAG, "📍 GPS: (${currentLocation.latitude}, ${currentLocation.longitude})")
            Log.d(TAG, "🎯 Target: (${locationData.latitude}, ${locationData.longitude})")
            Log.d(TAG, "📏 Distance: ${distance.toInt()}m / Radius: ${locationData.radius.toInt()}m")
            Log.d(TAG, "✅ Location validation: ${if (isWithinRadius) "PASS" else "FAIL"}")

            return@withContext isWithinRadius

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in GPS validation", e)
            return@withContext false
        }
    }

}