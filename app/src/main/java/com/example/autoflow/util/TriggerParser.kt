package com.example.autoflow.util

import android.util.Log
import com.example.autoflow.model.Trigger
import org.json.JSONObject

/**
 * ✅ Helper to parse trigger JSON values safely
 */
object TriggerParser {
    private const val TAG = "TriggerParser"

    // Parse time trigger data
    fun parseTimeData(trigger: Trigger): Pair<String, List<String>>? {
        if (trigger.type != "TIME") return null

        return try {
            val json = JSONObject(trigger.value)
            val time = json.optString("time", "")
            val daysArray = json.optJSONArray("days")
            val days = mutableListOf<String>()

            if (daysArray != null) {
                for (i in 0 until daysArray.length()) {
                    days.add(daysArray.getString(i))
                }
            }

            if (time.isNotEmpty()) Pair(time, days) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time trigger: ${e.message}")
            null
        }
    }

    // Parse location trigger data
    fun parseLocationData(trigger: Trigger): LocationData? {
        if (trigger.type != "LOCATION") return null

        return try {
            val json = JSONObject(trigger.value)
            LocationData(
                locationName = json.optString("locationName", "Unknown"),
                latitude = json.optDouble("latitude", 0.0),
                longitude = json.optDouble("longitude", 0.0),
                radius = json.optDouble("radius", 100.0),
                triggerOnEntry = json.optBoolean("triggerOnEntry", true),
                triggerOnExit = json.optBoolean("triggerOnExit", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing location trigger: ${e.message}")
            null
        }
    }

    // Parse WiFi trigger data
    fun parseWifiData(trigger: Trigger): WiFiData? {
        if (trigger.type != "WIFI") return null

        return try {
            val json = JSONObject(trigger.value)
            WiFiData(
                ssid = json.optString("ssid").takeIf { it.isNotEmpty() },
                state = json.optString("state", "connected")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WiFi trigger: ${e.message}")
            null
        }
    }

    // Parse Bluetooth trigger data
    fun parseBluetoothData(trigger: Trigger): BluetoothData? {
        if (trigger.type != "BLUETOOTH") return null

        return try {
            val json = JSONObject(trigger.value)
            BluetoothData(
                deviceAddress = json.optString("deviceAddress", ""),
                deviceName = json.optString("deviceName").takeIf { it.isNotEmpty() },
                state = json.optString("state", "connected")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Bluetooth trigger: ${e.message}")
            null
        }
    }

    // Parse Battery trigger data
    fun parseBatteryData(trigger: Trigger): BatteryData? {
        if (trigger.type != "BATTERY") return null

        return try {
            val json = JSONObject(trigger.value)
            BatteryData(
                level = json.optInt("level", 50),
                condition = json.optString("condition", "below")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing battery trigger: ${e.message}")
            null
        }
    }
}

// Data classes for parsed trigger data
data class LocationData(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val triggerOnEntry: Boolean,
    val triggerOnExit: Boolean
)

data class WiFiData(
    val ssid: String?,
    val state: String
)

data class BluetoothData(
    val deviceAddress: String,
    val deviceName: String?,
    val state: String
)

data class BatteryData(
    val level: Int,
    val condition: String
)
