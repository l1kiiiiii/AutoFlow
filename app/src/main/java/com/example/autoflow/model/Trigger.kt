package com.example.autoflow.model

import com.example.autoflow.util.Constants
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * ✅ Legacy WiFi Trigger class for backward compatibility
 */
class WiFiTriggerLegacy {
    @JvmField
    var ssid: String? = null

    @JvmField
    var bssid: String? = null

    @JvmField
    var triggerOn: String? = "connect" // "connect" or "disconnect"

    constructor()

    constructor(ssid: String?, triggerOn: String?) {
        this.ssid = ssid
        this.triggerOn = triggerOn
    }
}

/**
 * ✅ Legacy Bluetooth Trigger class for backward compatibility
 */
class BluetoothTriggerLegacy {
    @JvmField
    var macAddress: String? = null

    @JvmField
    var deviceName: String? = null

    @JvmField
    var triggerOn: String? = "connect" // "connect" or "disconnect"

    constructor()

    constructor(macAddress: String?, deviceName: String?, triggerOn: String?) {
        this.macAddress = macAddress
        this.deviceName = deviceName
        this.triggerOn = triggerOn
    }
}

/**
 * ✅ Sealed class representing different types of workflow triggers
 */
sealed class Trigger(val type: String, val value: String) {

    /**
     * Location-based trigger
     */
    data class LocationTrigger(
        val locationName: String,
        val latitude: Double,
        val longitude: Double,
        val radius: Double,
        val triggerOnEntry: Boolean,
        val triggerOnExit: Boolean,
        val triggerOn: String = "both"
    ) : Trigger(
        "LOCATION",
        buildJsonValue(locationName, latitude, longitude, radius, triggerOnEntry, triggerOnExit, triggerOn)
    )

    /**
     * WiFi-based trigger
     */
    data class WiFiTrigger(
        val ssid: String? = null,
        val state: String // "ON", "OFF", "CONNECTED", "DISCONNECTED"
    ) : Trigger("WIFI", buildWiFiJson(ssid, state))

    /**
     * Bluetooth-based trigger
     */
    data class BluetoothTrigger(
        val deviceAddress: String,
        val deviceName: String? = null
    ) : Trigger("BLUETOOTH", buildBluetoothJson(deviceAddress, deviceName))

    /**
     * Time-based trigger
     */
    data class TimeTrigger(
        val time: String,
        val days: List<String>
    ) : Trigger("TIME", buildTimeJson(time, days))

    /**
     * Battery level trigger
     */
    data class BatteryTrigger(
        val level: Int,
        val condition: String
    ) : Trigger("BATTERY", buildBatteryJson(level, condition))

    /**
     * ✅ Manual trigger - activated by user tap (for Meeting Mode, etc.)
     */
    data class ManualTrigger(
        val actionType: String = "quick_action"
    ) : Trigger("MANUAL", buildManualJson(actionType))

    // VALIDATION PROPERTIES

    val isValid: Boolean
        get() {
            // Basic checks
            if (type.isBlank() || value.isBlank()) return false

            return when (type.trim()) {
                Constants.TRIGGER_TIME -> validateTimeTrigger(value)
                Constants.TRIGGER_BLE -> validateBleTrigger(value)
                Constants.TRIGGER_LOCATION -> validateLocationTrigger(value)
                Constants.TRIGGER_WIFI -> validateWiFiTrigger(value)
                Constants.TRIGGER_APP_LAUNCH -> validateAppLaunchTrigger(value)
                Constants.TRIGGER_BATTERY_LEVEL -> validateBatteryLevelTrigger(value)
                Constants.TRIGGER_CHARGING_STATE -> validateChargingStateTrigger(value)
                Constants.TRIGGER_HEADPHONE_CONNECTION -> validateHeadphoneConnectionTrigger(value)
                "MANUAL" -> true // ✅ Manual triggers are always valid
                "TIME_RANGE" -> true // ✅ Time range triggers are always valid
                else -> false
            }
        }

