package com.example.autoflow.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkflowRepository(context: Context) {

    private val workflowDao: WorkflowDao = AppDatabase.getDatabase(context).workflowDao()

    companion object {
        private const val TAG = "WorkflowRepository"
    }

    //  LIVEDATA (Automatic background) 

    fun getAllWorkflows(): LiveData<List<WorkflowEntity>> {
        return workflowDao.getAllWorkflows()
    }

    //  SUSPEND FUNCTIONS (For coroutines) 

    suspend fun insert(workflow: WorkflowEntity): Long = withContext(Dispatchers.IO) {
        try {
            val id = workflowDao.insert(workflow)
            Log.d(TAG, "✅ Inserted workflow with ID: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Insert failed", e)
            throw e
        }
    }

    suspend fun update(workflow: WorkflowEntity): Int = withContext(Dispatchers.IO) {
        try {
            val rows = workflowDao.update(workflow)
            Log.d(TAG, "✅ Updated $rows workflow(s)")
            rows
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update failed", e)
            throw e
        }
    }

    suspend fun delete(workflowId: Long): Int = withContext(Dispatchers.IO) {
        try {
            val rows = workflowDao.deleteById(workflowId)
            Log.d(TAG, "✅ Deleted workflow ID: $workflowId")
            rows
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete failed", e)
            throw e
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        try {
            workflowDao.deleteAll()
            Log.d(TAG, "✅ Deleted all workflows")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete all failed", e)
            throw e
        }
    }

    suspend fun getById(id: Long): WorkflowEntity? = withContext(Dispatchers.IO) {
        workflowDao.getById(id)
    }

    suspend fun updateEnabled(workflowId: Long, enabled: Boolean): Int =
        withContext(Dispatchers.IO) {
            try {
                val rows = workflowDao.updateEnabled(workflowId, enabled)
                Log.d(TAG, "✅ Updated enabled for workflow $workflowId: $enabled")
                rows
            } catch (e: Exception) {
                Log.e(TAG, "❌ Update enabled failed", e)
                throw e
            }
        }

    suspend fun getEnabledWorkflows(): List<WorkflowEntity> = withContext(Dispatchers.IO) {
        workflowDao.getEnabledWorkflows()
    }

    suspend fun getCount(): Int = withContext(Dispatchers.IO) {
        workflowDao.getCount()
    }

    //  NON-SUSPEND (For synchronous access) 

    // For GeofenceReceiver and other places that can't use coroutines
    fun getByIdSync(id: Long): WorkflowEntity? {
        return workflowDao.getByIdSync(id)
    }

    //  CALLBACK-BASED (For Java compatibility) 

    fun insertWithCallback(workflow: WorkflowEntity, callback: InsertCallback) {
        CoroutineScope(Dispatchers.IO).launch {  // ✅ Now works!
            try {
                val id = workflowDao.insert(workflow)
                Log.d(TAG, "✅ Inserted workflow with ID: $id")
                withContext(Dispatchers.Main) {
                    callback.onInsertComplete(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Insert failed", e)
                withContext(Dispatchers.Main) {
                    callback.onInsertError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun updateWithCallback(workflow: WorkflowEntity, callback: UpdateCallback?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rows = workflowDao.update(workflow)
                withContext(Dispatchers.Main) {
                    callback?.onUpdateComplete(rows > 0)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onUpdateError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun deleteWithCallback(workflowId: Long, callback: DeleteCallback?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rows = workflowDao.deleteById(workflowId)
                withContext(Dispatchers.Main) {
                    callback?.onDeleteComplete(rows > 0)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onDeleteError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun updateEnabledWithCallback(workflowId: Long, enabled: Boolean, callback: UpdateCallback?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rows = workflowDao.updateEnabled(workflowId, enabled)
                withContext(Dispatchers.Main) {
                    callback?.onUpdateComplete(rows > 0)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onUpdateError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun getAllWorkflowsWithCallback(callback: WorkflowCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val workflows = workflowDao.getAllWorkflowsSync()
                withContext(Dispatchers.Main) {
                    callback.onWorkflowsLoaded(workflows.toMutableList())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onWorkflowsError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun getByIdWithCallback(workflowId: Long, callback: WorkflowByIdCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val workflow = workflowDao.getById(workflowId)
                withContext(Dispatchers.Main) {
                    callback.onWorkflowLoaded(workflow)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onWorkflowError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun getEnabledWorkflowsWithCallback(callback: WorkflowCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val workflows = workflowDao.getEnabledWorkflows()
                withContext(Dispatchers.Main) {
                    callback.onWorkflowsLoaded(workflows.toMutableList())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onWorkflowsError(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun getCountWithCallback(callback: CountCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val count = workflowDao.getCount()
                withContext(Dispatchers.Main) {
                    callback.onCountLoaded(count)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onCountError(e.message ?: "Unknown error")
                }
            }
        }
    }

    //  CALLBACK INTERFACES 

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

    interface WorkflowCallback {
        fun onWorkflowsLoaded(workflows: MutableList<WorkflowEntity>)
        fun onWorkflowsError(error: String) {}
    }

    interface WorkflowByIdCallback {
        fun onWorkflowLoaded(workflow: WorkflowEntity?)
        fun onWorkflowError(error: String) {}
    }

    interface CountCallback {
        fun onCountLoaded(count: Int)
        fun onCountError(error: String) {}
    }
}
