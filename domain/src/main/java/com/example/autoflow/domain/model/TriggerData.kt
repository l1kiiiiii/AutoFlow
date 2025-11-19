package com.example.autoflow.domain.model

import kotlinx.serialization.Serializable

/**
 * Serializable trigger data models for clean JSON handling
 * These replace manual JSON parsing with automatic serialization
 */

@Serializable
sealed class TriggerData {
    abstract val type: String
    
    @Serializable
    data class Location(
        override val type: String = "LOCATION",
        val locationName: String,
        val latitude: Double,
        val longitude: Double,
        val radius: Double,
        val triggerOnEntry: Boolean = true,
        val triggerOnExit: Boolean = false
    ) : TriggerData()
    
    @Serializable
    data class WiFi(
        override val type: String = "WIFI",
        val ssid: String? = null,
        val state: String // "ON", "OFF", "CONNECTED", "DISCONNECTED"
    ) : TriggerData()
    
    @Serializable
    data class Bluetooth(
        override val type: String = "BLUETOOTH",
        val deviceAddress: String,
        val deviceName: String? = null
    ) : TriggerData()
    
    @Serializable
    data class Time(
        override val type: String = "TIME",
        val time: String,
        val days: List<String> = emptyList()
    ) : TriggerData()
    
    @Serializable
    data class Battery(
        override val type: String = "BATTERY",
        val level: Int,
        val operator: String // ">", "<", "==", ">=", "<="
    ) : TriggerData()
}
