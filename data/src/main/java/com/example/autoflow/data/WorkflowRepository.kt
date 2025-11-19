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
 * Provides multiple access patterns: callbacks, coroutines, LiveData
 */
@Suppress("unused", "MemberVisibilityCanBePrivate") // Public API for various access patterns
class WorkflowRepository(private val workflowDao: WorkflowDao) {
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "WorkflowRepository"
    }

    // LIVEDATA QUERIES (for reactive UI)
    val allWorkflows: LiveData<List<WorkflowEntity>> = workflowDao.getAllWorkflows()
    val enabledWorkflows: LiveData<List<WorkflowEntity>> = workflowDao.getEnabledWorkflows()

    //  SYNCHRONOUS METHODS (for background threads/receivers) 

    /**
     * Get workflow for action execution (used by AlarmReceiver)
     * Must be called from background thread
     */
    fun getWorkflowEntityForActionExecution(workflowId: Long): WorkflowEntity? {
        return workflowDao.getByIdSync(workflowId)
    }

    //  INSERT OPERATIONS 

    /**
     * Insert workflow without callback (fire and forget)
     */
    fun insert(workflowEntity: WorkflowEntity) {
        executorService.execute {
            try {
                val id = workflowDao.insert(workflowEntity)
                Log.d(TAG, "✅ Inserted workflow with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Insert failed", e)
            }
        }
    }

    /**
     * Insert workflow with callback
     */
    fun insert(workflow: WorkflowEntity, callback: InsertCallback) {
        executorService.execute {
            try {
                val id = workflowDao.insert(workflow)
                Log.d(TAG, "✅ Inserted workflow with ID: $id")
                mainThreadHandler.post {
                    callback.onInsertComplete(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Insert failed", e)
                mainThreadHandler.post {
                    callback.onInsertError(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Insert workflow with coroutine support
     */
    suspend fun insertSuspend(workflow: WorkflowEntity): Long {
        return withContext(Dispatchers.IO) {
            workflowDao.insert(workflow)
        }
    }

    //  UPDATE OPERATIONS 

    /**
     * Update workflow without callback
     */
    fun update(workflowEntity: WorkflowEntity) {
        executorService.execute {
            try {
                workflowDao.update(workflowEntity)
                Log.d(TAG, "✅ Updated workflow ID: ${workflowEntity.id}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Update failed", e)
            }
        }
    }

    /**
     * Update workflow with callback
     */
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

    /**
     * Update workflow enabled state without callback
     */
    fun updateWorkflowEnabled(workflowId: Long, enabled: Boolean) {
        executorService.execute {
            try {
                workflowDao.updateEnabled(workflowId, enabled)
                Log.d(TAG, "✅ Updated workflow $workflowId enabled=$enabled")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Update enabled failed", e)
            }
        }
    }

    /**
     * Update workflow enabled state with callback
     */
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

    /**
     * Update workflow with coroutine support
     */
    suspend fun updateSuspend(workflow: WorkflowEntity): Int {
        return withContext(Dispatchers.IO) {
            workflowDao.update(workflow)
        }
    }

    //  DELETE OPERATIONS 

    /**
     * Delete workflow by ID without callback
     */
    fun delete(workflowId: Long) {
        executorService.execute {
            try {
                workflowDao.deleteById(workflowId)
                Log.d(TAG, "✅ Deleted workflow ID: $workflowId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Delete failed", e)
            }
        }
    }

    /**
     * Delete workflow by ID with callback
     */
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

    /**
     * Delete workflow entity without callback
     */
    fun delete(workflowEntity: WorkflowEntity) {
        executorService.execute {
            try {
                workflowDao.delete(workflowEntity)
                Log.d(TAG, "✅ Deleted workflow ID: ${workflowEntity.id}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Delete failed", e)
            }
        }
    }

    /**
     * Delete all workflows without callback
     */
    fun deleteAll() {
        executorService.execute {
            try {
                workflowDao.deleteAll()
                Log.d(TAG, "✅ Deleted all workflows")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Delete all failed", e)
            }
        }
    }

    /**
     * Delete all workflows with callback
     */
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

    /**
     * Delete workflow with coroutine support
     */
    suspend fun deleteSuspend(workflowId: Long): Int {
        return withContext(Dispatchers.IO) {
            workflowDao.deleteById(workflowId)
        }
    }

    //  QUERY OPERATIONS 

    /**
     * Get all workflows with callback
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

    /**
     * Get workflow by ID with callback
     */
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

    /**
     * Get enabled workflows with callback
     */
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

    /**
     * Get workflow count with callback
     */
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

    /**
     * Cleanup resources
     * Call this when repository is no longer needed
     */
    fun cleanup() {
        if (!executorService.isShutdown) {
            executorService.shutdown()
            Log.d(TAG, "🧹 Repository cleaned up")
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
}
