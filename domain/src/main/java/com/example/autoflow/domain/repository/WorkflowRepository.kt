package com.example.autoflow.domain.repository


import androidx.lifecycle.LiveData
import com.example.autoflow.domain.model.Workflow

/**
 * Repository interface for Workflow operations
 * This interface belongs to the domain layer and defines the contract
 * that the data layer must implement
 */
interface WorkflowRepository {
    
    // LiveData queries for reactive UI
    val allWorkflows: LiveData<List<Workflow>>
    val enabledWorkflows: LiveData<List<Workflow>>
    
    // Suspend functions for coroutine support
    suspend fun insert(workflow: Workflow): Long
    suspend fun update(workflow: Workflow): Int
    suspend fun delete(workflowId: Long): Int
    suspend fun getById(workflowId: Long): Workflow?
    suspend fun getAllWorkflows(): List<Workflow>
    suspend fun getEnabledWorkflows(): List<Workflow>
    suspend fun updateEnabled(workflowId: Long, enabled: Boolean): Int
    suspend fun deleteAll()
    suspend fun getCount(): Int
}
