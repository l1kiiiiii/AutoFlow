package com.example.autoflow.domain.usecase.workflow

import com.example.autoflow.domain.model.Workflow
import com.example.autoflow.domain.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for retrieving all workflows
 */
class GetWorkflowsUseCase(private val repository: WorkflowRepository) {
    
    suspend fun execute(): Result<List<Workflow>> = withContext(Dispatchers.IO) {
        try {
            val workflows = repository.getAllWorkflows()
            Result.success(workflows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getEnabled(): Result<List<Workflow>> = withContext(Dispatchers.IO) {
        try {
            val workflows = repository.getEnabledWorkflows()
            Result.success(workflows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
