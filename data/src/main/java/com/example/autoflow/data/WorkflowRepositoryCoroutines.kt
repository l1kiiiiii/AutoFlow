package com.example.autoflow.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Modern coroutine-based repository for WorkflowEntity operations
 * Replaces callback-based APIs with suspend functions and Flow
 * Follows clean architecture principles
 */
class WorkflowRepositoryCoroutines(private val workflowDao: WorkflowDao) {
    
    // LiveData queries (for reactive UI)
    val allWorkflows: LiveData<List<WorkflowEntity>> = workflowDao.getAllWorkflows()
    val enabledWorkflows: LiveData<List<WorkflowEntity>> = workflowDao.getEnabledWorkflows()
    
    // ===== INSERT OPERATIONS =====
    
    /**
     * Insert workflow and return its ID
     */
    suspend fun insert(workflow: WorkflowEntity): Long = withContext(Dispatchers.IO) {
        workflowDao.insert(workflow)
    }
    
    // ===== UPDATE OPERATIONS =====
    
    /**
     * Update workflow
     * @return number of rows updated
     */
    suspend fun update(workflow: WorkflowEntity): Int = withContext(Dispatchers.IO) {
        workflowDao.update(workflow)
    }
    
    /**
     * Update workflow enabled state
     * @return number of rows updated
     */
    suspend fun updateEnabled(workflowId: Long, enabled: Boolean): Int = withContext(Dispatchers.IO) {
        workflowDao.updateEnabled(workflowId, enabled)
    }
    
    // ===== DELETE OPERATIONS =====
    
    /**
     * Delete workflow by ID
     * @return number of rows deleted
     */
    suspend fun delete(workflowId: Long): Int = withContext(Dispatchers.IO) {
        workflowDao.deleteById(workflowId)
    }
    
    /**
     * Delete workflow entity
     */
    suspend fun delete(workflow: WorkflowEntity) = withContext(Dispatchers.IO) {
        workflowDao.delete(workflow)
    }
    
    /**
     * Delete all workflows
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        workflowDao.deleteAll()
    }
    
    // ===== QUERY OPERATIONS =====
    
    /**
     * Get all workflows as a one-time query
     */
    suspend fun getAllWorkflowsOnce(): List<WorkflowEntity> = withContext(Dispatchers.IO) {
        workflowDao.getAllWorkflowsSync()
    }
    
    /**
     * Get workflow by ID
     */
    suspend fun getWorkflowById(workflowId: Long): WorkflowEntity? = withContext(Dispatchers.IO) {
        workflowDao.getByIdSync(workflowId)
    }
    
    /**
     * Get enabled workflows as a one-time query
     */
    suspend fun getEnabledWorkflowsOnce(): List<WorkflowEntity> = withContext(Dispatchers.IO) {
        workflowDao.getEnabledWorkflowsSync()
    }
    
    /**
     * Get workflow count
     */
    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        workflowDao.getCount()
    }
    
    /**
     * Search workflows by name
     */
    suspend fun searchWorkflows(query: String): List<WorkflowEntity> = withContext(Dispatchers.IO) {
        workflowDao.search(query)
    }
    
    // ===== FLOW-BASED OPERATIONS =====
    
    /**
     * Get all workflows as a Flow (for one-time or streaming updates)
     */
    fun getAllWorkflowsFlow(): Flow<List<WorkflowEntity>> = flow {
        emit(workflowDao.getAllWorkflowsSync())
    }
    
    /**
     * Get enabled workflows as a Flow
     */
    fun getEnabledWorkflowsFlow(): Flow<List<WorkflowEntity>> = flow {
        emit(workflowDao.getEnabledWorkflowsSync())
    }
}
