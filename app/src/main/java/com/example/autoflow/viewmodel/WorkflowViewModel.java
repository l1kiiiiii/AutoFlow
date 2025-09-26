package com.example.autoflow.viewmodel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
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
import java.util.List;

public class WorkflowViewModel extends AndroidViewModel {
    private final WorkflowRepository repository;
    private final MutableLiveData<List<WorkflowEntity>> workflows;
    private final MutableLiveData<String> errorMessage;
    private final BLEManager bleManager;
    private final LocationManager locationManager;
    private final WiFiManager wifiManager;

    public WorkflowViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        repository = new WorkflowRepository(db);
        workflows = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        bleManager = new BLEManager(application);
        locationManager = new LocationManager(application);
        wifiManager = new WiFiManager(application);
        loadWorkflows();
    }

    private void loadWorkflows() {
        repository.getAllWorkflows(new WorkflowRepository.WorkflowCallback() {
            @Override
            public void onWorkflowsLoaded(@NonNull List<WorkflowEntity> loadedWorkflows) {
                workflows.postValue(loadedWorkflows);
            }

            @Override
            public void onWorkflowsError(@NonNull String error) {
                errorMessage.postValue("Failed to load workflows: " + error);
            }
        });
    }

    public LiveData<List<WorkflowEntity>> getWorkflows() {
        return workflows;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public void checkTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (trigger == null || callback == null) {
            return;
        }

        switch (trigger.getType()) {
            case Constants.TRIGGER_BLE:
                handleBleTrigger(trigger, callback);
                break;

            case Constants.TRIGGER_LOCATION:
                handleLocationTrigger(trigger, callback);
                break;

            case Constants.TRIGGER_TIME:
                handleTimeTrigger(trigger, callback);
                break;

            case Constants.TRIGGER_WIFI:
                handleWiFiTrigger(trigger, callback);
                break;

            default:
                callback.onTriggerFired(trigger, false);
                break;
        }
    }

    /**
     * Handle BLE triggers with proper permission checks and SecurityException handling
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    private void handleBleTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (!PermissionUtils.hasBluetoothPermissions(getApplication())) {
            callback.onTriggerFired(trigger, false);
            return;
        }

        try {
            bleManager.startScanning(new BLEManager.BLECallback() {
                @Override
                public void onDeviceDetected(@NonNull String deviceAddress, @NonNull String deviceName) {
                    boolean matched = deviceAddress.equals(trigger.getValue()) ||
                            deviceName.equals(trigger.getValue());
                    callback.onTriggerFired(trigger, matched);
                }

                @Override
                public void onScanStarted() {
                    // Optional: Handle scan start event if needed
                }

                @Override
                public void onScanStopped() {
                    // Optional: Handle scan stop event if needed
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    callback.onTriggerFired(trigger, false);
                }
            });
        } catch (SecurityException e) {
            errorMessage.postValue("BLE scanning permission denied: " + e.getMessage());
            callback.onTriggerFired(trigger, false);
        } catch (Exception e) {
            errorMessage.postValue("BLE scanning error: " + e.getMessage());
            callback.onTriggerFired(trigger, false);
        }
    }

    /**
     * Handle location-based triggers with permission checks
     * Fixed lambda parameter type mismatch
     */
    private void handleLocationTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (!PermissionUtils.hasLocationPermissions(getApplication())) {
            callback.onTriggerFired(trigger, false);
            return;
        }

        try {
            // Fixed: Use proper LocationCallback interface that matches the expected parameter types
            locationManager.getLastLocation(new LocationManager.LocationCallback() {
                @Override
                public void onLocationReceived(@NonNull Location location) {
                    boolean inRange = isInRange(location, trigger.getValue());
                    callback.onTriggerFired(trigger, inRange);
                }

                @Override
                public void onLocationError(@NonNull String errorMessage) {
                    WorkflowViewModel.this.errorMessage.postValue("Location error: " + errorMessage);
                    callback.onTriggerFired(trigger, false);
                }

                @Override
                public void onPermissionDenied() {
                    WorkflowViewModel.this.errorMessage.postValue("Location permission denied");
                    callback.onTriggerFired(trigger, false);
                }
            });
        } catch (SecurityException e) {
            errorMessage.postValue("Location permission denied: " + e.getMessage());
            callback.onTriggerFired(trigger, false);
        } catch (Exception e) {
            errorMessage.postValue("Location error: " + e.getMessage());
            callback.onTriggerFired(trigger, false);
        }
    }

    /**
     * Handle time-based triggers with better error handling
     */
    private void handleTimeTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        try {
            if (trigger.getValue() == null || trigger.getValue().trim().isEmpty()) {
                callback.onTriggerFired(trigger, false);
                return;
            }

            long targetTime = Long.parseLong(trigger.getValue().trim());
            long currentTime = System.currentTimeMillis();
            boolean isTriggered = currentTime >= targetTime &&
                    Math.abs(currentTime - targetTime) <= Constants.TIME_WINDOW_MS;
            callback.onTriggerFired(trigger, isTriggered);
        } catch (NumberFormatException e) {
            errorMessage.postValue("Invalid time format in trigger: " + e.getMessage());
            callback.onTriggerFired(trigger, false);
        }
    }

    /**
     * Handle WiFi-based triggers with proper permission checks
     */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    private void handleWiFiTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (!hasWiFiPermissions()) {
            callback.onTriggerFired(trigger, false);
            return;
        }

        try {
            boolean wifiState = wifiManager.isWiFiEnabled();
            boolean expectedState = Constants.WIFI_STATE_ON.equalsIgnoreCase(trigger.getValue());
            callback.onTriggerFired(trigger, wifiState == expectedState);
        } catch (SecurityException e) {
            errorMessage.postValue("WiFi permission denied: " + e.getMessage());
            callback.onTriggerFired(trigger, false);
        } catch (Exception e) {
            errorMessage.postValue("WiFi error: " + e.getMessage());
            callback.onTriggerFired(trigger, false);
        }
    }

    /**
     * Check if WiFi permissions are available
     */
    private boolean hasWiFiPermissions() {
        return ActivityCompat.checkSelfPermission(getApplication(),
                Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if current location is within range of target location
     * Fixed parameter type - now accepts proper Location type
     */
    private boolean isInRange(@NonNull Location location, @Nullable String value) {
        if (location == null || value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            // Parse location string format: "lat,lng,radius"
            String[] parts = value.split(",");
            if (parts.length < 2) {
                return false;
            }

            double targetLat = Double.parseDouble(parts[0].trim());
            double targetLng = Double.parseDouble(parts[1].trim());
            float radius = parts.length > 2 ? Float.parseFloat(parts[2].trim()) : Constants.LOCATION_DEFAULT_RADIUS;

            // Calculate distance between current and target location
            float[] results = new float[1];
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    targetLat, targetLng, results
            );

            return results[0] <= radius;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            errorMessage.postValue("Invalid location format: " + e.getMessage());
            return false;
        }
    }

    /**
     * Interface for trigger callback - this IS a functional interface
     */
    public interface TriggerCallback {
        void onTriggerFired(@NonNull Trigger trigger, boolean isFired);
    }

    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public void stopAllTriggers() {
        if (PermissionUtils.hasBluetoothPermissions(getApplication())) {
            try {
                bleManager.stopScanning();
            } catch (SecurityException e) {
                errorMessage.postValue("Failed to stop BLE scanning: " + e.getMessage());
            } catch (Exception e) {
                errorMessage.postValue("Error stopping BLE scanning: " + e.getMessage());
            }
        }

        if (wifiManager != null) {
            try {
                wifiManager.stopMonitoring();
            } catch (Exception e) {
                errorMessage.postValue("Error stopping WiFi monitoring: " + e.getMessage());
            }
        }
    }

    /**
     * Add workflow with validation and error handling
     */
    public void addWorkflow(@NonNull Trigger trigger, @NonNull Action action) {
        addWorkflow(trigger, action, null);
    }

    /**
     * Add workflow with validation and callback
     */
    public void addWorkflow(@NonNull Trigger trigger, @NonNull Action action, @Nullable WorkflowOperationCallback callback) {
        if (trigger == null || action == null) {
            if (callback != null) {
                callback.onError("Trigger and Action cannot be null");
            }
            return;
        }

        if (!trigger.isValid()) {
            if (callback != null) {
                callback.onError("Invalid trigger configuration");
            }
            return;
        }

        try {
            WorkflowEntity workflow = WorkflowEntity.fromTriggerAndAction(
                    "Workflow_" + System.currentTimeMillis(),
                    true,
                    trigger,
                    action
            );

            repository.insert(workflow, new WorkflowRepository.InsertCallback() {
                @Override
                public void onInsertComplete(long insertedId) {
                    loadWorkflows(); // Reload workflows after adding
                    if (callback != null) {
                        callback.onSuccess("Workflow created successfully with ID: " + insertedId);
                    }
                }

                @Override
                public void onInsertError(@NonNull String error) {
                    errorMessage.postValue("Failed to create workflow: " + error);
                    if (callback != null) {
                        callback.onError("Failed to create workflow: " + error);
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

    /**
     * Remove workflow by ID with proper error handling
     */
    public void removeWorkflow(long workflowId) {
        removeWorkflow(workflowId, null);
    }

    /**
     * Remove workflow by ID with callback
     */
    public void removeWorkflow(long workflowId, @Nullable WorkflowOperationCallback callback) {
        repository.delete(workflowId, new WorkflowRepository.DeleteCallback() {
            @Override
            public void onDeleteComplete(boolean success) {
                if (success) {
                    loadWorkflows(); // Reload workflows after deleting
                    if (callback != null) {
                        callback.onSuccess("Workflow deleted successfully");
                    }
                } else {
                    String error = "Failed to delete workflow with ID: " + workflowId;
                    errorMessage.postValue(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            }

            @Override
            public void onDeleteError(@NonNull String error) {
                errorMessage.postValue("Failed to delete workflow: " + error);
                if (callback != null) {
                    callback.onError("Failed to delete workflow: " + error);
                }
            }
        });
    }

    /**
     * Toggle workflow enabled state with error handling
     */
    public void toggleWorkflow(long workflowId, boolean enabled) {
        toggleWorkflow(workflowId, enabled, null);
    }

    /**
     * Toggle workflow enabled state with callback
     */
    public void toggleWorkflow(long workflowId, boolean enabled, @Nullable WorkflowOperationCallback callback) {
        repository.updateWorkflowEnabled(workflowId, enabled, new WorkflowRepository.UpdateCallback() {
            @Override
            public void onUpdateComplete(boolean success) {
                if (success) {
                    loadWorkflows(); // Reload workflows after updating
                    if (callback != null) {
                        callback.onSuccess("Workflow " + (enabled ? "enabled" : "disabled") + " successfully");
                    }
                } else {
                    String error = "Failed to update workflow with ID: " + workflowId;
                    errorMessage.postValue(error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            }

            @Override
            public void onUpdateError(@NonNull String error) {
                errorMessage.postValue("Failed to update workflow: " + error);
                if (callback != null) {
                    callback.onError("Failed to update workflow: " + error);
                }
            }
        });
    }

    /**
     * Get workflow by ID with callback
     */
    public void getWorkflowById(long workflowId, @NonNull WorkflowByIdCallback callback) {
        repository.getWorkflowById(workflowId, new WorkflowRepository.WorkflowByIdCallback() {
            @Override
            public void onWorkflowLoaded(@Nullable WorkflowEntity workflow) {
                callback.onWorkflowLoaded(workflow);
            }

            @Override
            public void onWorkflowError(@NonNull String error) {
                errorMessage.postValue("Failed to load workflow: " + error);
                callback.onWorkflowError(error);
            }
        });
    }

    /**
     * Callback interface for workflow operations
     */
    public interface WorkflowOperationCallback {
        void onSuccess(@NonNull String message);
        void onError(@NonNull String error);
    }

    /**
     * Callback interface for single workflow retrieval
     */
    public interface WorkflowByIdCallback {
        void onWorkflowLoaded(@Nullable WorkflowEntity workflow);
        void onWorkflowError(@NonNull String error);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources when ViewModel is cleared
        try {
            if (bleManager != null) {
                bleManager.cleanup();
            }
            if (wifiManager != null) {
                wifiManager.cleanup();
            }
            if (locationManager != null) {
                locationManager.cleanup();
            }
            if (repository != null) {
                repository.cleanup();
            }
        } catch (Exception e) {
            // Log cleanup errors but don't crash the app
            // Note: Cannot post to errorMessage here as ViewModel is being cleared
        }
    }
}
