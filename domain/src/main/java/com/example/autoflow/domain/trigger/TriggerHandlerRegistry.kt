package com.example.autoflow.domain.trigger

import com.example.autoflow.domain.model.Trigger

/**
 * Registry for managing trigger handlers
 * Follows Open/Closed Principle - new handlers can be registered without modifying this class
 */
class TriggerHandlerRegistry {
    private val handlers = mutableMapOf<String, TriggerHandler>()
    
    /**
     * Register a trigger handler
     */
    fun registerHandler(handler: TriggerHandler) {
        handlers[handler.getSupportedType()] = handler
    }
    
    /**
     * Get handler for a specific trigger
     */
    fun getHandler(trigger: Trigger): TriggerHandler? {
        return handlers[trigger.type]
    }
    
    /**
     * Evaluate a trigger using the appropriate handler
     */
    suspend fun evaluateTrigger(trigger: Trigger): Result<Boolean> {
        val handler = getHandler(trigger)
            ?: return Result.failure(IllegalArgumentException("No handler found for trigger type: ${trigger.type}"))
        
        return handler.evaluate(trigger)
    }
    
    /**
     * Get all registered handlers
     */
    fun getAllHandlers(): List<TriggerHandler> = handlers.values.toList()
}
