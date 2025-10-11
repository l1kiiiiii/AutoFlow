package com.example.autoflow.util

/**
 * Application-wide constants for AutoFlow
 * Centralized configuration for triggers, actions, and app behavior
 */
object Constants {

    // ========== APP INFO ==========
    const val APP_NAME = "AutoFlow"
    const val NOTIFICATION_CHANNEL_ID = "autoflow_channel"
    const val NOTIFICATION_CHANNEL_NAME = "AutoFlow Automations"

    // ========== TRIGGER TYPES ==========
    const val TRIGGER_LOCATION = "Location"
    const val TRIGGER_TIME = "Time"
    const val TRIGGER_WIFI = "WiFi"
    const val TRIGGER_BLE = "Bluetooth Device"
    const val TRIGGER_APP_LAUNCH = "App Launch"
    const val TRIGGER_BATTERY_LEVEL = "Battery Level"
    const val TRIGGER_CHARGING_STATE = "Charging State"
    const val TRIGGER_HEADPHONE_CONNECTION = "Headphone Connection"

    // ========== ACTION TYPES ==========
    const val ACTION_SEND_NOTIFICATION = "Send Notification"
    const val ACTION_TOGGLE_WIFI = "Toggle WiFi"
    const val ACTION_TOGGLE_BLUETOOTH = "Toggle Bluetooth"
    const val ACTION_SET_SOUND_MODE = "Set Sound Mode"
    const val ACTION_RUN_SCRIPT = "Run Script"
    const val ACTION_OPEN_APP = "Open App"
    const val ACTION_SEND_SMS = "Send SMS"
    const val ACTION_ADJUST_BRIGHTNESS = "Adjust Brightness"
    const val ACTION_ADJUST_VOLUME = "Adjust Volume"

    // ========== SOUND MODES ==========
    const val SOUND_MODE_NORMAL = "Normal"
    const val SOUND_MODE_SILENT = "Silent"
    const val SOUND_MODE_VIBRATE = "Vibrate"
    const val SOUND_MODE_DND = "DND"

    // ========== WIFI STATES ==========
    const val WIFI_STATE_ON = "ON"
    const val WIFI_STATE_OFF = "OFF"
    const val WIFI_STATE_CONNECTED = "CONNECTED"
    const val WIFI_STATE_DISCONNECTED = "DISCONNECTED"

    // ========== BLUETOOTH STATES ==========
    const val BLUETOOTH_STATE_ON = "ON"
    const val BLUETOOTH_STATE_OFF = "OFF"
    const val BLUETOOTH_STATE_CONNECTED = "CONNECTED"
    const val BLUETOOTH_STATE_DISCONNECTED = "DISCONNECTED"

    // ========== LOCATION CONSTANTS ==========
    const val LOCATION_MIN_RADIUS = 50f // meters
    const val LOCATION_MAX_RADIUS = 5000f // meters
    const val LOCATION_DEFAULT_RADIUS = 100f // meters

    // ========== TIME CONSTANTS ==========
    const val TIME_WINDOW_MS = 60000L // 1 minute
    const val MAX_FUTURE_TIME_MS = 31536000000L // 1 year

    // ========== BATTERY CONSTANTS ==========
    const val BATTERY_MIN_LEVEL = 0
    const val BATTERY_MAX_LEVEL = 100

    // ========== JSON KEYS ==========

    // Location
    const val JSON_KEY_LOCATION_COORDINATES = "coordinates"
    const val JSON_KEY_LOCATION_NAME = "locationName"
    const val JSON_KEY_LOCATION_RADIUS = "radius"
    const val JSON_KEY_LOCATION_TRIGGER_ON = "triggerOn"

    // WiFi
    const val JSON_KEY_WIFI_SSID = "ssid"
    const val JSON_KEY_WIFI_TARGET_STATE = "state"

    // BLE
    const val JSON_KEY_BLE_DEVICE_ADDRESS = "deviceAddress"
    const val JSON_KEY_BLE_DEVICE_NAME = "deviceName"

    // App Launch
    const val JSON_KEY_APP_PACKAGE_NAME = "packageName"
    const val JSON_KEY_APP_NAME = "appName"

    // Action
    const val JSON_KEY_ACTION_TYPE = "type"
    const val JSON_KEY_ACTION_VALUE = "value"
    const val JSON_KEY_ACTION_TITLE = "title"
    const val JSON_KEY_ACTION_MESSAGE = "message"
    const val JSON_KEY_ACTION_PRIORITY = "priority"

    // ========== WORKER KEYS ==========
    const val KEY_WORKFLOW_ID = "workflow_id"
    const val KEY_TRIGGER_TYPE = "trigger_type"
    const val KEY_TIME_TRIGGER = "time_trigger"
    const val KEY_BLE_DEVICE_ADDRESS = "ble_device_address"
    const val KEY_LOCATION_LAT = "location_lat"
    const val KEY_LOCATION_LNG = "location_lng"
    const val KEY_LOCATION_RADIUS = "location_radius"

    // ========== PREFERENCES ==========
    const val PREF_NAME = "autoflow_prefs"
    const val PREF_FIRST_LAUNCH = "first_launch"
    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"

    // ========== NOTIFICATION PRIORITIES ==========
    const val NOTIFICATION_PRIORITY_LOW = "Low"
    const val NOTIFICATION_PRIORITY_NORMAL = "Normal"
    const val NOTIFICATION_PRIORITY_HIGH = "High"

    // ========== VALIDATION ==========
    const val MAX_WORKFLOW_NAME_LENGTH = 50
    const val MIN_WORKFLOW_NAME_LENGTH = 1
    const val MAX_NOTIFICATION_TITLE_LENGTH = 50
    const val MAX_NOTIFICATION_MESSAGE_LENGTH = 200

    // ========== DEBUGGING ==========
    const val DEBUG_MODE = true
    const val LOG_TAG = "AutoFlow"

    // ========== TRIGGER OPTIONS ==========
    val TRIGGER_OPTIONS = listOf(
        TRIGGER_LOCATION,
        TRIGGER_TIME,
        TRIGGER_WIFI,
        TRIGGER_BLE,
        TRIGGER_APP_LAUNCH,
        TRIGGER_BATTERY_LEVEL,
        TRIGGER_CHARGING_STATE,
        TRIGGER_HEADPHONE_CONNECTION
    )

    val ACTION_OPTIONS = listOf(
        ACTION_SEND_NOTIFICATION,
        ACTION_TOGGLE_WIFI,
        ACTION_TOGGLE_BLUETOOTH,
        ACTION_SET_SOUND_MODE,
        ACTION_RUN_SCRIPT,
        ACTION_OPEN_APP,
        ACTION_ADJUST_BRIGHTNESS,
        ACTION_ADJUST_VOLUME
    )

    val SOUND_MODE_OPTIONS = listOf(
        SOUND_MODE_NORMAL,
        SOUND_MODE_SILENT,
        SOUND_MODE_VIBRATE,
        SOUND_MODE_DND
    )

    val NOTIFICATION_PRIORITY_OPTIONS = listOf(
        NOTIFICATION_PRIORITY_LOW,
        NOTIFICATION_PRIORITY_NORMAL,
        NOTIFICATION_PRIORITY_HIGH
    )
}
