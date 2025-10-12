package com.example.autoflow.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Repository class for managing WorkflowEntity database operations
 * Provides async operations with callbacks for UI integration
 */
class WorkflowRepository(private val database: AppDatabase) {

    private val workflowDao: WorkflowDao = database.workflowDao()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    //  LIVEDATA QUERIES 

    val allWorkflows: LiveData<List<WorkflowEntity>> = workflowDao.getAllWorkflows()
    val enabledWorkflows: LiveData<List<WorkflowEntity>> = workflowDao.getEnabledWorkflows()

    //  SYNCHRONOUS METHODS 

    /**
     * Get a single workflow entity for action execution (synchronous)
     */
    fun getWorkflowEntityForActionExecution(workflowId: Long): WorkflowEntity? {
        return workflowDao.getByIdSync(workflowId)
    }

    //  INSERT OPERATIONS 

    fun insert(workflowEntity: WorkflowEntity) {
        executorService.execute {
            workflowDao.insert(workflowEntity)
        }
    }

    fun insert(workflow: WorkflowEntity, callback: InsertCallback) {
        Thread {
            try {
                val id = workflowDao.insert(workflow)
                Log.d("WorkflowRepository", "✅ Inserted workflow with ID: $id")
                mainThreadHandler.post {
                    callback.onInsertComplete(id)
                }
            } catch (e: Exception) {
                Log.e("WorkflowRepository", "❌ Insert failed", e)
                mainThreadHandler.post {
                    callback.onInsertError(e.message ?: "Unknown error")
                }
            }
        }.start()
    }

    //  UPDATE OPERATIONS 

    fun update(workflowEntity: WorkflowEntity) {
        executorService.execute {
            workflowDao.update(workflowEntity)
        }
    }

    fun update(workflow: WorkflowEntity, callback: UpdateCallback?) {
        executorService.execute {
            try {
                val rowsUpdated = workflowDao.update(workflow)
                callback?.let {
                    mainThreadHandler.post {
                        it.onUpdateComplete(rowsUpdated > 0)
                    }
                }
            } catch (e: Exception) {
                callback?.let {
                    mainThreadHandler.post {
                        it.onUpdateError(e.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    fun updateWorkflowEnabled(workflowId: Long, enabled: Boolean) {
        executorService.execute {
            workflowDao.updateEnabled(workflowId, enabled)
        }
    }

    fun updateWorkflowEnabled(workflowId: Long, enabled: Boolean, callback: UpdateCallback?) {
        executorService.execute {
            try {
                val updatedRows = workflowDao.updateEnabled(workflowId, enabled)
                callback?.let {
                    mainThreadHandler.post {
                        it.onUpdateComplete(updatedRows > 0)
                    }
                }
            } catch (e: Exception) {
                callback?.let {
                    mainThreadHandler.post {
                        it.onUpdateError(e.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    //  DELETE OPERATIONS 

    fun delete(workflowId: Long) {
        executorService.execute {
            workflowDao.deleteById(workflowId)
        }
    }

    fun delete(workflowId: Long, callback: DeleteCallback?) {
        executorService.execute {
            try {
                val deletedRows = workflowDao.deleteById(workflowId)
                callback?.let {
                    mainThreadHandler.post {
                        it.onDeleteComplete(deletedRows > 0)
                    }
                }
            } catch (e: Exception) {
                callback?.let {
                    mainThreadHandler.post {
                        it.onDeleteError(e.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    fun delete(workflowEntity: WorkflowEntity) {
        executorService.execute {
            workflowDao.delete(workflowEntity)
        }
    }

    fun deleteAll() {
        executorService.execute {
            workflowDao.deleteAll()
        }
    }

    fun deleteAll(callback: DeleteCallback?) {
        executorService.execute {
            try {
                workflowDao.deleteAll()
                callback?.let {
                    mainThreadHandler.post {
                        it.onDeleteComplete(true)
                    }
                }
            } catch (e: Exception) {
                callback?.let {
                    mainThreadHandler.post {
                        it.onDeleteError(e.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    //  QUERY OPERATIONS 

    /**
     * Get all workflows with callback
     * ✅ FIXED: Use getAllWorkflowsSync() function instead of property
     */
    fun getAllWorkflows(callback: WorkflowCallback) {
        executorService.execute {
            try {
                val workflowsList = workflowDao.getAllWorkflowsSync()
                mainThreadHandler.post {
                    callback.onWorkflowsLoaded(workflowsList.toMutableList())
                }
            } catch (e: Exception) {
                mainThreadHandler.post {
                    callback.onWorkflowsError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun getWorkflowById(workflowId: Long, callback: WorkflowByIdCallback) {
        executorService.execute {
            try {
                val workflow = workflowDao.getByIdSync(workflowId)
                mainThreadHandler.post {
                    callback.onWorkflowLoaded(workflow)
                }
            } catch (e: Exception) {
                mainThreadHandler.post {
                    callback.onWorkflowError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun getEnabledWorkflows(callback: WorkflowCallback) {
        executorService.execute {
            try {
                val workflowsList = workflowDao.getEnabledWorkflowsSync()
                mainThreadHandler.post {
                    callback.onWorkflowsLoaded(workflowsList.toMutableList())
                }
            } catch (e: Exception) {
                mainThreadHandler.post {
                    callback.onWorkflowsError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun getWorkflowCount(callback: CountCallback) {
        executorService.execute {
            try {
                val count = workflowDao.getCount()
                mainThreadHandler.post {
                    callback.onCountLoaded(count)
                }
            } catch (e: Exception) {
                mainThreadHandler.post {
                    callback.onCountError(e.message ?: "Unknown error")
                }
            }
        }
    }

    //  CLEANUP 

    fun cleanup() {
        if (!executorService.isShutdown) {
            executorService.shutdown()
        }
    }

    //  CALLBACK INTERFACES 

    interface WorkflowCallback {
        fun onWorkflowsLoaded(workflows: MutableList<WorkflowEntity>)
        fun onWorkflowsError(error: String) {}
    }

    interface WorkflowByIdCallback {
        fun onWorkflowLoaded(workflow: WorkflowEntity?)
        fun onWorkflowError(error: String) {}
    }

    interface InsertCallback {
        fun onInsertComplete(insertedId: Long)
        fun onInsertError(error: String)
    }

    interface UpdateCallback {
        fun onUpdateComplete(success: Boolean)
        fun onUpdateError(error: String) {}
    }

    interface DeleteCallback {
        fun onDeleteComplete(success: Boolean)
        fun onDeleteError(error: String) {}
    }

    interface CountCallback {
        fun onCountLoaded(count: Int)
        fun onCountError(error: String) {}
    }

    suspend fun insertSuspend(workflow: WorkflowEntity): Long {
        return withContext(Dispatchers.IO) {
            workflowDao.insert(workflow)
        }
    }

}
