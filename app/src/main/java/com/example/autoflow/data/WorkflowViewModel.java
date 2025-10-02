package com.example.autoflow.data;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.autoflow.model.Action;
import com.example.autoflow.model.Trigger;

import org.json.JSONObject;

import java.util.List;

public class WorkflowViewModel extends AndroidViewModel {
    private final WorkflowRepository repository;
    private final MutableLiveData<List<WorkflowEntity>> workflows;

    // Add these missing LiveData fields
    private final MutableLiveData<String> successMessage;
    private final MutableLiveData<String> errorMessage;

    public WorkflowViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        repository = new WorkflowRepository(db);
        workflows = new MutableLiveData<>();
        successMessage = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        loadWorkflows();
    }

    // Getters for LiveData
    public LiveData<List<WorkflowEntity>> getWorkflows() {
        return workflows;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Load all workflows
    private void loadWorkflows() {
        repository.getAllWorkflows(new WorkflowRepository.WorkflowCallback() {
            @Override
            public void onWorkflowsLoaded(List<WorkflowEntity> loadedWorkflows) {
                workflows.postValue(loadedWorkflows);
            }
        });
    }

    // Add a new workflow
    public void addWorkflow(@NonNull Trigger trigger,
                            @NonNull Action action,
                            WorkflowOperationCallback callback) {
        try {
            // Create workflow entity
            String workflowName = "Workflow_" + System.currentTimeMillis();
            WorkflowEntity workflow = WorkflowEntity.fromTriggerAndAction(
                    workflowName,
                    true,
                    trigger,
                    action
            );

            repository.insert(workflow, new WorkflowRepository.InsertCallback() {
                @Override
                public void onInsertComplete(long insertedId) {
                    loadWorkflows();
                    successMessage.postValue("Workflow created with ID: " + insertedId);
                    if (callback != null) {
                        callback.onSuccess("Workflow created successfully with ID: " + insertedId);
                    }
                }

                @Override
                public void onInsertError(@NonNull String error) {
                    errorMessage.postValue("Failed to create workflow: " + error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            });
        } catch (Exception e) {
            String error = "Error creating workflow: " + e.getMessage();
            errorMessage.postValue(error);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }

    // Delete a workflow
    public void deleteWorkflow(long workflowId) {   
        repository.delete(workflowId, new WorkflowRepository.DeleteCallback() {
            @Override
            public void onDeleteComplete(boolean success) {
                if (success) {
                    loadWorkflows();
                    successMessage.postValue("Workflow deleted successfully");
                }
            }

            @Override
            public void onDeleteError(@NonNull String error) {
                errorMessage.postValue("Failed to delete workflow: " + error);
            }
        });
    }

    // Update workflow enabled state
    public void updateWorkflowEnabled(long workflowId, boolean enabled) {
        repository.updateWorkflowEnabled(workflowId, enabled, new WorkflowRepository.UpdateCallback() {
            @Override
            public void onUpdateComplete(boolean success) {
                if (success) {
                    loadWorkflows();
                }
            }

            @Override
            public void onUpdateError(@NonNull String error) {
                errorMessage.postValue("Failed to update workflow: " + error);
            }
        });
    }

    // Update an existing workflow
    public void updateWorkflow(long workflowId,
                               String workflowName,
                               Trigger trigger,
                               Action action,
                               WorkflowOperationCallback callback) {
        try {
            // Convert Trigger to JSON
            JSONObject triggerJson = new JSONObject();
            triggerJson.put("type", trigger.type);
            triggerJson.put("value", trigger.value);
            String triggerDetails = triggerJson.toString();

            // Convert Action to JSON
            JSONObject actionJson = new JSONObject();
            actionJson.put("type", action.type);
            if (action.title != null) actionJson.put("title", action.title);
            if (action.message != null) actionJson.put("message", action.message);
            if (action.priority != null) actionJson.put("priority", action.priority);
            if (action.value != null) actionJson.put("value", action.value);
            String actionDetails = actionJson.toString();

            // Get existing workflow and update it
            repository.getWorkflowById(workflowId, new WorkflowRepository.WorkflowByIdCallback() {
                @Override
                public void onWorkflowLoaded(@androidx.annotation.Nullable WorkflowEntity existingWorkflow) {
                    if (existingWorkflow != null) {
                        // Update the fields
                        existingWorkflow.setWorkflowName(workflowName);
                        existingWorkflow.setTriggerDetails(triggerDetails);
                        existingWorkflow.setActionDetails(actionDetails);
                        existingWorkflow.setEnabled(true);

                        // Save updated entity
                        repository.update(existingWorkflow, new WorkflowRepository.UpdateCallback() {
                            @Override
                            public void onUpdateComplete(boolean success) {
                                if (success) {
                                    loadWorkflows();
                                    successMessage.postValue("Workflow updated successfully");
                                    if (callback != null) {
                                        callback.onSuccess("Workflow updated successfully");
                                    }
                                }
                            }

                            @Override
                            public void onUpdateError(@NonNull String error) {
                                errorMessage.postValue("Failed to update workflow: " + error);
                                if (callback != null) {
                                    callback.onError(error);
                                }
                            }
                        });
                    } else {
                        String error = "Workflow not found";
                        errorMessage.postValue(error);
                        if (callback != null) {
                            callback.onError(error);
                        }
                    }
                }

                @Override
                public void onWorkflowError(@NonNull String error) {
                    errorMessage.postValue("Error loading workflow: " + error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            });
        } catch (Exception e) {
            String error = "Error updating workflow: " + e.getMessage();
            errorMessage.postValue(error);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }


    // Callback interface for workflow operations
    public interface WorkflowOperationCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
