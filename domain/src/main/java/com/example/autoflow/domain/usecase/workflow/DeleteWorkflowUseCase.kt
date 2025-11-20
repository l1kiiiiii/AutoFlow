package com.example.autoflow.domain.usecase.workflow

import com.example.autoflow.domain.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for deleting a workflow
 */
class DeleteWorkflowUseCase(private val repository: WorkflowRepository) {
    
    suspend fun execute(workflowId: Long): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (workflowId <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid workflow ID"))
            }
            
            val rowsAffected = repository.delete(workflowId)
            Result.success(rowsAffected)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
