package com.example.autoflow.model

import com.example.autoflow.util.*

/**
 * ✅ Helper functions to create triggers easily - Updated for JSON approach
 */
object TriggerHelpers {

    fun createTimeTrigger(time: String, days: List<String>): Trigger {
        val daysJson = days.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
        val value = """{"time":"$time","days":$daysJson}"""
        return Trigger(type = "TIME", value = value)
    }

    fun createLocationTrigger(
        locationName: String,
        latitude: Double,
        longitude: Double,
        radius: Double = 100.0,
        triggerOnEntry: Boolean = true,
        triggerOnExit: Boolean = false
    ): Trigger {
        val value = """{"locationName":"$locationName","latitude":$latitude,"longitude":$longitude,"radius":$radius,"triggerOnEntry":$triggerOnEntry,"triggerOnExit":$triggerOnExit}"""
        return Trigger(type = "LOCATION", value = value)
    }

    fun createWifiTrigger(ssid: String?, state: String): Trigger {
        val value = if (ssid != null) {
            """{"ssid":"$ssid","state":"$state"}"""
        } else {
            """{"state":"$state"}"""
        }
        return Trigger(type = "WIFI", value = value)
    }

    fun createBluetoothTrigger(deviceAddress: String, deviceName: String? = null): Trigger {
        val value = if (deviceName != null) {
            """{"deviceAddress":"$deviceAddress","deviceName":"$deviceName"}"""
        } else {
            """{"deviceAddress":"$deviceAddress"}"""
        }
        return Trigger(type = "BLUETOOTH", value = value)
    }

    fun createBatteryTrigger(level: Int, condition: String): Trigger {
        val value = """{"level":$level,"condition":"$condition"}"""
        return Trigger(type = "BATTERY", value = value)
    }

    // ✅ Parse trigger data back to individual fields
    fun parseTimeTrigger(trigger: Trigger): Pair<String, List<String>>? {
        return TriggerParser.parseTimeData(trigger)
    }

    fun parseLocationTrigger(trigger: Trigger): LocationData? {
        return TriggerParser.parseLocationData(trigger)
    }

    fun parseWifiTrigger(trigger: Trigger): WiFiData? {
        return TriggerParser.parseWifiData(trigger)
    }

    fun parseBluetoothTrigger(trigger: Trigger): BluetoothData? {
        return TriggerParser.parseBluetoothData(trigger)
    }

    fun parseBatteryTrigger(trigger: Trigger): BatteryData? {
        return TriggerParser.parseBatteryData(trigger)
    }
}
