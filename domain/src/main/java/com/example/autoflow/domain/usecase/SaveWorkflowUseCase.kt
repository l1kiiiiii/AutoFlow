package com.example.autoflow.domain.usecase

import com.example.autoflow.domain.model.Action
import com.example.autoflow.domain.model.Trigger
import com.example.autoflow.domain.model.Workflow
import com.example.autoflow.domain.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase for saving workflows to the database
 * Encapsulates business logic for workflow creation
 * Follows Single Responsibility Principle
 */
class SaveWorkflowUseCase(private val repository: WorkflowRepository) {
    
    /**
     * Save a workflow with triggers and actions
     * @return Workflow ID on success, null on failure
     */
    suspend fun execute(
        workflowName: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        triggerLogic: String = "AND"
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Validate inputs
            if (workflowName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Workflow name cannot be empty"))
            }
            
            if (triggers.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("At least one trigger is required"))
            }
            
            if (actions.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("At least one action is required"))
            }
            
            // Create domain workflow model
            val workflow = Workflow(
                workflowName = workflowName.trim(),
                triggers = triggers,
                actions = actions,
                triggerLogic = triggerLogic,
                isEnabled = true
            )
            
            // Save to database via repository
            val workflowId = repository.insert(workflow)
            Result.success(workflowId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
