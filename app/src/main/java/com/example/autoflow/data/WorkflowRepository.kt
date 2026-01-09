package com.example.autoflow.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository class for managing WorkflowEntity database operations.
 * Refactored to use Clean Code principles and Kotlin Coroutines.
 *
 * This repository follows modern Android architecture guidelines:
 * - Suspend functions for one-shot operations (insert, update, delete)
 * - LiveData for reactive UI updates
 * - No manual thread management (uses Dispatchers.IO)
 */
class WorkflowRepository(private val workflowDao: WorkflowDao) {

    // ============================================================================================
    // LIVEDATA QUERIES
    // Observed by the UI (ViewModel). Room handles background threading for these automatically.
    // ============================================================================================

    val allWorkflows: LiveData<List<WorkflowEntity>> = workflowDao.getAllWorkflows()
    val enabledWorkflows: LiveData<List<WorkflowEntity>> = workflowDao.getEnabledWorkflows()

    // ============================================================================================
    // SUSPEND FUNCTIONS (The "Kotlin Way")
    // These must be called from a CoroutineScope (e.g., viewModelScope.launch)
    // ============================================================================================

    /**
     * Insert a new workflow.
     */
    suspend fun insert(workflow: WorkflowEntity): Long {
        return withContext(Dispatchers.IO) {
            workflowDao.insert(workflow)
        }
    }

    /**
     * Update an existing workflow.
     */
    suspend fun update(workflow: WorkflowEntity) {
        withContext(Dispatchers.IO) {
            workflowDao.update(workflow)
        }
    }

    /**
     * Toggle a workflow's enabled state efficiently.
     */
    suspend fun updateWorkflowEnabled(workflowId: Long, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            workflowDao.updateEnabled(workflowId, enabled)
        }
    }

    /**
     * Delete a workflow by ID.
     */
    suspend fun delete(workflowId: Long) {
        withContext(Dispatchers.IO) {
            workflowDao.deleteById(workflowId)
        }
    }

    /**
     * Delete a specific workflow entity object.
     */
    suspend fun delete(workflow: WorkflowEntity) {
        withContext(Dispatchers.IO) {
            workflowDao.delete(workflow)
        }
    }

    /**
     * Wipe all workflows from the database.
     */
    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            workflowDao.deleteAll()
        }
    }

    /**
     * Fetch a single workflow by ID.
     */
    suspend fun getWorkflowById(workflowId: Long): WorkflowEntity? {
        return withContext(Dispatchers.IO) {
            workflowDao.getByIdSync(workflowId)
        }
    }

    /**
     * Get total count of workflows.
     */
    suspend fun getWorkflowCount(): Int {
        return withContext(Dispatchers.IO) {
            workflowDao.getCount()
        }
    }

    /**
     * Get all workflows as a list (non-LiveData).
     * Used by ViewModel to load initial data without observing.
     */
    suspend fun getAllWorkflowsList(): List<WorkflowEntity> {
        return withContext(Dispatchers.IO) {
            workflowDao.getAllWorkflowsSync()
        }
    }

    // ============================================================================================
    // SPECIALIZED METHODS
    // ============================================================================================

    /**
     * Get workflow synchronously.
     * ⚠️ ONLY use this in background contexts (like AlarmReceiver) where you can't use coroutines.
     */
    fun getWorkflowEntityForActionExecution(workflowId: Long): WorkflowEntity? {
        return workflowDao.getByIdSync(workflowId)
    }
}