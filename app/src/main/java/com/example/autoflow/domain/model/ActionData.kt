package com.example.autoflow.domain.model

import kotlinx.serialization.Serializable

/**
 * Serializable action data models for clean JSON handling
 * These replace manual JSON parsing with automatic serialization
 */

@Serializable
sealed class ActionData {
    abstract val type: String
    
    @Serializable
    data class Notification(
        override val type: String = "NOTIFICATION",
        val title: String,
        val message: String,
        val priority: String = "Normal"
    ) : ActionData()
    
    @Serializable
    data class SoundMode(
        override val type: String = "SET_SOUND_MODE",
        val mode: String // "Normal", "Vibrate", "Silent", "DND"
    ) : ActionData()
    
    @Serializable
    data class WiFiToggle(
        override val type: String = "TOGGLE_WIFI",
        val enabled: Boolean
    ) : ActionData()
    
    @Serializable
    data class BluetoothToggle(
        override val type: String = "TOGGLE_BLUETOOTH",
        val enabled: Boolean
    ) : ActionData()
    
    @Serializable
    data class BlockApps(
        override val type: String = "BLOCK_APPS",
        val packages: String,
        val duration: Long? = null
    ) : ActionData()
    
    @Serializable
    data class RunScript(
        override val type: String = "RUN_SCRIPT",
        val scriptContent: String
    ) : ActionData()
    
    @Serializable
    data class SetBrightness(
        override val type: String = "SET_BRIGHTNESS",
        val level: Int // 0-100
    ) : ActionData()
    
    @Serializable
    data class SetVolume(
        override val type: String = "SET_VOLUME",
        val level: Int // 0-100
    ) : ActionData()
}
