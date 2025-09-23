package com.example.autoflow.model;

import com.example.autoflow.util.Constants;

public class Trigger {
    private long id;
    private long workflowId;
    private String type; // e.g., TIME, BLE, LOCATION
    private String value; // Specific trigger data (e.g., time in millis, BLE device address)

    // Constructor
    public Trigger(long id, long workflowId, String type, String value) {
        this.id = id;
        this.workflowId = workflowId;
        this.type = type;
        this.value = value;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(long workflowId) {
        this.workflowId = workflowId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // Helper method to validate trigger
    public boolean isValid() {
        if (type == null || type.isEmpty()) {
            return false;
        }
        switch (type) {
            case Constants.TRIGGER_TIME:
                try {
                    Long.parseLong(value); // Ensure value is a valid timestamp
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case Constants.TRIGGER_BLE:
                return value != null && !value.isEmpty(); // Ensure BLE device address is non-empty
            case Constants.TRIGGER_LOCATION:
                // Add validation for location (e.g., latitude,longitude format)
                return value != null && value.matches("^-?\\d+\\.\\d+,-?\\d+\\.\\d+$");
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "Trigger{" +
                "id=" + id +
                ", workflowId=" + workflowId +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}