package com.example.autoflow.util

/**
 * Central constants repository for AutoFlow automation app
 * Organized by category for better maintainability
 */
object Constants {

    const val ACTION_BLOCK_APPS = "BLOCK_APPS"
    const val ACTION_UNBLOCK_APPS = "UNBLOCK_APPS"

    //  TRIGGER TYPES 
    const val TRIGGER_TIME = "TIME"
    const val TRIGGER_BLE = "BLE"
    const val TRIGGER_LOCATION = "LOCATION"
    const val TRIGGER_WIFI = "WIFI" // Standardized name (was TRIGGER_WIFI_STATE)
    const val TRIGGER_APP_LAUNCH = "APP_LAUNCH"
    const val TRIGGER_BATTERY_LEVEL = "BATTERY_LEVEL"
    const val TRIGGER_CHARGING_STATE = "CHARGING_STATE"
    const val TRIGGER_HEADPHONE_CONNECTION = "HEADPHONE_CONNECTION"

    // Action type for sound mode
    const val ACTION_SET_SOUND_MODE = "SET_SOUND_MODE"

    // Sound mode values
    const val SOUND_MODE_RING = "ring"
    const val SOUND_MODE_VIBRATE = "vibrate"
    const val SOUND_MODE_SILENT = "silent"

    // DND modes (require notification policy access)
    const val SOUND_MODE_DND_NONE = "dnd_none"       // Total silence
    const val SOUND_MODE_DND_PRIORITY = "dnd_priority"
    const val SOUND_MODE_DND_ALARMS = "dnd_alarms"
    const val SOUND_MODE_DND_ALL = "dnd_all"         // Turn off DND


    //  ACTION TYPES 
    const val ACTION_SEND_NOTIFICATION = "SEND_NOTIFICATION"
    const val ACTION_TOGGLE_WIFI = "TOGGLE_WIFI"
    const val ACTION_TOGGLE_BLUETOOTH = "TOGGLE_BLUETOOTH"
    const val ACTION_TOGGLE_SETTINGS = "TOGGLE_SETTINGS"
    const val ACTION_RUN_SCRIPT = "RUN_SCRIPT"
    const val ACTION_LAUNCH_APP = "LAUNCH_APP"
    const val ACTION_SEND_SMS = "SEND_SMS"
    const val ACTION_PLAY_SOUND = "PLAY_SOUND"
    const val ACTION_SET_VOLUME = "SET_VOLUME"
    const val ACTION_TOGGLE_FLASHLIGHT = "TOGGLE_FLASHLIGHT"

    //  WORKER INPUT KEYS 
    const val KEY_WORKFLOW_ID = "workflow_id"
    const val KEY_TRIGGER_TYPE = "trigger_type"
    const val KEY_TRIGGER_VALUE = "trigger_value"
    const val KEY_ACTION_TYPE = "action_type"
    const val KEY_ACTION_PARAMETERS = "action_parameters"
    const val KEY_BLE_DEVICE_ADDRESS = "ble_device_address"
    const val KEY_BLE_DEVICE_NAME = "ble_device_name"
    const val KEY_TIME_TRIGGER = "time_trigger"
    const val KEY_LOCATION_LAT = "location_latitude"
    const val KEY_LOCATION_LNG = "location_longitude"
    const val KEY_LOCATION_RADIUS = "location_radius"

    //  JSON PARAMETER KEYS 
    // Notification parameters
    const val JSON_KEY_NOTIFICATION_TITLE = "notificationTitle"
    const val JSON_KEY_NOTIFICATION_MESSAGE = "notificationMessage"
    const val JSON_KEY_NOTIFICATION_PRIORITY = "notificationPriority"
    const val JSON_KEY_NOTIFICATION_CHANNEL_ID = "notificationChannelId"
    const val JSON_KEY_NOTIFICATION_ICON = "notificationIcon"

    // WiFi parameters
    const val JSON_KEY_WIFI_TARGET_STATE = "wifiTargetState"
    const val JSON_KEY_WIFI_SSID = "wifiSsid"
    const val JSON_KEY_WIFI_SECURITY_TYPE = "wifiSecurityType"

    // Settings toggle parameters
    const val JSON_KEY_SETTING_TO_TOGGLE = "settingToToggle"
    const val JSON_KEY_SETTING_TARGET_STATE = "settingTargetState"

