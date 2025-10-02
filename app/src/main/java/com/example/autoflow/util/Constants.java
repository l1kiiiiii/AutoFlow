package com.example.autoflow.util;

/**
 * Central constants repository for AutoFlow automation app
 * Organized by category for better maintainability
 */
public class Constants {

    // ========== TRIGGER TYPES ==========
    public static final String TRIGGER_TIME = "TIME";
    public static final String TRIGGER_BLE = "BLE";
    public static final String TRIGGER_LOCATION = "LOCATION";
    public static final String TRIGGER_WIFI = "WIFI"; // Standardized name (was TRIGGER_WIFI_STATE)
    public static final String TRIGGER_APP_LAUNCH = "APP_LAUNCH";
    public static final String TRIGGER_BATTERY_LEVEL = "BATTERY_LEVEL";
    public static final String TRIGGER_CHARGING_STATE = "CHARGING_STATE";
    public static final String TRIGGER_HEADPHONE_CONNECTION = "HEADPHONE_CONNECTION";

    // Action type for sound mode
    public static final String ACTION_SET_SOUND_MODE = "SET_SOUND_MODE";

    // Sound mode values
    public static final String SOUND_MODE_RING = "ring";
    public static final String SOUND_MODE_VIBRATE = "vibrate";
    public static final String SOUND_MODE_SILENT = "silent";

    // DND modes (require notification policy access)
    public static final String SOUND_MODE_DND_NONE = "dnd_none";       // Total silence
    public static final String SOUND_MODE_DND_PRIORITY = "dnd_priority";
    public static final String SOUND_MODE_DND_ALARMS = "dnd_alarms";
    public static final String SOUND_MODE_DND_ALL = "dnd_all";         // Turn off DND


    // ========== ACTION TYPES ==========
    public static final String ACTION_SEND_NOTIFICATION = "SEND_NOTIFICATION";
    public static final String ACTION_TOGGLE_WIFI = "TOGGLE_WIFI";
    public static final String ACTION_TOGGLE_BLUETOOTH = "TOGGLE_BLUETOOTH";
    public static final String ACTION_TOGGLE_SETTINGS = "TOGGLE_SETTINGS";
    public static final String ACTION_RUN_SCRIPT = "RUN_SCRIPT";
    public static final String ACTION_LAUNCH_APP = "LAUNCH_APP";
    public static final String ACTION_SEND_SMS = "SEND_SMS";
    public static final String ACTION_PLAY_SOUND = "PLAY_SOUND";
    public static final String ACTION_SET_VOLUME = "SET_VOLUME";
    public static final String ACTION_TOGGLE_FLASHLIGHT = "TOGGLE_FLASHLIGHT";

    // ========== WORKER INPUT KEYS ==========
    public static final String KEY_WORKFLOW_ID = "workflow_id";
    public static final String KEY_TRIGGER_TYPE = "trigger_type";
    public static final String KEY_TRIGGER_VALUE = "trigger_value";
    public static final String KEY_ACTION_TYPE = "action_type";
    public static final String KEY_ACTION_PARAMETERS = "action_parameters";
    public static final String KEY_BLE_DEVICE_ADDRESS = "ble_device_address";
    public static final String KEY_BLE_DEVICE_NAME = "ble_device_name";
    public static final String KEY_TIME_TRIGGER = "time_trigger";
    public static final String KEY_LOCATION_LAT = "location_latitude";
    public static final String KEY_LOCATION_LNG = "location_longitude";
    public static final String KEY_LOCATION_RADIUS = "location_radius";

    // ========== JSON PARAMETER KEYS ==========
    // Notification parameters
    public static final String JSON_KEY_NOTIFICATION_TITLE = "notificationTitle";
    public static final String JSON_KEY_NOTIFICATION_MESSAGE = "notificationMessage";
    public static final String JSON_KEY_NOTIFICATION_PRIORITY = "notificationPriority";
    public static final String JSON_KEY_NOTIFICATION_CHANNEL_ID = "notificationChannelId";
    public static final String JSON_KEY_NOTIFICATION_ICON = "notificationIcon";

    // WiFi parameters
    public static final String JSON_KEY_WIFI_TARGET_STATE = "wifiTargetState";
    public static final String JSON_KEY_WIFI_SSID = "wifiSsid";
    public static final String JSON_KEY_WIFI_SECURITY_TYPE = "wifiSecurityType";

    // Settings toggle parameters
    public static final String JSON_KEY_SETTING_TO_TOGGLE = "settingToToggle";
    public static final String JSON_KEY_SETTING_TARGET_STATE = "settingTargetState";

