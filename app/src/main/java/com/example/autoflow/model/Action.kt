package com.example.autoflow.model

class Action {
    @JvmField
    var type: String?

    @JvmField
    var title: String?

    @JvmField
    var message: String?

    @JvmField
    var priority: String?

    @JvmField
    var value: String?

    @JvmField
    var duration: Long?

    @JvmField
    var scheduledUnblockTime: Long?

    // Constructor for notification actions
    constructor(type: String?, title: String?, message: String?, priority: String?) {
        this.type = type
        this.title = title
        this.message = message
        this.priority = priority
        this.value = null
        this.duration = null //
        this.scheduledUnblockTime = null //
    }

    // Constructor for simple actions (only type)
    constructor(type: String?) {
        this.type = type
        this.title = null
        this.message = null
        this.priority = "Normal"
        this.value = null
        this.duration = null //
        this.scheduledUnblockTime = null //
    }
}
