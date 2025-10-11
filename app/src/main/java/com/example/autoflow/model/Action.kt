package com.example.autoflow.model

class Action {
    // Getters
    @JvmField
    var type: String?
    @JvmField
    var title: String? // For notification action
    @JvmField
    var message: String? // For notification action
    @JvmField
    var priority: String? // For notification action

    // Setter for value (to be used by workers if needed)
    // Getter for value
    @JvmField
    var value: String? // Generic value for other parameters (e.g., Wi-Fi state "On"/"Off")

    // Constructor for notification actions
    constructor(type: String?, title: String?, message: String?, priority: String?) {
        this.type = type
        this.title = title
        this.message = message
        this.priority = priority
        this.value = null // Initialize value
    }

    // Constructor for simple actions (only type)
    constructor(type: String?) {
        this.type = type
        this.title = null
        this.message = null
        this.priority = "Normal" // Default priority as String
        this.value = null // Initialize value
    }
}