    // Script parameters
    public static final String JSON_KEY_SCRIPT_CONTENT = "scriptContent";
    public static final String JSON_KEY_SCRIPT_LANGUAGE = "scriptLanguage";
    public static final String JSON_KEY_SCRIPT_TIMEOUT = "scriptTimeout";

    // App launch parameters
    public static final String JSON_KEY_APP_PACKAGE_NAME = "appPackageName";
    public static final String JSON_KEY_APP_CLASS_NAME = "appClassName";

    // Location parameters
    public static final String JSON_KEY_LOCATION_NAME = "locationName";
    public static final String JSON_KEY_LOCATION_COORDINATES = "locationCoordinates";
    public static final String JSON_KEY_LOCATION_RADIUS = "locationRadius";
    public static final String JSON_KEY_LOCATION_ENTRY_EXIT = "locationEntryExit";

    // ========== NOTIFICATION CONSTANTS ==========
    public static final String NOTIFICATION_CHANNEL_ID_DEFAULT = "autoflow_default";
    public static final String NOTIFICATION_CHANNEL_ID_HIGH_PRIORITY = "autoflow_high_priority";
    public static final String NOTIFICATION_CHANNEL_ID_TRIGGERS = "autoflow_triggers";
    public static final String NOTIFICATION_CHANNEL_NAME_DEFAULT = "AutoFlow Notifications";
    public static final String NOTIFICATION_CHANNEL_NAME_HIGH_PRIORITY = "AutoFlow High Priority";
    public static final String NOTIFICATION_CHANNEL_NAME_TRIGGERS = "AutoFlow Triggers";

    // Notification priorities
    public static final String NOTIFICATION_PRIORITY_LOW = "Low";
    public static final String NOTIFICATION_PRIORITY_NORMAL = "Normal";
    public static final String NOTIFICATION_PRIORITY_HIGH = "High";
    public static final String NOTIFICATION_PRIORITY_MAX = "Max";

    // ========== WIFI CONSTANTS ==========
    public static final String WIFI_STATE_ON = "ON";
    public static final String WIFI_STATE_OFF = "OFF";
    public static final String WIFI_STATE_CONNECTED = "CONNECTED";
    public static final String WIFI_STATE_DISCONNECTED = "DISCONNECTED";

    // ========== BLUETOOTH CONSTANTS ==========
    public static final String BLUETOOTH_STATE_ON = "ON";
    public static final String BLUETOOTH_STATE_OFF = "OFF";
    public static final String BLUETOOTH_STATE_CONNECTED = "CONNECTED";
    public static final String BLUETOOTH_STATE_DISCONNECTED = "DISCONNECTED";

    // ========== LOCATION CONSTANTS ==========
    public static final String LOCATION_TRIGGER_ENTRY = "Entry";
    public static final String LOCATION_TRIGGER_EXIT = "Exit";
    public static final String LOCATION_TRIGGER_BOTH = "Both";

    // Default location radius in meters
    public static final float LOCATION_DEFAULT_RADIUS = 100f;
    public static final float LOCATION_MIN_RADIUS = 20f;
    public static final float LOCATION_MAX_RADIUS = 1000f;

    // ========== TIME CONSTANTS ==========
    // Time window for time-based triggers (1 minute in milliseconds)
    public static final long TIME_WINDOW_MS = 60 * 1000L;
    // Default trigger check interval (5 minutes)
    public static final long TRIGGER_CHECK_INTERVAL_MS = 5 * 60 * 1000L;
    // Maximum allowed future time for triggers (24 hours)
    public static final long MAX_FUTURE_TIME_MS = 24 * 60 * 60 * 1000L;

    // ========== SYSTEM SETTINGS CONSTANTS ==========
    public static final String SETTING_WIFI = "WiFi";
    public static final String SETTING_BLUETOOTH = "Bluetooth";
    public static final String SETTING_LOCATION = "Location";
    public static final String SETTING_AIRPLANE_MODE = "AirplaneMode";
    public static final String SETTING_DO_NOT_DISTURB = "DoNotDisturb";
    public static final String SETTING_AUTO_ROTATE = "AutoRotate";
    public static final String SETTING_FLASHLIGHT = "Flashlight";
    public static final String SETTING_MOBILE_DATA = "MobileData";

    // ========== DATABASE CONSTANTS ==========
    public static final String DATABASE_NAME = "autoflow_database";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_WORKFLOWS = "workflows";

    // ========== SHARED PREFERENCES KEYS ==========
    public static final String PREF_FILE_NAME = "autoflow_preferences";
    public static final String PREF_FIRST_RUN = "pref_first_run";
    public static final String PREF_NOTIFICATIONS_ENABLED = "pref_notifications_enabled";
    public static final String PREF_LOCATION_UPDATES_ENABLED = "pref_location_updates_enabled";
    public static final String PREF_BLUETOOTH_SCAN_ENABLED = "pref_bluetooth_scan_enabled";
    public static final String PREF_TRIGGER_CHECK_INTERVAL = "pref_trigger_check_interval";
    public static final String PREF_DEBUG_MODE_ENABLED = "pref_debug_mode_enabled";

