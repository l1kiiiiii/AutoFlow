package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WorkflowDao {
    // ✅ INSERT METHODS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(workflowEntity: WorkflowEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(workflow: WorkflowEntity): Long

    // ✅ UPDATE METHODS
    @Update
    fun update(workflowEntity: WorkflowEntity): Int

    @Update
    fun updateSync(workflow: WorkflowEntity)

    @Query("UPDATE workflows SET is_enabled = :enabled WHERE id = :workflowId")
    fun updateEnabled(workflowId: Long, enabled: Boolean): Int

    @Query("UPDATE workflows SET is_enabled = :enabled WHERE id = :workflowId")
    fun updateEnabledSync(workflowId: Long, enabled: Boolean)

    // ✅ DELETE METHODS
    @Delete
    fun delete(workflowEntity: WorkflowEntity)

    @Query("DELETE FROM workflows WHERE id = :workflowId")
    fun deleteById(workflowId: Long): Int

    @Query("DELETE FROM workflows WHERE id = :workflowId")
    fun deleteByIdSync(workflowId: Long)

    @Query("DELETE FROM workflows")
    fun deleteAll()

    // ✅ SYNCHRONOUS QUERIES
    @Query("SELECT * FROM workflows ORDER BY created_at DESC")
    fun getAllWorkflowsSync(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    fun getEnabledWorkflowsSync(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun getByIdSync(id: Long): WorkflowEntity?

    @Query("SELECT COUNT(*) FROM workflows")
    fun getCount(): Int

    @Query("SELECT * FROM workflows")
    fun getAllSyncBlocking(): List<WorkflowEntity>

    // ✅ SUSPEND QUERIES (for coroutines)
    @Query("SELECT * FROM workflows")
    suspend fun getAllSync(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    suspend fun getAllEnabledSync(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    suspend fun getActiveWorkflows(): List<WorkflowEntity>

    // ✅ LIVEDATA QUERIES (for UI observations)
    @Query("SELECT * FROM workflows ORDER BY created_at DESC")
    fun getAll(): LiveData<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows ORDER BY created_at DESC")
    fun getAllWorkflows(): LiveData<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    fun getEnabledWorkflows(): LiveData<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun getById(id: Long): LiveData<WorkflowEntity?>

    // ✅ SEARCH QUERIES
    @Query("SELECT * FROM workflows WHERE workflow_name LIKE '%' || :query || '%'")
    fun search(query: String): List<WorkflowEntity>
}
