package com.example.autoflow.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List; // Import List

@Dao
public interface WorkflowDao {

    @Insert
    long insert(WorkflowEntity workflowEntity);

    @Update
    int update(WorkflowEntity workflowEntity);

    @Delete
    void delete(WorkflowEntity workflowEntity);

    @Query("DELETE FROM workflows WHERE id = :workflowId")
    int deleteById(long workflowId);

    @Query("DELETE FROM workflows")
    void deleteAll();

    @Query("SELECT * FROM workflows")
    List<WorkflowEntity> getAllWorkflowsSync();

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    List<WorkflowEntity> getEnabledWorkflowsSync();

    @Query("SELECT * FROM workflows WHERE id = :id")
    WorkflowEntity getByIdSync(long id);

    @Query("UPDATE workflows SET is_enabled = :enabled WHERE id = :workflowId")
    int updateEnabled(long workflowId, boolean enabled);

    @Query("SELECT COUNT(*) FROM workflows")
    int getCount();

    // LiveData versions for reactive programming (optional)
    @Query("SELECT * FROM workflows")
    LiveData<List<WorkflowEntity>> getAllWorkflows();

    @Query("SELECT * FROM workflows WHERE is_enabled = 1")
    LiveData<List<WorkflowEntity>> getEnabledWorkflows();

    @Query("SELECT * FROM workflows WHERE id = :id")
    LiveData<WorkflowEntity> getById(long id);

}
