package com.example.autoflow.util;

public class Constants {
    // Trigger types
    public static final String TRIGGER_TIME = "TIME";
    public static final String TRIGGER_BLE = "BLE";
    public static final String TRIGGER_LOCATION = "LOCATION";

    // Action types
    public static final String ACTION_TOGGLE_WIFI = "TOGGLE_WIFI";
    public static final String ACTION_SEND_NOTIFICATION = "SEND_NOTIFICATION";

    // Worker input keys
    public static final String KEY_BLE_DEVICE_ADDRESS = "ble_device_address";
    public static final String KEY_TIME_TRIGGER = "time_trigger";
    public static final String KEY_WORKFLOW_ID = "workflow_id";

    // JSON Parameter Keys (used within WorkflowEntity.value for action parameters)
    public static final String JSON_KEY_NOTIFICATION_TITLE = "notificationTitle";
    public static final String JSON_KEY_NOTIFICATION_MESSAGE = "notificationMessage";
    public static final String JSON_KEY_NOTIFICATION_PRIORITY = "notificationPriority";
    public static final String JSON_KEY_WIFI_TARGET_STATE = "wifiTargetState"; // Added

    // Time window for time-based triggers (in milliseconds, e.g., 1 minute)
    public static final long TIME_WINDOW_MS = 60 * 1000;
}