package com.example.autoflow.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.autoflow.data.AppDatabase;
import com.example.autoflow.data.WorkflowEntity;
import com.example.autoflow.data.WorkflowRepository;
import com.example.autoflow.integrations.BLEManager;
import com.example.autoflow.integrations.LocationManager;
import com.example.autoflow.integrations.WiFiManager;
import com.example.autoflow.model.Action;
import com.example.autoflow.model.Trigger;
import com.example.autoflow.util.Constants;
import com.example.autoflow.util.PermissionUtils;
import java.util.List; // Import List

public class WorkflowViewModel extends AndroidViewModel {
    private final WorkflowRepository repository;
    private final MutableLiveData<List<WorkflowEntity>> workflows; // Changed to List
    private final BLEManager bleManager;
    private final LocationManager locationManager;
    private final WiFiManager wifiManager;

    public WorkflowViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        repository = new WorkflowRepository(db);
        workflows = new MutableLiveData<>(); // Initialize for List
        bleManager = new BLEManager(application);
        locationManager = new LocationManager(application);
        wifiManager = new WiFiManager(application);
        loadWorkflows();
    }

    private void loadWorkflows() {
        // Repository's getAllWorkflows now uses a callback expecting List<WorkflowEntity>
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

    public void checkTrigger(Trigger trigger, TriggerCallback callback) {
        switch (trigger.getType()) {
            case Constants.TRIGGER_BLE:
                if (PermissionUtils.hasBluetoothPermissions(getApplication())) {
                    bleManager.startScanning(address -> callback.onTriggerFired(trigger, address.equals(trigger.getValue())));
                } else {
                    // callback.onTriggerFired(trigger, false);
                }
                break;
            case Constants.TRIGGER_LOCATION:
                if (PermissionUtils.hasLocationPermissions(getApplication())) {
                    locationManager.getLastLocation(location -> callback.onTriggerFired(trigger, isInRange(location, trigger.getValue())));
                } else {
                    // callback.onTriggerFired(trigger, false);
                }
                break;
            case Constants.TRIGGER_TIME:
                try {
                    long targetTime = Long.parseLong(trigger.getValue());
                    long currentTime = System.currentTimeMillis();
                    callback.onTriggerFired(trigger, currentTime >= targetTime && Math.abs(currentTime - targetTime) <= Constants.TIME_WINDOW_MS);
                } catch (NumberFormatException e) {
                    callback.onTriggerFired(trigger, false);
                }
                break;
        }
    }

    private boolean isInRange(android.location.Location location, String value) {
        if (location == null || value == null || value.isEmpty()) return false;
        return true; // Implement geofencing logic
    }

    public interface TriggerCallback {
        void onTriggerFired(Trigger trigger, boolean isFired);
    }

    public void stopAllTriggers() {
        if (PermissionUtils.hasBluetoothPermissions(getApplication())) {
            bleManager.stopScanning();
        }
        wifiManager.stopMonitoring();
    }

    public void addWorkflow(Trigger trigger, Action action) {
        if (trigger == null || !trigger.isValid() || action == null) return;
        WorkflowEntity workflow = WorkflowEntity.fromTriggerAndAction(
                "Workflow_" + System.currentTimeMillis(),
                true,
                trigger,
                action
        );
        repository.insert(workflow);
        loadWorkflows(); // Reload workflows after adding a new one
    }
}