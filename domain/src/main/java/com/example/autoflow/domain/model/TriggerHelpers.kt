package com.example.autoflow.domain.model


/**
 * ✅ Helper functions to create triggers easily - Fixed to use data classes
 */
object TriggerHelpers {

    /**
     * Create a time-based trigger
     */
    fun createTimeTrigger(time: String, days: List<String>): Trigger {
        return Trigger.TimeTrigger(time = time, days = days)
    }

    /**
     * Create a location-based trigger
     */
    fun createLocationTrigger(
        locationName: String,
        latitude: Double,
        longitude: Double,
        radius: Double = 100.0,
        triggerOnEntry: Boolean = true,
        triggerOnExit: Boolean = false
    ): Trigger {
        val triggerOn = when {
            triggerOnEntry && triggerOnExit -> "both"
            triggerOnEntry -> "enter"
            triggerOnExit -> "exit"
            else -> "both"
        }
        return Trigger.LocationTrigger(
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            triggerOnEntry = triggerOnEntry,
            triggerOnExit = triggerOnExit,
            triggerOn = triggerOn
        )
    }

    /**
     * Create a WiFi-based trigger
     */
    fun createWifiTrigger(ssid: String?, state: String): Trigger {
        return Trigger.WiFiTrigger(ssid = ssid, state = state)
    }

    /**
     * Create a Bluetooth-based trigger
     */
    fun createBluetoothTrigger(deviceAddress: String, deviceName: String? = null): Trigger {
        return Trigger.BluetoothTrigger(
            deviceAddress = deviceAddress,
            deviceName = deviceName
        )
    }

    /**
     * Create a battery level trigger
     */
    fun createBatteryTrigger(level: Int, condition: String): Trigger {
        return Trigger.BatteryTrigger(level = level, condition = condition)
    }

    /**
     * ✅ Create a manual trigger (for Meeting Mode, etc.)
     */
    fun createManualTrigger(actionType: String = "quick_action"): Trigger {
        return Trigger.ManualTrigger(actionType = actionType)
    }

    /**
     * ✅ Create a time range trigger (for Sleep Mode, etc.)
     */
    fun createTimeRangeTrigger(
        startTime: String = "22:00",
        endTime: String = "07:00",
        days: List<String> = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    ): TimeRangeTrigger {
        return TimeRangeTrigger(startTime = startTime, endTime = endTime, days = days)
    }

    // ✅ Parse trigger data back to individual fields

    /**
     * Parse time trigger data
     */
    fun parseTimeTrigger(trigger: Trigger): Pair<String, List<String>>? {
        return TriggerParser.parseTimeData(trigger)
    }

    /**
     * Parse location trigger data
     */
    fun parseLocationTrigger(trigger: Trigger): LocationData? {
        return TriggerParser.parseLocationData(trigger)
    }

    /**
     * Parse WiFi trigger data
     */
    fun parseWifiTrigger(trigger: Trigger): WiFiData? {
        return TriggerParser.parseWifiData(trigger)
    }

    /**
     * Parse Bluetooth trigger data
     */
    fun parseBluetoothTrigger(trigger: Trigger): BluetoothData? {
        return TriggerParser.parseBluetoothData(trigger)
    }

    /**
     * Parse battery trigger data
     */
    fun parseBatteryTrigger(trigger: Trigger): BatteryData? {
        return TriggerParser.parseBatteryData(trigger)
    }

    /**
     * ✅ Parse manual trigger data
     */
    fun parseManualTrigger(trigger: Trigger): String? {
        return if (trigger is Trigger.ManualTrigger) {
            trigger.actionType
        } else {
            null
        }
    }

    /**
     * ✅ Check if trigger is a manual trigger
     */
    fun isManualTrigger(trigger: Trigger): Boolean {
        return trigger is Trigger.ManualTrigger || trigger.type == "MANUAL"
    }

    /**
     * ✅ Check if trigger is a time range trigger
     */
    fun isTimeRangeTrigger(trigger: Trigger): Boolean {
        return trigger is TimeRangeTrigger || trigger.type == "TIME_RANGE"
    }
}