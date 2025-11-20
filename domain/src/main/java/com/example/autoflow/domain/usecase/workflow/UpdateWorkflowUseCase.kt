package com.example.autoflow.domain.usecase.workflow

import com.example.autoflow.domain.model.Action
import com.example.autoflow.domain.model.Trigger
import com.example.autoflow.domain.model.Workflow
import com.example.autoflow.domain.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for updating an existing workflow
 */
class UpdateWorkflowUseCase(private val repository: WorkflowRepository) {
    
    suspend fun execute(
        workflowId: Long,
        workflowName: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        triggerLogic: String = "AND"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Validate inputs
            if (workflowId <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid workflow ID: $workflowId"))
            }
            
            if (workflowName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Workflow name cannot be empty"))
            }
            
            if (triggers.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("At least one trigger is required"))
            }
            
            if (actions.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("At least one action is required"))
            }
            
            // Get existing workflow
            val existingWorkflow = repository.getById(workflowId)
                ?: return@withContext Result.failure(IllegalArgumentException("Workflow not found"))
            
            // Update workflow
            val updatedWorkflow = existingWorkflow.copy(
                workflowName = workflowName.trim(),
                triggers = triggers,
                actions = actions,
                triggerLogic = triggerLogic,
                updatedAt = System.currentTimeMillis()
            )
            
            val rowsAffected = repository.update(updatedWorkflow)
            Result.success(rowsAffected)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
