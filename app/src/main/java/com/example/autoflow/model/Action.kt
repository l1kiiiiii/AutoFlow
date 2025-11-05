package com.example.autoflow.model

/**
 *  Enhanced Action class for AutoFlow workflows
 * Represents different types of actions that can be executed
 */
class Action {
    @JvmField
    var type: String? = null

    @JvmField
    var title: String? = null

    @JvmField
    var message: String? = null

    @JvmField
    var priority: String? = null

    @JvmField
    var value: String? = null

    @JvmField
    var duration: Long? = null

    @JvmField
    var scheduledUnblockTime: Long? = null

    // Constructor for notification actions
    constructor(type: String?, title: String?, message: String?, priority: String?) {
        this.type = type
        this.title = title
        this.message = message
        this.priority = priority
        this.value = null
        this.duration = null
        this.scheduledUnblockTime = null
    }

    // Constructor for simple actions (only type)
    constructor(type: String?) {
        this.type = type
        this.title = null
        this.message = null
        this.priority = "Normal"
        this.value = null
        this.duration = null
        this.scheduledUnblockTime = null
    }

    // Constructor for actions with value (e.g., sound mode, toggles)
    constructor(type: String?, value: String?) {
        this.type = type
        this.value = value
        this.title = null
        this.message = null
        this.priority = "Normal"
        this.duration = null
        this.scheduledUnblockTime = null
    }

    // Constructor for actions with duration (e.g., timers, blocks)
    constructor(type: String?, value: String?, duration: Long?) {
        this.type = type
        this.value = value
        this.duration = duration
        this.title = null
        this.message = null
        this.priority = "Normal"
        this.scheduledUnblockTime = null
    }

    // Full constructor
    constructor(
        type: String?,
        title: String?,
        message: String?,
        priority: String?,
        value: String?,
        duration: Long?,
        scheduledUnblockTime: Long?
    ) {
        this.type = type
        this.title = title
        this.message = message
        this.priority = priority
        this.value = value
        this.duration = duration
        this.scheduledUnblockTime = scheduledUnblockTime
    }

    // Default constructor
    constructor() {
        this.type = null
        this.title = null
        this.message = null
        this.priority = "Normal"
        this.value = null
        this.duration = null
        this.scheduledUnblockTime = null
    }

    companion object {
        // Action types constants
        const val TYPE_NOTIFICATION = "NOTIFICATION"
        const val TYPE_SOUND_MODE = "SET_SOUND_MODE"
        const val TYPE_WIFI_TOGGLE = "TOGGLE_WIFI"
        const val TYPE_BLUETOOTH_TOGGLE = "TOGGLE_BLUETOOTH"
        const val TYPE_BLOCK_APPS = "BLOCK_APPS"
        const val TYPE_RUN_SCRIPT = "RUN_SCRIPT"
        const val TYPE_BRIGHTNESS = "SET_BRIGHTNESS"
        const val TYPE_VOLUME = "SET_VOLUME"
        const val TYPE_AIRPLANE_MODE = "TOGGLE_AIRPLANE_MODE"
        const val TYPE_AUTO_ROTATION = "TOGGLE_AUTO_ROTATION"
        const val TYPE_FLASHLIGHT = "TOGGLE_FLASHLIGHT"

        // Sound mode values
        const val SOUND_NORMAL = "Normal"
        const val SOUND_VIBRATE = "Vibrate"
        const val SOUND_SILENT = "Silent"
        const val SOUND_DND = "DND"

        // Priority levels
        const val PRIORITY_LOW = "Low"
        const val PRIORITY_NORMAL = "Normal"
        const val PRIORITY_HIGH = "High"
        const val PRIORITY_URGENT = "Urgent"

        // Factory methods for common actions
        fun createNotificationAction(title: String, message: String, priority: String = PRIORITY_NORMAL): Action {
            return Action(TYPE_NOTIFICATION, title, message, priority)
        }

        fun createSoundModeAction(soundMode: String): Action {
            return Action(TYPE_SOUND_MODE, soundMode)
        }

        fun createWifiToggleAction(enabled: Boolean): Action {
            return Action(TYPE_WIFI_TOGGLE, if (enabled) "ON" else "OFF")
        }

        fun createBluetoothToggleAction(enabled: Boolean): Action {
            return Action(TYPE_BLUETOOTH_TOGGLE, if (enabled) "ON" else "OFF")
        }

        fun createBlockAppsAction(packages: String, duration: Long? = null): Action {
            return Action(TYPE_BLOCK_APPS, packages, duration)
        }

        fun createScriptAction(scriptContent: String): Action {
            return Action(TYPE_RUN_SCRIPT, scriptContent)
        }
    }

    override fun toString(): String {
        return "Action(type='$type', title='$title', message='$message', priority='$priority', value='$value', duration=$duration, scheduledUnblockTime=$scheduledUnblockTime)"
    }

    fun isValid(): Boolean {
        return !type.isNullOrEmpty()
    }

    fun getDisplayName(): String {
        return when (type) {
            TYPE_NOTIFICATION -> "Show Notification"
            TYPE_SOUND_MODE -> "Change Sound Mode"
            TYPE_WIFI_TOGGLE -> "Toggle WiFi"
            TYPE_BLUETOOTH_TOGGLE -> "Toggle Bluetooth"
            TYPE_BLOCK_APPS -> "Block Apps"
            TYPE_RUN_SCRIPT -> "Run Script"
            TYPE_BRIGHTNESS -> "Set Brightness"
            TYPE_VOLUME -> "Set Volume"
            TYPE_AIRPLANE_MODE -> "Toggle Airplane Mode"
            TYPE_AUTO_ROTATION -> "Toggle Auto Rotation"
            TYPE_FLASHLIGHT -> "Toggle Flashlight"
            else -> "Unknown Action"
        }
    }

    fun getDescription(): String {
        return when (type) {
            TYPE_NOTIFICATION -> title ?: message ?: "Show notification"
            TYPE_SOUND_MODE -> "Set to ${value ?: "Unknown"}"
            TYPE_WIFI_TOGGLE -> "Turn ${value ?: "Unknown"} WiFi"
            TYPE_BLUETOOTH_TOGGLE -> "Turn ${value ?: "Unknown"} Bluetooth"
            TYPE_BLOCK_APPS -> "Block selected apps${if (duration != null) " for ${duration}ms" else ""}"
            TYPE_RUN_SCRIPT -> "Execute custom script"
            else -> "Perform action"
        }
    }

    fun copy(): Action {
        return Action(type, title, message, priority, value, duration, scheduledUnblockTime)
    }
}