    // ========== INTENT ACTION CONSTANTS ==========
    public static final String ACTION_TRIGGER_FIRED = "com.example.autoflow.TRIGGER_FIRED";
    public static final String ACTION_WORKFLOW_EXECUTED = "com.example.autoflow.WORKFLOW_EXECUTED";
    public static final String ACTION_PERMISSION_GRANTED = "com.example.autoflow.PERMISSION_GRANTED";
    public static final String ACTION_PERMISSION_DENIED = "com.example.autoflow.PERMISSION_DENIED";

    // ========== INTENT EXTRA KEYS ==========
    public static final String EXTRA_WORKFLOW_ID = "extra_workflow_id";
    public static final String EXTRA_TRIGGER_TYPE = "extra_trigger_type";
    public static final String EXTRA_TRIGGER_VALUE = "extra_trigger_value";
    public static final String EXTRA_EXECUTION_SUCCESS = "extra_execution_success";
    public static final String EXTRA_ERROR_MESSAGE = "extra_error_message";

    // ========== VALIDATION CONSTANTS ==========
    public static final int MAX_WORKFLOW_NAME_LENGTH = 100;
    public static final int MAX_NOTIFICATION_TITLE_LENGTH = 50;
    public static final int MAX_NOTIFICATION_MESSAGE_LENGTH = 200;
    public static final int MAX_SCRIPT_LENGTH = 10000;
    public static final int MIN_TRIGGER_CHECK_INTERVAL_SECONDS = 30;
    public static final int MAX_CONCURRENT_WORKFLOWS = 50;

    // ========== ERROR CODES ==========
    public static final int ERROR_CODE_PERMISSION_DENIED = 1001;
    public static final int ERROR_CODE_BLUETOOTH_NOT_AVAILABLE = 1002;
    public static final int ERROR_CODE_LOCATION_NOT_AVAILABLE = 1003;
    public static final int ERROR_CODE_WIFI_NOT_AVAILABLE = 1004;
    public static final int ERROR_CODE_INVALID_TRIGGER_DATA = 1005;
    public static final int ERROR_CODE_INVALID_ACTION_DATA = 1006;
    public static final int ERROR_CODE_WORKFLOW_EXECUTION_FAILED = 1007;
    public static final int ERROR_CODE_DATABASE_ERROR = 1008;

    // ========== SUCCESS CODES ==========
    public static final int SUCCESS_CODE_WORKFLOW_CREATED = 2001;
    public static final int SUCCESS_CODE_WORKFLOW_EXECUTED = 2002;
    public static final int SUCCESS_CODE_TRIGGER_REGISTERED = 2003;
    public static final int SUCCESS_CODE_PERMISSIONS_GRANTED = 2004;

    // ========== SCRIPT LANGUAGES ==========
    public static final String SCRIPT_LANGUAGE_JAVASCRIPT = "javascript";
    public static final String SCRIPT_LANGUAGE_SHELL = "shell";
    public static final String SCRIPT_LANGUAGE_PYTHON = "python";

    // Script execution timeouts (in milliseconds)
    public static final long SCRIPT_TIMEOUT_SHORT = 5000L;   // 5 seconds
    public static final long SCRIPT_TIMEOUT_MEDIUM = 30000L; // 30 seconds
    public static final long SCRIPT_TIMEOUT_LONG = 60000L;   // 1 minute

    // ========== BATTERY CONSTANTS ==========
    public static final int BATTERY_LEVEL_LOW = 20;
    public static final int BATTERY_LEVEL_CRITICAL = 10;
    public static final int BATTERY_LEVEL_FULL = 100;

    // ========== VOLUME CONSTANTS ==========
    public static final String VOLUME_TYPE_MEDIA = "media";
    public static final String VOLUME_TYPE_RING = "ring";
    public static final String VOLUME_TYPE_ALARM = "alarm";
    public static final String VOLUME_TYPE_NOTIFICATION = "notification";
    public static final String VOLUME_TYPE_CALL = "call";

    // Volume levels (0-100)
    public static final int VOLUME_MIN = 0;
    public static final int VOLUME_MAX = 100;
    public static final int VOLUME_SILENT = 0;
    public static final int VOLUME_LOW = 25;
    public static final int VOLUME_MEDIUM = 50;
    public static final int VOLUME_HIGH = 75;
    public static final int VOLUME_MAXIMUM = 100;



    // ========== PRIVATE CONSTRUCTOR ==========
    // Prevent instantiation of utility class
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