    // Script parameters
    const val JSON_KEY_SCRIPT_CONTENT = "scriptContent"
    const val JSON_KEY_SCRIPT_LANGUAGE = "scriptLanguage"
    const val JSON_KEY_SCRIPT_TIMEOUT = "scriptTimeout"

    // App launch parameters
    const val JSON_KEY_APP_PACKAGE_NAME = "appPackageName"
    const val JSON_KEY_APP_CLASS_NAME = "appClassName"

    // Location parameters
    const val JSON_KEY_LOCATION_NAME = "locationName"
    const val JSON_KEY_LOCATION_COORDINATES = "locationCoordinates"
    const val JSON_KEY_LOCATION_RADIUS = "locationRadius"
    const val JSON_KEY_LOCATION_ENTRY_EXIT = "locationEntryExit"

    //  NOTIFICATION CONSTANTS 
    const val NOTIFICATION_CHANNEL_ID_DEFAULT = "autoflow_default"
    const val NOTIFICATION_CHANNEL_ID_HIGH_PRIORITY = "autoflow_high_priority"
    const val NOTIFICATION_CHANNEL_ID_TRIGGERS = "autoflow_triggers"
    const val NOTIFICATION_CHANNEL_NAME_DEFAULT = "AutoFlow Notifications"
    const val NOTIFICATION_CHANNEL_NAME_HIGH_PRIORITY = "AutoFlow High Priority"
    const val NOTIFICATION_CHANNEL_NAME_TRIGGERS = "AutoFlow Triggers"

    // Notification priorities
    const val NOTIFICATION_PRIORITY_LOW = "Low"
    const val NOTIFICATION_PRIORITY_NORMAL = "Normal"
    const val NOTIFICATION_PRIORITY_HIGH = "High"
    const val NOTIFICATION_PRIORITY_MAX = "Max"

    //  WIFI CONSTANTS 
    const val WIFI_STATE_ON = "ON"
    const val WIFI_STATE_OFF = "OFF"
    const val WIFI_STATE_CONNECTED = "CONNECTED"
    const val WIFI_STATE_DISCONNECTED = "DISCONNECTED"

    //  BLUETOOTH CONSTANTS 
    const val BLUETOOTH_STATE_ON = "ON"
    const val BLUETOOTH_STATE_OFF = "OFF"
    const val BLUETOOTH_STATE_CONNECTED = "CONNECTED"
    const val BLUETOOTH_STATE_DISCONNECTED = "DISCONNECTED"

    //  LOCATION CONSTANTS 
    const val LOCATION_TRIGGER_ENTRY = "Entry"
    const val LOCATION_TRIGGER_EXIT = "Exit"
    const val LOCATION_TRIGGER_BOTH = "Both"

    // Default location radius in meters
    const val LOCATION_DEFAULT_RADIUS = 100f
    const val LOCATION_MIN_RADIUS = 20f
    const val LOCATION_MAX_RADIUS = 1000f

    //  TIME CONSTANTS 
    // Time window for time-based triggers (1 minute in milliseconds)
    const val TIME_WINDOW_MS = 60 * 1000L
    // Default trigger check interval (5 minutes)
    const val TRIGGER_CHECK_INTERVAL_MS = 5 * 60 * 1000L
    // Maximum allowed future time for triggers (24 hours)
    const val MAX_FUTURE_TIME_MS = 24 * 60 * 60 * 1000L

    //  SYSTEM SETTINGS CONSTANTS 
    const val SETTING_WIFI = "WiFi"
    const val SETTING_BLUETOOTH = "Bluetooth"
    const val SETTING_LOCATION = "Location"
    const val SETTING_AIRPLANE_MODE = "AirplaneMode"
    const val SETTING_DO_NOT_DISTURB = "DoNotDisturb"
    const val SETTING_AUTO_ROTATE = "AutoRotate"
    const val SETTING_FLASHLIGHT = "Flashlight"
    const val SETTING_MOBILE_DATA = "MobileData"

    //  DATABASE CONSTANTS 
    const val DATABASE_NAME = "autoflow_database"
    const val DATABASE_VERSION = 1
    const val TABLE_WORKFLOWS = "workflows"

