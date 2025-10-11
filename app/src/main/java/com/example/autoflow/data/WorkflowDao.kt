package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WorkflowDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workflow: WorkflowEntity): Long  //  suspend + non-nullable

    @Update
    suspend fun update(workflow: WorkflowEntity): Int

    @Delete
    suspend fun delete(workflow: WorkflowEntity)

    @Query("DELETE FROM workflows WHERE id = :workflowId")
    suspend fun deleteById(workflowId: Long): Int

    @Query("DELETE FROM workflows")
    suspend fun deleteAll()

    @Query("SELECT * FROM workflows")
    fun getAllWorkflows(): LiveData<List<WorkflowEntity>>  //  LiveData for UI

    @Query("SELECT * FROM workflows")
    suspend fun getAllWorkflowsSync(): List<WorkflowEntity>  //  For background

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    suspend fun getEnabledWorkflows(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE id = :id")
    suspend fun getById(id: Long): WorkflowEntity?  //  Nullable only here

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun getByIdSync(id: Long): WorkflowEntity?  //  For non-suspend calls

    @Query("UPDATE workflows SET is_enabled = :enabled WHERE id = :workflowId")
    suspend fun updateEnabled(workflowId: Long, enabled: Boolean): Int

    @Query("SELECT COUNT(*) FROM workflows")
    suspend fun getCount(): Int
}
