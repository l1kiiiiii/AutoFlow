package com.example.autoflow.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.util.Log
import org.json.JSONObject

@Entity(tableName = "meeting_modes")
data class MeetingModeEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "name")
    var name: String = "Meeting Mode",

    @ColumnInfo(name = "is_active")
    var isActive: Boolean = false,

    @ColumnInfo(name = "start_type")
    var startType: String = "IMMEDIATE", // IMMEDIATE, SCHEDULED, MANUAL

    @ColumnInfo(name = "scheduled_start_time")
    var scheduledStartTime: Long? = null, // Unix timestamp for scheduled start

    @ColumnInfo(name = "end_type")
    var endType: String = "MANUAL", // MANUAL, DURATION, SCHEDULED_END

    @ColumnInfo(name = "duration_minutes")
    var durationMinutes: Int? = null, // Duration in minutes if end_type = DURATION

    @ColumnInfo(name = "scheduled_end_time")
    var scheduledEndTime: Long? = null, // Unix timestamp for scheduled end

    @ColumnInfo(name = "auto_reply_enabled")
    var autoReplyEnabled: Boolean = true,

    @ColumnInfo(name = "auto_reply_message")
    var autoReplyMessage: String = "I'm currently in a meeting and will get back to you shortly.",

    @ColumnInfo(name = "dnd_enabled")
    var dndEnabled: Boolean = true,

    @ColumnInfo(name = "app_blocking_enabled")
    var appBlockingEnabled: Boolean = false,

    @ColumnInfo(name = "blocked_apps")
    var blockedApps: String = "[]", // JSON array of package names

    @ColumnInfo(name = "created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_started")
    var lastStarted: Long? = null,

    @ColumnInfo(name = "last_ended")
    var lastEnded: Long? = null
) {
    companion object {
        private const val TAG = "MeetingModeEntity"

        // Create immediate meeting mode
        fun createImmediate(
            name: String = "Meeting Mode",
            autoReplyMessage: String = "I'm currently in a meeting and will get back to you shortly.",
            endType: String = "MANUAL",
            durationMinutes: Int? = null
        ): MeetingModeEntity {
            return MeetingModeEntity(
                name = name,
                startType = "IMMEDIATE",
                endType = endType,
                durationMinutes = durationMinutes,
                autoReplyMessage = autoReplyMessage
            )
        }

        // Create scheduled meeting mode
        fun createScheduled(
            name: String = "Scheduled Meeting",
            startTime: Long,
            endType: String = "DURATION",
            durationMinutes: Int? = 60,
            endTime: Long? = null,
            autoReplyMessage: String = "I'm currently in a meeting and will get back to you shortly."
        ): MeetingModeEntity {
            return MeetingModeEntity(
                name = name,
                startType = "SCHEDULED",
                scheduledStartTime = startTime,
                endType = endType,
                durationMinutes = durationMinutes,
                scheduledEndTime = endTime,
                autoReplyMessage = autoReplyMessage
            )
        }
    }

    fun getBlockedAppsList(): List<String> {
        return try {
            val jsonArray = org.json.JSONArray(blockedApps)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing blocked apps: ${e.message}")
            emptyList()
        }
    }

    fun setBlockedAppsList(apps: List<String>) {
        blockedApps = org.json.JSONArray(apps).toString()
    }

    fun getEffectiveEndTime(): Long? {
        return when (endType) {
            "SCHEDULED_END" -> scheduledEndTime
            "DURATION" -> {
                val startTime = lastStarted ?: scheduledStartTime ?: System.currentTimeMillis()
                val duration = durationMinutes ?: return null
                startTime + (duration * 60 * 1000L)
            }
            else -> null // MANUAL end
        }
    }

    fun isExpired(): Boolean {
        if (!isActive) return false
        val endTime = getEffectiveEndTime() ?: return false
        return System.currentTimeMillis() >= endTime
    }
}
