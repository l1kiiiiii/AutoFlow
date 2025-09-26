package com.example.autoflow.data;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List; // Import List

public class WorkflowViewModel extends androidx.lifecycle.ViewModel { // Extend ViewModel
    private final WorkflowRepository repository;
    private final MutableLiveData<List<WorkflowEntity>> workflows; // Changed to MutableLiveData and List

    // Constructor for ViewModel usually doesn't take Application directly unless it's AndroidViewModel
    // For simplicity with existing code, keeping Application but this might need adjustment
    // if using Hilt or other DI, or if it should be an AndroidViewModel.
    public WorkflowViewModel(Application app) { 
        super(); 
        AppDatabase db = AppDatabase.getDatabase(app);
        repository = new WorkflowRepository(db);
        workflows = new MutableLiveData<>(); // Initialize for List
        loadWorkflows();
    }

    private void loadWorkflows() {
        repository.getAllWorkflows(new WorkflowRepository.WorkflowCallback() {
            @Override
            public void onWorkflowsLoaded(List<WorkflowEntity> loadedWorkflows) { // Changed to List
                workflows.postValue(loadedWorkflows); // Post the list
            }
        });
    }

    public LiveData<List<WorkflowEntity>> getWorkflows() { // Changed to return LiveData<List>
        return workflows;
    }

    // Example method to add a workflow via the repository
    public void addWorkflow(WorkflowEntity workflowEntity) {
        repository.insert(workflowEntity);
        // Depending on your Room DAO, LiveData might auto-update.
        // If not, you might need to call loadWorkflows() again or have repository return LiveData directly.
    }
}