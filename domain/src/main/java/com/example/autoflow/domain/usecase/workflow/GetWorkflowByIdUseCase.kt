package com.example.autoflow.domain.usecase.workflow

import com.example.autoflow.domain.model.Workflow
import com.example.autoflow.domain.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for retrieving a workflow by ID
 */
class GetWorkflowByIdUseCase(private val repository: WorkflowRepository) {
    
    suspend fun execute(workflowId: Long): Result<Workflow?> = withContext(Dispatchers.IO) {
        try {
            if (workflowId <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid workflow ID"))
            }
            
            val workflow = repository.getById(workflowId)
            Result.success(workflow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
