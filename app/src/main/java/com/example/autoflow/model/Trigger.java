package com.example.autoflow.model;

import android.util.Patterns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.autoflow.util.Constants;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.regex.Pattern;

public class Trigger {
    private final long id;
    private final long workflowId;
    public final String type;
    public final String value;

    // Validation patterns
    private static final Pattern MAC_ADDRESS_PATTERN =
            Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    private static final Pattern COORDINATES_PATTERN =
            Pattern.compile("^-?\\d+\\.\\d+,-?\\d+\\.\\d+(,\\d+(\\.\\d+)?)?$");

    // Constructor
    public Trigger(long id, long workflowId, @NonNull String type, @NonNull String value) {
        this.id = id;
        this.workflowId = workflowId;
        this.type = type;
        this.value = value;
    }

    // Constructor without ID (for creating new triggers)
    public Trigger(long workflowId, @NonNull String type, @NonNull String value) {
        this(0, workflowId, type, value);
    }

    // Getters
    public long getId() {
        return id;
    }

    public long getWorkflowId() {
        return workflowId;
    }

    @NonNull
    public String getType() {
        return type != null ? type : "";
    }

    @NonNull
    public String getValue() {
        return value != null ? value : "";
    }

    /**
     * Comprehensive trigger validation with enhanced security and error checking
     */
    public boolean isValid() {
        // Basic null and empty checks
        if (type == null || type.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            return false;
        }

        String trimmedType = type.trim();
        String trimmedValue = value.trim();

        switch (trimmedType) {
            case Constants.TRIGGER_TIME:
                return validateTimeTrigger(trimmedValue);

            case Constants.TRIGGER_BLE:
                return validateBleTrigger(trimmedValue);

            case Constants.TRIGGER_LOCATION:
                return validateLocationTrigger(trimmedValue);

            case Constants.TRIGGER_WIFI: // Updated to use consistent constant
                return validateWiFiTrigger(trimmedValue);

            case Constants.TRIGGER_APP_LAUNCH:
                return validateAppLaunchTrigger(trimmedValue);

            case Constants.TRIGGER_BATTERY_LEVEL:
                return validateBatteryLevelTrigger(trimmedValue);

            case Constants.TRIGGER_CHARGING_STATE:
                return validateChargingStateTrigger(trimmedValue);

            case Constants.TRIGGER_HEADPHONE_CONNECTION:
                return validateHeadphoneConnectionTrigger(trimmedValue);

            default:
                return false; // Unknown trigger type
        }
    }

