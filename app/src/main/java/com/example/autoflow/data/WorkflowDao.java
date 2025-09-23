package com.example.autoflow.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List; // Import List

@Dao
public interface WorkflowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkflowEntity workflowEntity);

    @Query("SELECT * FROM workflows WHERE id = :id")
    WorkflowEntity getByIdSync(long id);

    // New method to get all workflows as a List (synchronous)
    @Query("SELECT * FROM workflows")
    List<WorkflowEntity> getAllWorkflowsSync();
}