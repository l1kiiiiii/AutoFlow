package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ COMPLETE FIXED WorkflowRepository
 */
class WorkflowRepository(private val workflowDao: WorkflowDao) {

    fun getAllWorkflows(callback: WorkflowCallback) {
        try {
            val workflows: LiveData<List<WorkflowEntity>> = workflowDao.getAll()

            // ✅ FIXED: Create proper Observer implementation
            val observer = object : Observer<List<WorkflowEntity>> {
                override fun onChanged(value: List<WorkflowEntity>) {
                    if (value.isNotEmpty()) {
                        callback.onWorkflowsLoaded(value.toMutableList())
                        workflows.removeObserver(this)
                    } else {
                        callback.onWorkflowsError("No workflows found")
                        workflows.removeObserver(this)
                    }
                }
            }

            workflows.observeForever(observer)
        } catch (e: Exception) {
            callback.onWorkflowsError(e.message ?: "Unknown error")
        }
    }

    fun getWorkflowById(workflowId: Long, callback: WorkflowByIdCallback) {
        try {
            val workflow: LiveData<WorkflowEntity?> = workflowDao.getById(workflowId)

            // ✅ FIXED: Create proper Observer implementation
            val observer = object : Observer<WorkflowEntity?> {
                override fun onChanged(value: WorkflowEntity?) {
                    callback.onWorkflowLoaded(value)
                    workflow.removeObserver(this)
                }
            }

            workflow.observeForever(observer)
        } catch (e: Exception) {
            callback.onWorkflowError(e.message ?: "Unknown error")
        }
    }

    fun insert(workflow: WorkflowEntity, callback: InsertCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val insertedId: Long = workflowDao.insertSync(workflow)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onInsertComplete(insertedId)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onInsertError(e.message ?: "Insert failed")
                }
            }
        }
    }

    fun updateWorkflow(workflow: WorkflowEntity, callback: UpdateCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                workflowDao.updateSync(workflow)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onUpdateComplete(true)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onUpdateError(e.message ?: "Update failed")
                }
            }
        }
    }

    fun updateWorkflowEnabled(workflowId: Long, enabled: Boolean, callback: UpdateCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                workflowDao.updateEnabledSync(workflowId, enabled)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onUpdateComplete(true)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onUpdateError(e.message ?: "Update failed")
                }
            }
        }
    }

    fun deleteWorkflow(workflowId: Long, callback: DeleteCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                workflowDao.deleteByIdSync(workflowId)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onDeleteComplete()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onDeleteError(e.message ?: "Delete failed")
                }
            }
        }
    }

    fun cleanup() {
        // Cleanup resources if needed
    }

    // ✅ CALLBACK INTERFACES
    interface WorkflowCallback {
        fun onWorkflowsLoaded(workflows: MutableList<WorkflowEntity>)
        fun onWorkflowsError(error: String)
    }

    interface WorkflowByIdCallback {
        fun onWorkflowLoaded(workflow: WorkflowEntity?)
        fun onWorkflowError(error: String)
    }

    interface InsertCallback {
        fun onInsertComplete(insertedId: Long)
        fun onInsertError(error: String)
    }

    interface UpdateCallback {
        fun onUpdateComplete(success: Boolean)
        fun onUpdateError(error: String)
    }

    interface DeleteCallback {
        fun onDeleteComplete()
        fun onDeleteError(error: String)
    }
}
