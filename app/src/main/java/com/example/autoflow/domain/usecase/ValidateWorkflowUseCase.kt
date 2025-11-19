package com.example.autoflow.domain.usecase

import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger

/**
 * UseCase for validating workflow data before saving
 * Encapsulates validation business logic
 * Follows Single Responsibility Principle
 */
class ValidateWorkflowUseCase {
    
    /**
     * Validate workflow data
     * @return Result with validation errors or Unit on success
     */
    fun execute(
        workflowName: String,
        triggers: List<Trigger>,
        actions: List<Action>
    ): Result<Unit> {
        // Validate workflow name
        if (workflowName.isBlank()) {
            return Result.failure(ValidationException("Workflow name cannot be empty"))
        }
        
        if (workflowName.length > 100) {
            return Result.failure(ValidationException("Workflow name is too long (max 100 characters)"))
        }
        
        // Validate triggers
        if (triggers.isEmpty()) {
            return Result.failure(ValidationException("At least one trigger is required"))
        }
        
        if (triggers.size > 10) {
            return Result.failure(ValidationException("Too many triggers (max 10)"))
        }
        
        // Validate each trigger
        triggers.forEach { trigger ->
            if (trigger.type.isBlank()) {
                return Result.failure(ValidationException("Trigger type cannot be empty"))
            }
            if (trigger.value.isBlank()) {
                return Result.failure(ValidationException("Trigger value cannot be empty"))
            }
        }
        
        // Validate actions
        if (actions.isEmpty()) {
            return Result.failure(ValidationException("At least one action is required"))
        }
        
        if (actions.size > 10) {
            return Result.failure(ValidationException("Too many actions (max 10)"))
        }
        
        // Validate each action
        actions.forEach { action ->
            if (action.type.isNullOrBlank()) {
                return Result.failure(ValidationException("Action type cannot be empty"))
            }
            
            // Validate notification action
            if (action.type == "NOTIFICATION") {
                if (action.title.isNullOrBlank() && action.message.isNullOrBlank()) {
                    return Result.failure(ValidationException("Notification must have title or message"))
                }
            }
            
            // Validate script action
            if (action.type == "RUN_SCRIPT") {
                if (action.value.isNullOrBlank()) {
                    return Result.failure(ValidationException("Script action must have script content"))
                }
            }
            
            // Validate block apps action
            if (action.type == "BLOCK_APPS") {
                if (action.value.isNullOrBlank()) {
                    return Result.failure(ValidationException("Block apps action must specify apps to block"))
                }
            }
        }
        
        return Result.success(Unit)
    }
}

/**
 * Custom exception for validation errors
 */
class ValidationException(message: String) : Exception(message)
