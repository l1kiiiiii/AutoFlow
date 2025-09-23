package com.example.autoflow.data;

import android.os.Handler;
import android.os.Looper;
import java.util.List; // Import List
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkflowRepository {
    private final WorkflowDao workflowDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public WorkflowRepository(AppDatabase database) {
        workflowDao = database.workflowDao();
    }

    public WorkflowEntity getWorkflowEntityForActionExecution(long workflowId) {
        // This method likely remains the same if it's for a single entity for worker execution
        return workflowDao.getByIdSync(workflowId);
    }

    public void insert(final WorkflowEntity workflowEntity) {
        executorService.execute(() -> workflowDao.insert(workflowEntity));
    }

    // Updated to use List<WorkflowEntity>
    public void getAllWorkflows(final WorkflowCallback callback) {
        executorService.execute(() -> {
            // Assume workflowDao will have a method like getAllWorkflowsSync()
            // that returns List<WorkflowEntity>. We will define this in WorkflowDao next.
            final List<WorkflowEntity> workflowsList = workflowDao.getAllWorkflowsSync(); // Changed to List
            mainThreadHandler.post(() -> callback.onWorkflowsLoaded(workflowsList));
        });
    }

    // Updated callback interface
    public interface WorkflowCallback {
        void onWorkflowsLoaded(List<WorkflowEntity> workflows); // Changed to List
    }
}