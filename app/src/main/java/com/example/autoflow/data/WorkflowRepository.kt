package com.example.autoflow.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository class for managing WorkflowEntity database operations
 * Provides async operations with callbacks for UI integration
 */
public class WorkflowRepository {
    private final WorkflowDao workflowDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Constructor
     * @param database The app database instance
     */
    public WorkflowRepository(@NonNull AppDatabase database) {
        workflowDao = database.workflowDao();
    }

    // ========== SYNCHRONOUS METHODS ==========

    /**
     * Get a single workflow entity for action execution (synchronous)
     * Used by WorkManager or other background services
     * @param workflowId The workflow ID to retrieve
     * @return WorkflowEntity or null if not found
     */
    @Nullable
    public WorkflowEntity getWorkflowEntityForActionExecution(long workflowId) {
        return workflowDao.getByIdSync(workflowId);
    }

    // ========== INSERT OPERATIONS ==========

    /**
     * Insert workflow entity without callback
     * @param workflowEntity The workflow to insert
     */
    public void insert(@NonNull final WorkflowEntity workflowEntity) {
        executorService.execute(() -> workflowDao.insert(workflowEntity));
    }

    /**
     * Insert workflow entity with callback (required by WorkflowViewModel)
     * @param workflow The workflow to insert
     * @param callback Callback to receive insert result
     */
    public void insert(WorkflowEntity workflow, InsertCallback callback) {
        new Thread(() -> {
            try {
                long id = workflowDao.insert(workflow);
                Log.d("WorkflowRepository", "✅ Inserted workflow with ID: " + id);
                callback.onInsertComplete(id);
            } catch (Exception e) {
                Log.e("WorkflowRepository", "❌ Insert failed", e);
                callback.onInsertError(e.getMessage());
            }
        }).start();
    }

    // ========== UPDATE OPERATIONS ==========

    /**
     * Update workflow entity without callback
     * @param workflowEntity The workflow to update
     */
    public void update(@NonNull final WorkflowEntity workflowEntity) {
        executorService.execute(() -> workflowDao.update(workflowEntity));
    }

    /**
     * Update workflow entity with callback
     * @param workflow The workflow to update
     * @param callback Callback to receive update result
     */
    public void update(@NonNull WorkflowEntity workflow, @Nullable UpdateCallback callback) {
        executorService.execute(() -> {
            try {
                int rowsUpdated = workflowDao.update(workflow);
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onUpdateComplete(rowsUpdated > 0));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onUpdateError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Update workflow enabled state without callback
     * @param workflowId The workflow ID to update
     * @param enabled Whether to enable or disable the workflow
     */
    public void updateWorkflowEnabled(final long workflowId, final boolean enabled) {
        executorService.execute(() -> workflowDao.updateEnabled(workflowId, enabled));
    }

    /**
     * Update workflow enabled state with callback (required by WorkflowViewModel)
     * @param workflowId The workflow ID to update
     * @param enabled Whether to enable or disable the workflow
     * @param callback Callback to receive update result
     */
    public void updateWorkflowEnabled(final long workflowId, final boolean enabled,
                                      @Nullable final UpdateCallback callback) {
        executorService.execute(() -> {
            try {
                int updatedRows = workflowDao.updateEnabled(workflowId, enabled);
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onUpdateComplete(updatedRows > 0));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onUpdateError(e.getMessage()));
                }
            }
        });
    }

    // ========== DELETE OPERATIONS ==========

    /**
     * Delete workflow by ID without callback
     * @param workflowId The workflow ID to delete
     */
    public void delete(final long workflowId) {
        executorService.execute(() -> workflowDao.deleteById(workflowId));
    }

    /**
     * Delete workflow by ID with callback (required by WorkflowViewModel)
     * @param workflowId The workflow ID to delete
     * @param callback Callback to receive delete result
     */
    public void delete(final long workflowId, @Nullable final DeleteCallback callback) {
        executorService.execute(() -> {
            try {
                int deletedRows = workflowDao.deleteById(workflowId);
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onDeleteComplete(deletedRows > 0));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onDeleteError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Delete workflow entity without callback
     * @param workflowEntity The workflow to delete
     */
    public void delete(@NonNull final WorkflowEntity workflowEntity) {
        executorService.execute(() -> workflowDao.delete(workflowEntity));
    }

    /**
     * Delete all workflows without callback
     */
    public void deleteAll() {
        executorService.execute(() -> workflowDao.deleteAll());
    }

    /**
     * Delete all workflows with callback
     * @param callback Callback to receive delete result
     */
    public void deleteAll(@Nullable final DeleteCallback callback) {
        executorService.execute(() -> {
            try {
                workflowDao.deleteAll();
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onDeleteComplete(true));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onDeleteError(e.getMessage()));
                }
            }
        });
    }

