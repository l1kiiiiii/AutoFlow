package com.example.autoflow.data;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkflowRepository {
    private final WorkflowDao workflowDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public WorkflowRepository(@NonNull AppDatabase database) {
        workflowDao = database.workflowDao();

    }

    /**
     * Get a single workflow entity for action execution (synchronous)
     * Used by WorkManager or other background services
     */
    @Nullable
    public WorkflowEntity getWorkflowEntityForActionExecution(long workflowId) {
        return workflowDao.getByIdSync(workflowId);
    }

    /**
     * Insert workflow entity without callback
     */
    public void insert(@NonNull final WorkflowEntity workflowEntity) {
        executorService.execute(() -> workflowDao.insert(workflowEntity));
    }

    /**
     * Insert workflow entity with callback (required by WorkflowViewModel)
     */
    public void insert(@NonNull final WorkflowEntity workflowEntity, @Nullable final InsertCallback callback) {
        executorService.execute(() -> {
            try {
                long insertedId = workflowDao.insert(workflowEntity);
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onInsertComplete(insertedId));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainThreadHandler.post(() -> callback.onInsertError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Update workflow entity
     */
    public void update(@NonNull final WorkflowEntity workflowEntity) {
        executorService.execute(() -> workflowDao.update(workflowEntity));
    }

    /**
     * Update workflow entity with callback
     */
    public void update(@NonNull final WorkflowEntity workflowEntity, @Nullable final UpdateCallback callback) {
        executorService.execute(() -> {
            try {
                int updatedRows = workflowDao.update(workflowEntity);
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

    /**
     * Delete workflow by ID without callback
     */
    public void delete(final long workflowId) {
        executorService.execute(() -> workflowDao.deleteById(workflowId));
    }

    /**
     * Delete workflow by ID with callback (required by WorkflowViewModel)
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
     * Delete workflow entity
     */
    public void delete(@NonNull final WorkflowEntity workflowEntity) {
        executorService.execute(() -> workflowDao.delete(workflowEntity));
    }

    /**
     * Update workflow enabled state (required by WorkflowViewModel)
     */
    public void updateWorkflowEnabled(final long workflowId, final boolean enabled) {
        executorService.execute(() -> workflowDao.updateEnabled(workflowId, enabled));
    }

    /**
     * Update workflow enabled state with callback (required by WorkflowViewModel)
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

    /**
     * Get all workflows with callback
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
     * Delete all workflows
     */
    public void deleteAll() {
        executorService.execute(() -> workflowDao.deleteAll());
    }

    /**
     * Delete all workflows with callback
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

    /**
     * Get workflow count
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
        void onWorkflowsLoaded(@NonNull List<WorkflowEntity> workflows);
        default void onWorkflowsError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for single workflow operations (required by WorkflowViewModel)
     */
    public interface WorkflowByIdCallback {
        void onWorkflowLoaded(@Nullable WorkflowEntity workflow);
        default void onWorkflowError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for insert operations (required by WorkflowViewModel)
     */
    public interface InsertCallback {
        void onInsertComplete(long insertedId);
        default void onInsertError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for update operations (required by WorkflowViewModel)
     */
    public interface UpdateCallback {
        void onUpdateComplete(boolean success);
        default void onUpdateError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for delete operations (required by WorkflowViewModel)
     */
    public interface DeleteCallback {
        void onDeleteComplete(boolean success);
        default void onDeleteError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }

    /**
     * Callback interface for count operations
     */
    public interface CountCallback {
        void onCountLoaded(int count);
        default void onCountError(@NonNull String error) {
            // Default implementation for backward compatibility
        }
    }
}
