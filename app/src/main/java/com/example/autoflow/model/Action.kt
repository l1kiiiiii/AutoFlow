package com.example.autoflow.model;

public class Action {
    public String type;
    public String title;    // For notification action
    public String message;  // For notification action
    public String priority; // For notification action
    public String value;    // Generic value for other parameters (e.g., Wi-Fi state "On"/"Off")

    // Constructor for notification actions
    public Action(String type, String title, String message, String priority) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.priority = priority;
        this.value = null; // Initialize value
    }

    // Constructor for simple actions (only type)
    public Action(String type) {
        this.type = type;
        this.title = null;
        this.message = null;
        this.priority = "Normal"; // Default priority as String
        this.value = null; // Initialize value
    }

    // Getters
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getPriority() { return priority; }
    public String getValue() { return value; } // Getter for value
    public void setType(String type) { this.type = type; }

    // Setter for value (to be used by workers if needed)
    public void setValue(String value) {
        this.value = value;
    }
}