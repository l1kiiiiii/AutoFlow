package com.example.autoflow.domain.trigger

import com.example.autoflow.model.Trigger

/**
 * Strategy interface for trigger evaluation
 * Follows Open/Closed Principle - new triggers can be added without modifying existing code
 */
interface TriggerHandler {
    /**
     * Check if this handler supports the given trigger type
     */
    fun canHandle(trigger: Trigger): Boolean
    
    /**
     * Evaluate if the trigger condition is met
     * @return true if trigger should fire, false otherwise
     */
    suspend fun evaluate(trigger: Trigger): Result<Boolean>
    
    /**
     * Get trigger type this handler supports
     */
    fun getSupportedType(): String
}
