package com.example.autoflow.viewmodel;

// Callback interface for workflow operations
public interface WorkflowOperationCallback {
    void onSuccess(String message);

    void onError(String error);
}