    /**
     * Validate time-based triggers
     */
    private boolean validateTimeTrigger(@NonNull String value) {
        try {
            long timestamp = Long.parseLong(value);
            // Check if timestamp is reasonable (not too far in past/future)
            long currentTime = System.currentTimeMillis();
            long maxFutureTime = currentTime + Constants.MAX_FUTURE_TIME_MS;
            long minPastTime = currentTime - Constants.MAX_FUTURE_TIME_MS;

            return timestamp >= minPastTime && timestamp <= maxFutureTime;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate BLE triggers with enhanced MAC address validation
     */
    private boolean validateBleTrigger(@NonNull String value) {
        // Check if it's a MAC address format
        if (MAC_ADDRESS_PATTERN.matcher(value).matches()) {
            return true;
        }

        // Check if it's a device name (basic validation)
        if (value.length() >= 1 && value.length() <= 248) { // Bluetooth name length limits
            // Ensure it doesn't contain only whitespace
            return !value.trim().isEmpty();
        }

        return false;
    }

    /**
     * Validate location-based triggers with multiple format support
     */
    private boolean validateLocationTrigger(@NonNull String value) {
        // Try JSON format first
        if (validateLocationJson(value)) {
            return true;
        }

        // Try simple coordinate format: "lat,lng" or "lat,lng,radius"
        return validateLocationCoordinates(value);
    }

    /**
     * Validate location JSON format
     */
    private boolean validateLocationJson(@NonNull String value) {
        try {
            JSONObject json = new JSONObject(value);

            // Check for coordinates field
            if (json.has(Constants.JSON_KEY_LOCATION_COORDINATES)) {
                String coordinates = json.getString(Constants.JSON_KEY_LOCATION_COORDINATES);
                return validateLocationCoordinates(coordinates);
            }

            // Check for separate lat/lng fields
            if (json.has("latitude") && json.has("longitude")) {
                double lat = json.getDouble("latitude");
                double lng = json.getDouble("longitude");
                return isValidLatitude(lat) && isValidLongitude(lng);
            }

            return false;
        } catch (JSONException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate coordinate string format
     */
    private boolean validateLocationCoordinates(@NonNull String coordinates) {
        if (!COORDINATES_PATTERN.matcher(coordinates).matches()) {
            return false;
        }

        try {
            String[] parts = coordinates.split(",");
            if (parts.length < 2 || parts.length > 3) {
                return false;
            }

            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());

            if (!isValidLatitude(lat) || !isValidLongitude(lng)) {
                return false;
            }

            // Validate radius if present
            if (parts.length == 3) {
                float radius = Float.parseFloat(parts[2].trim());
                return radius >= Constants.LOCATION_MIN_RADIUS && radius <= Constants.LOCATION_MAX_RADIUS;
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate WiFi triggers with JSON or simple state format
     */
    private boolean validateWiFiTrigger(@NonNull String value) {
        // Try JSON format first
        try {
            JSONObject json = new JSONObject(value);

            // Check for WiFi state
            if (json.has(Constants.JSON_KEY_WIFI_TARGET_STATE)) {
                String state = json.getString(Constants.JSON_KEY_WIFI_TARGET_STATE);
                return isValidWiFiState(state);
            }

            // Check for SSID-based triggers
            if (json.has(Constants.JSON_KEY_WIFI_SSID)) {
                String ssid = json.getString(Constants.JSON_KEY_WIFI_SSID);
                return isValidSSID(ssid);
            }

            return false;
        } catch (JSONException e) {
            // Try simple state format
            return isValidWiFiState(value);
        }
    }

    /**
     * Validate app launch triggers
     */
    private boolean validateAppLaunchTrigger(@NonNull String value) {
        try {
            JSONObject json = new JSONObject(value);

            // Must have package name
            if (!json.has(Constants.JSON_KEY_APP_PACKAGE_NAME)) {
                return false;
            }

            String packageName = json.getString(Constants.JSON_KEY_APP_PACKAGE_NAME);
            return isValidPackageName(packageName);
        } catch (JSONException e) {
            // Try as simple package name
            return isValidPackageName(value);
        }
    }

    /**
     * Validate battery level triggers
     */
    private boolean validateBatteryLevelTrigger(@NonNull String value) {
        try {
            int level = Integer.parseInt(value);
            return level >= 0 && level <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate charging state triggers
     */
    private boolean validateChargingStateTrigger(@NonNull String value) {
        return "CHARGING".equalsIgnoreCase(value) ||
                "NOT_CHARGING".equalsIgnoreCase(value) ||
                "PLUGGED".equalsIgnoreCase(value) ||
                "UNPLUGGED".equalsIgnoreCase(value);
    }

    /**
     * Validate headphone connection triggers
     */
    private boolean validateHeadphoneConnectionTrigger(@NonNull String value) {
        return "CONNECTED".equalsIgnoreCase(value) ||
                "DISCONNECTED".equalsIgnoreCase(value);
    }

    // Helper validation methods
    private boolean isValidLatitude(double lat) {
        return lat >= -90.0 && lat <= 90.0;
    }

    private boolean isValidLongitude(double lng) {
        return lng >= -180.0 && lng <= 180.0;
    }

    private boolean isValidWiFiState(@NonNull String state) {
        return Constants.WIFI_STATE_ON.equalsIgnoreCase(state) ||
                Constants.WIFI_STATE_OFF.equalsIgnoreCase(state) ||
                Constants.WIFI_STATE_CONNECTED.equalsIgnoreCase(state) ||
                Constants.WIFI_STATE_DISCONNECTED.equalsIgnoreCase(state);
    }

    private boolean isValidSSID(@NonNull String ssid) {
        // SSID should be between 1-32 characters
        return ssid.length() >= 1 && ssid.length() <= 32 && !ssid.trim().isEmpty();
    }

    private boolean isValidPackageName(@NonNull String packageName) {
        // Basic package name validation
        return packageName.matches("^[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)*$") &&
                packageName.length() >= 3 && packageName.length() <= 255;
    }

    /**
     * Get validation error message for debugging
     */
    @Nullable
    public String getValidationError() {
        if (type == null || type.trim().isEmpty()) {
            return "Trigger type cannot be empty";
        }

        if (value == null || value.trim().isEmpty()) {
            return "Trigger value cannot be empty";
        }

        String trimmedType = type.trim();
        String trimmedValue = value.trim();

        switch (trimmedType) {
            case Constants.TRIGGER_TIME:
                if (!validateTimeTrigger(trimmedValue)) {
                    return "Invalid timestamp format or value out of range";
                }
                break;

            case Constants.TRIGGER_BLE:
                if (!validateBleTrigger(trimmedValue)) {
                    return "Invalid BLE device address or name format";
                }
                break;

            case Constants.TRIGGER_LOCATION:
                if (!validateLocationTrigger(trimmedValue)) {
                    return "Invalid location format. Use 'lat,lng,radius' or JSON format";
                }
                break;

            case Constants.TRIGGER_WIFI:
                if (!validateWiFiTrigger(trimmedValue)) {
                    return "Invalid WiFi state or SSID format";
                }
                break;

            default:
                return "Unknown trigger type: " + trimmedType;
        }

        return null; // No error
    }

    /**
     * Create a copy of this trigger with updated values
     */
    @NonNull
    public Trigger copyWith(long newId, long newWorkflowId, @Nullable String newType, @Nullable String newValue) {
        return new Trigger(
                newId != 0 ? newId : this.id,
                newWorkflowId != 0 ? newWorkflowId : this.workflowId,
                newType != null ? newType : this.type,
                newValue != null ? newValue : this.value
        );
    }

    @Override
    @NonNull
    public String toString() {
        return "Trigger{" +
                "id=" + id +
                ", workflowId=" + workflowId +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Trigger trigger = (Trigger) obj;
        return id == trigger.id &&
                workflowId == trigger.workflowId &&
                type.equals(trigger.type) &&
                value.equals(trigger.value);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(id);
        result = 31 * result + Long.hashCode(workflowId);
        result = 31 * result + type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
