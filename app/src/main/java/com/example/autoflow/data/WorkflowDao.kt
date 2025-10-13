package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkflowDao {
    // INSERT
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workflowEntity: WorkflowEntity): Long  // ✅ Made suspend for coroutines

    @Insert
    fun insertAll(vararg workflows: WorkflowEntity): List<Long>

    // UPDATE
    @Update
    suspend fun update(workflowEntity: WorkflowEntity): Int  // ✅ Made suspend

    // ✅ FIXED: Use camelCase property name, not snake_case SQL column name
    @Query("UPDATE workflows SET isEnabled = :enabled WHERE id = :workflowId")
    suspend fun updateEnabled(workflowId: Long, enabled: Boolean): Int

    // DELETE
    @Delete
    suspend fun delete(workflowEntity: WorkflowEntity)  // ✅ Made suspend

    @Query("DELETE FROM workflows WHERE id = :workflowId")
    suspend fun deleteById(workflowId: Long): Int

    @Query("DELETE FROM workflows")
    suspend fun deleteAll()

    // SYNCHRONOUS QUERIES
    @Query("SELECT * FROM workflows")
    fun getAllWorkflowsSync(): List<WorkflowEntity>

    // ✅ FIXED: Use camelCase property name
    @Query("SELECT * FROM workflows WHERE isEnabled = 1")
    fun getEnabledWorkflowsSync(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun getByIdSync(id: Long): WorkflowEntity?

    @Query("SELECT COUNT(*) FROM workflows")
    fun getCount(): Int

    // LIVEDATA QUERIES
    @Query("SELECT * FROM workflows")
    fun getAllWorkflows(): LiveData<List<WorkflowEntity>>

    // ✅ FIXED: Use camelCase property name
    @Query("SELECT * FROM workflows WHERE isEnabled = 1")
    fun getEnabledWorkflows(): LiveData<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun getById(id: Long): LiveData<WorkflowEntity>

    // ✅ FIXED: Use camelCase property name
    @Query("SELECT * FROM workflows WHERE workflowName LIKE :query")
    fun search(query: String): List<WorkflowEntity>
}
