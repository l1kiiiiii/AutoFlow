// file: TimeUtils.kt in util package
package com.example.autoflow.domain.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object TimeUtils {

    /**
     * Convert LocalDateTime to Unix timestamp in milliseconds
     */
    fun dateTimeToUnixTimestamp(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Convert separate date and time to Unix timestamp
     */
    fun dateAndTimeToUnixTimestamp(date: LocalDate, time: LocalTime): Long {
        val dateTime = LocalDateTime.of(date, time)
        return dateTimeToUnixTimestamp(dateTime)
    }

    /**
     * Convert Unix timestamp to LocalDateTime
     */
    fun unixTimestampToDateTime(timestamp: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
    }

    /**
     * Format LocalDateTime for display
     */
    fun formatDateTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")
        return dateTime.format(formatter)
    }

    /**
     * Format LocalDate for display
     */
    fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return date.format(formatter)
    }

    /**
     * Format LocalTime for display
     */
    fun formatTime(time: LocalTime): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return time.format(formatter)
    }

    /**
     * Check if the selected time is in the future
     */
    fun isFutureTime(dateTime: LocalDateTime): Boolean {
        return dateTime.isAfter(LocalDateTime.now())
    }
}
