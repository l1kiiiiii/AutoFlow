package com.example.autoflow.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.autoflow.data.WorkflowDao
import com.example.autoflow.data.mapper.WorkflowMapper
import com.example.autoflow.domain.model.Workflow
import com.example.autoflow.domain.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of WorkflowRepository interface
 * Handles data operations and maps between domain and data models
 */
class WorkflowRepositoryImpl(
    private val workflowDao: WorkflowDao
) : WorkflowRepository {
    
    override val allWorkflows: LiveData<List<Workflow>> = 
        workflowDao.getAllWorkflows().map { entities ->
            entities.map { WorkflowMapper.toDomain(it) }
        }
    
    override val enabledWorkflows: LiveData<List<Workflow>> = 
        workflowDao.getEnabledWorkflows().map { entities ->
            entities.map { WorkflowMapper.toDomain(it) }
        }
    
    override suspend fun insert(workflow: Workflow): Long {
        return withContext(Dispatchers.IO) {
            val entity = WorkflowMapper.toEntity(workflow)
            workflowDao.insert(entity)
        }
    }
    
    override suspend fun update(workflow: Workflow): Int {
        return withContext(Dispatchers.IO) {
            val entity = WorkflowMapper.toEntity(workflow)
            workflowDao.update(entity)
        }
    }
    
    override suspend fun delete(workflowId: Long): Int {
        return withContext(Dispatchers.IO) {
            workflowDao.deleteById(workflowId)
        }
    }
    
    override suspend fun getById(workflowId: Long): Workflow? {
        return withContext(Dispatchers.IO) {
            val entity = workflowDao.getByIdSync(workflowId)
            entity?.let { WorkflowMapper.toDomain(it) }
        }
    }
    
    override suspend fun getAllWorkflows(): List<Workflow> {
        return withContext(Dispatchers.IO) {
            val entities = workflowDao.getAllWorkflowsSync()
            entities.map { WorkflowMapper.toDomain(it) }
        }
    }
    
    override suspend fun getEnabledWorkflows(): List<Workflow> {
        return withContext(Dispatchers.IO) {
            val entities = workflowDao.getEnabledWorkflowsSync()
            entities.map { WorkflowMapper.toDomain(it) }
        }
    }
    
    override suspend fun updateEnabled(workflowId: Long, enabled: Boolean): Int {
        return withContext(Dispatchers.IO) {
            workflowDao.updateEnabled(workflowId, enabled)
        }
    }
    
    override suspend fun deleteAll() {
        return withContext(Dispatchers.IO) {
            workflowDao.deleteAll()
        }
    }
    
    override suspend fun getCount(): Int {
        return withContext(Dispatchers.IO) {
            workflowDao.getCount()
        }
    }
}
