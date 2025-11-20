package com.example.autoflow.domain.usecase.workflow

import com.example.autoflow.domain.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for updating workflow enabled state
 */
class UpdateWorkflowEnabledUseCase(private val repository: WorkflowRepository) {
    
    suspend fun execute(workflowId: Long, enabled: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (workflowId <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid workflow ID: $workflowId"))
            }
            
            val rowsAffected = repository.updateEnabled(workflowId, enabled)
            Result.success(rowsAffected)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