    // ========== QUERY OPERATIONS ==========

    /**
     * Get all workflows with callback
     * @param callback Callback to receive workflows list
     */
    public void getAllWorkflows(@NonNull final WorkflowCallback callback) {
        executorService.execute(() -> {
            try {
                final List<WorkflowEntity> workflowsList = workflowDao.getAllWorkflowsSync();
                mainThreadHandler.post(() -> callback.onWorkflowsLoaded(workflowsList));
            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onWorkflowsError(e.getMessage()));
            }
        });
    }

    /**
     * Get workflow by ID with callback (required by WorkflowViewModel)
     * @param workflowId The workflow ID to retrieve
     * @param callback Callback to receive workflow
     */
    public void getWorkflowById(final long workflowId, @NonNull final WorkflowByIdCallback callback) {
        executorService.execute(() -> {
            try {
                final WorkflowEntity workflow = workflowDao.getByIdSync(workflowId);
                mainThreadHandler.post(() -> callback.onWorkflowLoaded(workflow));
            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onWorkflowError(e.getMessage()));
            }
        });
    }

    /**
     * Get all enabled workflows
     * @param callback Callback to receive enabled workflows list
     */
    public void getEnabledWorkflows(@NonNull final WorkflowCallback callback) {
        executorService.execute(() -> {
            try {
                final List<WorkflowEntity> workflowsList = workflowDao.getEnabledWorkflowsSync();
                mainThreadHandler.post(() -> callback.onWorkflowsLoaded(workflowsList));
            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onWorkflowsError(e.getMessage()));
            }
        });
    }

    /**
     * Get workflow count
     * @param callback Callback to receive count
     */
    public void getWorkflowCount(@NonNull final CountCallback callback) {
        executorService.execute(() -> {
            try {
                final int count = workflowDao.getCount();
                mainThreadHandler.post(() -> callback.onCountLoaded(count));
            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onCountError(e.getMessage()));
            }
        });
    }

    // ========== CLEANUP ==========

    /**
     * Cleanup resources when repository is no longer needed
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // ========== CALLBACK INTERFACES ==========

    /**
     * Callback interface for workflow list operations
     */
    public interface WorkflowCallback {
        /**
         * Called when workflows are loaded successfully
         * @param workflows List of loaded workflows
         */
        void onWorkflowsLoaded(@NonNull List<WorkflowEntity> workflows);

        /**
         * Called when workflow loading fails
         * @param error Error message
         */
        default void onWorkflowsError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for single workflow operations (required by WorkflowViewModel)
     */
    public interface WorkflowByIdCallback {
        /**
         * Called when workflow is loaded successfully
         * @param workflow The loaded workflow, or null if not found
         */
        void onWorkflowLoaded(@Nullable WorkflowEntity workflow);

        /**
         * Called when workflow loading fails
         * @param error Error message
         */
        default void onWorkflowError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for insert operations (required by WorkflowViewModel)
     */
    public interface InsertCallback {
        /**
         * Called when insert completes successfully
         * @param insertedId The ID of the inserted workflow
         */
        void onInsertComplete(long insertedId);

        /**
         * Called when insert fails
         * @param error Error message
         */
        void onInsertError(@NonNull String error);
    }

    /**
     * Callback interface for update operations (required by WorkflowViewModel)
     */
    public interface UpdateCallback {
        /**
         * Called when update completes
         * @param success Whether the update was successful
         */
        void onUpdateComplete(boolean success);

        /**
         * Called when update fails
         * @param error Error message
         */
        default void onUpdateError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for delete operations (required by WorkflowViewModel)
     */
    public interface DeleteCallback {
        /**
         * Called when delete completes
         * @param success Whether the delete was successful
         */
        void onDeleteComplete(boolean success);

        /**
         * Called when delete fails
         * @param error Error message
         */
        default void onDeleteError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for count operations
     */
    public interface CountCallback {
        /**
         * Called when count is loaded successfully
         * @param count The workflow count
         */
        void onCountLoaded(int count);

        /**
         * Called when count loading fails
         * @param error Error message
         */
        default void onCountError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }
}
