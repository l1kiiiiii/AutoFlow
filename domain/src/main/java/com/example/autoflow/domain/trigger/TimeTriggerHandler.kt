package com.example.autoflow.domain.trigger

import android.content.Context
import com.example.autoflow.model.Trigger
import com.example.autoflow.util.Constants
import com.example.autoflow.util.TriggerParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handler for time-based triggers
 * Follows Single Responsibility Principle
 */
class TimeTriggerHandler(private val context: Context) : TriggerHandler {
    
    override fun canHandle(trigger: Trigger): Boolean {
        return trigger.type == Constants.TRIGGER_TIME
    }
    
    override fun getSupportedType(): String = Constants.TRIGGER_TIME
    
    override suspend fun evaluate(trigger: Trigger): Result<Boolean> = withContext(Dispatchers.Default) {
        try {
            // Parse time data from trigger
            val timeData = TriggerParser.parseTimeData(trigger)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid time data"))
            
            val (targetTime, days) = timeData
            val currentTime = System.currentTimeMillis()
            
            // Parse time string
            val timeParts = targetTime.split(":")
            if (timeParts.size != 2) {
                return@withContext Result.failure(IllegalArgumentException("Invalid time format"))
            }
            
            val targetHour = timeParts[0].toIntOrNull() ?: 0
            val targetMinute = timeParts[1].toIntOrNull() ?: 0
            
            // Build target time
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            calendar.set(java.util.Calendar.MINUTE, targetMinute)
            calendar.set(java.util.Calendar.SECOND, 0)
            
            val targetTimeMs = calendar.timeInMillis
            
            // Check if within time window
            val isTriggered = currentTime >= targetTimeMs &&
                    (currentTime - targetTimeMs) <= Constants.TIME_WINDOW_MS
            
            Result.success(isTriggered)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
