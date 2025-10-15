package com.example.autoflow.model

import java.util.Date

/**
 * Represents in-app notifications and task execution history
 */
data class AppNotification(
    val id: String = System.currentTimeMillis().toString(),
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Date = Date(),
    val isRead: Boolean = false,
    val isClearable: Boolean = true,
    val actionData: Map<String, String>? = null
)

enum class NotificationType {
    TASK_EXECUTED,
    SMS_SENT,
    MEETING_MODE,
    WORKFLOW_TRIGGERED,
    ERROR,
    SUCCESS,
    INFO
}