    //  SHARED PREFERENCES KEYS 
    const val PREF_FILE_NAME = "autoflow_preferences"
    const val PREF_FIRST_RUN = "pref_first_run"
    const val PREF_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"
    const val PREF_LOCATION_UPDATES_ENABLED = "pref_location_updates_enabled"
    const val PREF_BLUETOOTH_SCAN_ENABLED = "pref_bluetooth_scan_enabled"
    const val PREF_TRIGGER_CHECK_INTERVAL = "pref_trigger_check_interval"
    const val PREF_DEBUG_MODE_ENABLED = "pref_debug_mode_enabled"

    //  INTENT ACTION CONSTANTS 
    const val ACTION_TRIGGER_FIRED = "com.example.autoflow.TRIGGER_FIRED"
    const val ACTION_WORKFLOW_EXECUTED = "com.example.autoflow.WORKFLOW_EXECUTED"
    const val ACTION_PERMISSION_GRANTED = "com.example.autoflow.PERMISSION_GRANTED"
    const val ACTION_PERMISSION_DENIED = "com.example.autoflow.PERMISSION_DENIED"

    //  INTENT EXTRA KEYS 
    const val EXTRA_WORKFLOW_ID = "extra_workflow_id"
    const val EXTRA_TRIGGER_TYPE = "extra_trigger_type"
    const val EXTRA_TRIGGER_VALUE = "extra_trigger_value"
    const val EXTRA_EXECUTION_SUCCESS = "extra_execution_success"
    const val EXTRA_ERROR_MESSAGE = "extra_error_message"

    //  VALIDATION CONSTANTS 
    const val MAX_WORKFLOW_NAME_LENGTH = 100
    const val MAX_NOTIFICATION_TITLE_LENGTH = 50
    const val MAX_NOTIFICATION_MESSAGE_LENGTH = 200
    const val MAX_SCRIPT_LENGTH = 10000
    const val MIN_TRIGGER_CHECK_INTERVAL_SECONDS = 30
    const val MAX_CONCURRENT_WORKFLOWS = 50

    //  ERROR CODES 
    const val ERROR_CODE_PERMISSION_DENIED = 1001
    const val ERROR_CODE_BLUETOOTH_NOT_AVAILABLE = 1002
    const val ERROR_CODE_LOCATION_NOT_AVAILABLE = 1003
    const val ERROR_CODE_WIFI_NOT_AVAILABLE = 1004
    const val ERROR_CODE_INVALID_TRIGGER_DATA = 1005
    const val ERROR_CODE_INVALID_ACTION_DATA = 1006
    const val ERROR_CODE_WORKFLOW_EXECUTION_FAILED = 1007
    const val ERROR_CODE_DATABASE_ERROR = 1008

    //  SUCCESS CODES 
    const val SUCCESS_CODE_WORKFLOW_CREATED = 2001
    const val SUCCESS_CODE_WORKFLOW_EXECUTED = 2002
    const val SUCCESS_CODE_TRIGGER_REGISTERED = 2003
    const val SUCCESS_CODE_PERMISSIONS_GRANTED = 2004

    //  SCRIPT LANGUAGES 
    const val SCRIPT_LANGUAGE_JAVASCRIPT = "javascript"
    const val SCRIPT_LANGUAGE_SHELL = "shell"
    const val SCRIPT_LANGUAGE_PYTHON = "python"

    // Script execution timeouts (in milliseconds)
    const val SCRIPT_TIMEOUT_SHORT = 5000L   // 5 seconds
    const val SCRIPT_TIMEOUT_MEDIUM = 30000L // 30 seconds
    const val SCRIPT_TIMEOUT_LONG = 60000L   // 1 minute

    //  BATTERY CONSTANTS 
    const val BATTERY_LEVEL_LOW = 20
    const val BATTERY_LEVEL_CRITICAL = 10
    const val BATTERY_LEVEL_FULL = 100

    //  VOLUME CONSTANTS 
    const val VOLUME_TYPE_MEDIA = "media"
    const val VOLUME_TYPE_RING = "ring"
    const val VOLUME_TYPE_ALARM = "alarm"
    const val VOLUME_TYPE_NOTIFICATION = "notification"
    const val VOLUME_TYPE_CALL = "call"

    // Volume levels (0-100)
    const val VOLUME_MIN = 0
    const val VOLUME_MAX = 100
    const val VOLUME_SILENT = 0
    const val VOLUME_LOW = 25
    const val VOLUME_MEDIUM = 50
    const val VOLUME_HIGH = 75
    const val VOLUME_MAXIMUM = 100
}