    val validationError: String?
        get() {
            if (type.isBlank()) return "Trigger type cannot be empty"
            if (value.isBlank()) return "Trigger value cannot be empty"

            return when (type.trim()) {
                Constants.TRIGGER_TIME -> if (!validateTimeTrigger(value))
                    "Invalid timestamp format or value out of range" else null

                Constants.TRIGGER_BLE -> if (!validateBleTrigger(value))
                    "Invalid BLE device address or name format" else null

                Constants.TRIGGER_LOCATION -> if (!validateLocationTrigger(value))
                    "Invalid location format. Use 'lat,lng,radius' or JSON format" else null

                Constants.TRIGGER_WIFI -> if (!validateWiFiTrigger(value))
                    "Invalid WiFi state or SSID format" else null

                "MANUAL" -> null // ✅ Manual triggers don't need validation
                "TIME_RANGE" -> null // ✅ Time range triggers validated separately

                else -> "Unknown trigger type: $type"
            }
        }

    companion object {
        private val MAC_ADDRESS_PATTERN: Pattern = Pattern.compile(
            "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
        )
        private val COORDINATES_PATTERN: Pattern = Pattern.compile(
            "^-?\\d+\\.?\\d*,-?\\d+\\.?\\d*(,-?\\d+\\.?\\d*)?$"
        )

        //  JSON BUILDER FUNCTIONS

        fun buildJsonValue(
            locationName: String,
            latitude: Double,
            longitude: Double,
            radius: Double,
            triggerOnEntry: Boolean,
            triggerOnExit: Boolean,
            triggerOn: String
        ): String {
            return """{"locationName":"$locationName","latitude":$latitude,"longitude":$longitude,"radius":$radius,"triggerOnEntry":$triggerOnEntry,"triggerOnExit":$triggerOnExit,"triggerOn":"$triggerOn"}"""
        }

        fun buildWiFiJson(ssid: String?, state: String): String {
            return if (ssid != null) {
                """{"ssid":"$ssid","state":"$state"}"""
            } else {
                """{"state":"$state"}"""
            }
        }

        fun buildBluetoothJson(deviceAddress: String, deviceName: String?): String {
            return if (deviceName != null) {
                """{"deviceAddress":"$deviceAddress","deviceName":"$deviceName"}"""
            } else {
                """{"deviceAddress":"$deviceAddress"}"""
            }
        }

        fun buildTimeJson(time: String, days: List<String>): String {
            val daysJson = days.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
            return """{"time":"$time","days":$daysJson}"""
        }

        fun buildBatteryJson(level: Int, condition: String): String {
            return """{"level":$level,"condition":"$condition"}"""
        }

        /**
         * ✅ Build JSON for manual trigger
         */
        fun buildManualJson(actionType: String): String {
            return JSONObject().apply {
                put("type", "MANUAL")
                put("value", actionType)
            }.toString()
        }

        //  VALIDATION METHODS

        private fun validateTimeTrigger(value: String): Boolean {
            return try {
                val timestamp = value.toLong()
                val currentTime = System.currentTimeMillis()
                val maxFutureTime = currentTime + Constants.MAX_FUTURE_TIME_MS
                val minPastTime = currentTime - Constants.MAX_FUTURE_TIME_MS

                timestamp in minPastTime..maxFutureTime
            } catch (e: NumberFormatException) {
                false
            }
        }

        private fun validateBleTrigger(value: String): Boolean {
            // Check MAC address format
            if (MAC_ADDRESS_PATTERN.matcher(value).matches()) return true

            // Check device name (1-248 characters)
            return value.length in 1..248 && value.isNotBlank()
        }

        private fun validateLocationTrigger(value: String): Boolean {
            // Try JSON format first
            if (validateLocationJson(value)) return true

            // Try coordinate format: "lat,lng" or "lat,lng,radius"
            return validateLocationCoordinates(value)
        }

        private fun validateLocationJson(value: String): Boolean {
            return try {
                val json = JSONObject(value)

                // Check for coordinates field
                if (json.has(Constants.JSON_KEY_LOCATION_COORDINATES)) {
                    val coordinates = json.getString(Constants.JSON_KEY_LOCATION_COORDINATES)
                    return validateLocationCoordinates(coordinates)
                }

                // Check for separate lat/lng fields
                if (json.has("latitude") && json.has("longitude")) {
                    val lat = json.getDouble("latitude")
                    val lng = json.getDouble("longitude")
                    return isValidLatitude(lat) && isValidLongitude(lng)
                }

                false
            } catch (e: Exception) {
                false
            }
        }

        private fun validateLocationCoordinates(coordinates: String): Boolean {
            if (!COORDINATES_PATTERN.matcher(coordinates).matches()) return false

            return try {
                val parts = coordinates.split(",")
                if (parts.size !in 2..3) return false

                val lat = parts[0].trim().toDouble()
                val lng = parts[1].trim().toDouble()

                if (!isValidLatitude(lat) || !isValidLongitude(lng)) return false

                // Validate radius if present
                if (parts.size == 3) {
                    val radius = parts[2].trim().toFloat()
                    return radius in Constants.LOCATION_MIN_RADIUS..Constants.LOCATION_MAX_RADIUS
                }

                true
            } catch (e: NumberFormatException) {
                false
            }
        }

        private fun validateWiFiTrigger(value: String): Boolean {
            // Try JSON format first
            return try {
                val json = JSONObject(value)

                // Check for WiFi state
                if (json.has(Constants.JSON_KEY_WIFI_TARGET_STATE)) {
                    val state = json.getString(Constants.JSON_KEY_WIFI_TARGET_STATE)
                    return isValidWiFiState(state)
                }

                // Check for SSID
                if (json.has(Constants.JSON_KEY_WIFI_SSID)) {
                    val ssid = json.getString(Constants.JSON_KEY_WIFI_SSID)
                    return isValidSSID(ssid)
                }

                false
            } catch (e: Exception) {
                // Try simple state format
                isValidWiFiState(value)
            }
        }

        private fun validateAppLaunchTrigger(value: String): Boolean {
            return try {
                val json = JSONObject(value)

                // Must have package name
                if (!json.has(Constants.JSON_KEY_APP_PACKAGE_NAME)) return false

                val packageName = json.getString(Constants.JSON_KEY_APP_PACKAGE_NAME)
                isValidPackageName(packageName)
            } catch (e: Exception) {
                // Try as simple package name
                isValidPackageName(value)
            }
        }

        private fun validateBatteryLevelTrigger(value: String): Boolean {
            return try {
                val level = value.toInt()
                level in 0..100
            } catch (e: NumberFormatException) {
                false
            }
        }

        private fun validateChargingStateTrigger(value: String): Boolean {
            return value.uppercase() in listOf("CHARGING", "NOT_CHARGING", "PLUGGED", "UNPLUGGED")
        }

        private fun validateHeadphoneConnectionTrigger(value: String): Boolean {
            return value.uppercase() in listOf("CONNECTED", "DISCONNECTED")
        }

        //  HELPER METHODS

        private fun isValidLatitude(lat: Double): Boolean = lat in -90.0..90.0

        private fun isValidLongitude(lng: Double): Boolean = lng in -180.0..180.0

        private fun isValidWiFiState(state: String): Boolean {
            return state.uppercase() in listOf(
                Constants.WIFI_STATE_ON.uppercase(),
                Constants.WIFI_STATE_OFF.uppercase(),
                Constants.WIFI_STATE_CONNECTED.uppercase(),
                Constants.WIFI_STATE_DISCONNECTED.uppercase()
            )
        }

        private fun isValidSSID(ssid: String): Boolean {
            // SSID should be 1-32 characters
            return ssid.length in 1..32 && ssid.isNotBlank()
        }

        private fun isValidPackageName(packageName: String): Boolean {
            // Basic package name validation
            val pattern = "^[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)*$".toRegex()
            return packageName.matches(pattern) && packageName.length in 3..255
        }
    }
}

/**
 * ✅ Enhanced Time Range Trigger with sleep mode support
 */
class TimeRangeTrigger : Trigger {
    var startTime: String = "22:00"
    var endTime: String = "07:00"
    var isActive: Boolean = false
    var recurringDays: List<String> = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    // Primary constructor with default values
    constructor(
        startTime: String = "22:00",
        endTime: String = "07:00",
        days: List<String> = listOf(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
        )
    ) : super(
        type = "TIME_RANGE",
        value = buildTimeRangeJson(startTime, endTime, days)
    ) {
        this.startTime = startTime
        this.endTime = endTime
        this.recurringDays = days
    }
    companion object {
        private fun buildTimeRangeJson(startTime: String, endTime: String, days: List<String>): String {
            val daysJson = days.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
            return """{"startTime":"$startTime","endTime":"$endTime","days":$daysJson}"""
        }
    }
}