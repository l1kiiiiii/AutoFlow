package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkflowDao {

    //  INSERT 
    @Insert
    fun insert(workflowEntity: WorkflowEntity): Long

    @Insert
    fun insertAll(vararg workflows: WorkflowEntity): List<Long>

    //  UPDATE 
    @Update
    fun update(workflowEntity: WorkflowEntity): Int

    @Query("UPDATE workflows SET is_enabled = :enabled WHERE id = :workflowId")
    fun updateEnabled(workflowId: Long, enabled: Boolean): Int

    //  DELETE 
    @Delete
    fun delete(workflowEntity: WorkflowEntity)

    @Query("DELETE FROM workflows WHERE id = :workflowId")
    fun deleteById(workflowId: Long): Int

    @Query("DELETE FROM workflows")
    fun deleteAll()

    //  SYNCHRONOUS QUERIES (NO @JvmField!) 
    @Query("SELECT * FROM workflows")
    fun getAllWorkflowsSync(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    fun getEnabledWorkflowsSync(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun getByIdSync(id: Long): WorkflowEntity?

    @Query("SELECT COUNT(*) FROM workflows")
    fun getCount(): Int

    //  LIVEDATA QUERIES 
    @Query("SELECT * FROM workflows")
    fun getAllWorkflows(): LiveData<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    fun getEnabledWorkflows(): LiveData<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun getById(id: Long): LiveData<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE workflow_name LIKE :query")
    fun search(query: String): List<WorkflowEntity>
}
