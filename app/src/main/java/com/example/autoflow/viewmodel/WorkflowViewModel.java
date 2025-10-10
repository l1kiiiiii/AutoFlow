package com.example.autoflow.viewmodel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Production-Ready WorkflowViewModel
 * Manages workflow CRUD operations and trigger monitoring
 * Thread-safe with proper error handling and resource cleanup
 */
public class WorkflowViewModel extends AndroidViewModel {
    private static final String TAG = "WorkflowViewModel";

    // Repository and LiveData
    private final WorkflowRepository repository;
    private final MutableLiveData<List<WorkflowEntity>> workflows;
    private final MutableLiveData<String> successMessage;
    private final MutableLiveData<String> errorMessage;

    // Integration managers for trigger monitoring
    private final BLEManager bleManager;
    private final LocationManager locationManager;
    private final WiFiManager wifiManager;

    /**
     * Constructor - initializes all components
     */
    public WorkflowViewModel(@NonNull Application application) {
        super(application);

        // Initialize database and repository
        AppDatabase db = AppDatabase.getDatabase(application);
        repository = new WorkflowRepository(db);

        // Initialize LiveData
        workflows = new MutableLiveData<>();
        successMessage = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();

        // Initialize integration managers
        bleManager = new BLEManager(application);
        locationManager = new LocationManager(application);
        wifiManager = new WiFiManager(application);

        // Load initial workflows
        loadWorkflows();

        Log.d(TAG, "ViewModel initialized successfully");
    }

    // ==================== LiveData Getters ====================

    /**
     * Get LiveData of all workflows
     */
    @NonNull
    public LiveData<List<WorkflowEntity>> getWorkflows() {
        return workflows;
    }

    /**
     * Get LiveData for success messages
     */
    @NonNull
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    /**
     * Get LiveData for error messages
     */
    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // ==================== Workflow Loading ====================

    /**
     * Load all workflows from repository
     */
    public void loadWorkflows() {
        repository.getAllWorkflows(new WorkflowRepository.WorkflowCallback() {
            @Override
            public void onWorkflowsLoaded(@NonNull List<WorkflowEntity> loadedWorkflows) {
                workflows.postValue(loadedWorkflows);
                Log.d(TAG, "✅ Loaded " + loadedWorkflows.size() + " workflows");
            }

            @Override
            public void onWorkflowsError(@NonNull String error) {
                errorMessage.postValue("Failed to load workflows: " + error);
                Log.e(TAG, "❌ Load error: " + error);
            }
        });
    }

    // ==================== Add Workflow ====================

    /**
     * Add a new workflow
     * @param workflowName Name of the workflow
     * @param trigger Trigger condition
     * @param action Action to execute
     */
    public void addWorkflow(@NonNull String workflowName,
                            @NonNull Trigger trigger,
                            @NonNull Action action) {
        // Validate inputs
        if (workflowName == null || workflowName.trim().isEmpty()) {
            String error = "Workflow name cannot be empty";
            errorMessage.postValue(error);
            Log.e(TAG, "❌ " + error);
            return;
        }

        if (trigger == null) {
            String error = "Trigger cannot be null";
            errorMessage.postValue(error);
            Log.e(TAG, "❌ " + error);
            return;
        }

        if (action == null) {
            String error = "Action cannot be null";
            errorMessage.postValue(error);
            Log.e(TAG, "❌ " + error);
            return;
        }

        Log.d(TAG, "🔵 addWorkflow - Name: " + workflowName);
        Log.d(TAG, "  Trigger: " + trigger.getType() + " = " + trigger.getValue());
        Log.d(TAG, "  Action: " + action.getType());

        try {
            // Create workflow entity
            WorkflowEntity workflow = WorkflowEntity.fromTriggerAndAction(
                    workflowName.trim(),
                    true,
                    trigger,
                    action
            );

            if (workflow == null) {
                String error = "Failed to create workflow entity";
                errorMessage.postValue(error);
                Log.e(TAG, "❌ " + error);
                return;
            }

            Log.d(TAG, "✅ WorkflowEntity created");

            // Insert into database
            repository.insert(workflow, new WorkflowRepository.InsertCallback() {
                @Override
                public void onInsertComplete(long insertedId) {
                    Log.d(TAG, "🎉 Workflow inserted - ID: " + insertedId);
                    loadWorkflows();
                    successMessage.postValue("Workflow '" + workflowName + "' created");
                }

                @Override
                public void onInsertError(@NonNull String error) {
                    errorMessage.postValue("Failed to create workflow: " + error);
                    Log.e(TAG, "❌ Insert error: " + error);
                }
            });

        } catch (Exception e) {
            String error = "Error creating workflow: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, "❌ " + error, e);
        }
    }

    // ==================== Update Workflow ====================

    /**
     * Update an existing workflow
     * @param workflowId ID of workflow to update
     * @param workflowName New name
     * @param trigger New trigger
     * @param action New action
     * @param callback Optional callback
     */
    public void updateWorkflow(long workflowId,
                               @NonNull String workflowName,
                               @NonNull Trigger trigger,
                               @NonNull Action action,
                               @Nullable WorkflowOperationCallback callback) {
        if (workflowId <= 0) {
            String error = "Invalid workflow ID";
            errorMessage.postValue(error);
            if (callback != null) callback.onError(error);
            return;
        }

        if (workflowName == null || workflowName.trim().isEmpty()) {
            String error = "Workflow name cannot be empty";
            errorMessage.postValue(error);
            if (callback != null) callback.onError(error);
            return;
        }

        try {
            // Build JSON for trigger
            JSONObject triggerJson = new JSONObject();
            triggerJson.put("type", trigger.type);
            triggerJson.put("value", trigger.value);
            String triggerDetails = triggerJson.toString();

            // Build JSON for action
            JSONObject actionJson = new JSONObject();
            actionJson.put("type", action.type);
            if (action.title != null) actionJson.put("title", action.title);
            if (action.message != null) actionJson.put("message", action.message);
            if (action.priority != null) actionJson.put("priority", action.priority);
            String actionDetails = actionJson.toString();

            // Get and update workflow
            repository.getWorkflowById(workflowId, new WorkflowRepository.WorkflowByIdCallback() {
                @Override
                public void onWorkflowLoaded(@Nullable WorkflowEntity existingWorkflow) {
                    if (existingWorkflow == null) {
                        String error = "Workflow not found";
                        errorMessage.postValue(error);
                        if (callback != null) callback.onError(error);
                        return;
                    }

                    // Update fields
                    existingWorkflow.setWorkflowName(workflowName.trim());
                    existingWorkflow.setTriggerDetails(triggerDetails);
                    existingWorkflow.setActionDetails(actionDetails);
                    existingWorkflow.setEnabled(true);

                    // Save
                    repository.update(existingWorkflow, new WorkflowRepository.UpdateCallback() {
                        @Override
                        public void onUpdateComplete(boolean success) {
                            if (success) {
                                loadWorkflows();
                                String successMsg = "Workflow updated";
                                successMessage.postValue(successMsg);
                                Log.d(TAG, "✅ " + successMsg);
                                if (callback != null) callback.onSuccess(successMsg);
                            } else {
                                String error = "Update failed";
                                errorMessage.postValue(error);
                                if (callback != null) callback.onError(error);
                            }
                        }

                        @Override
                        public void onUpdateError(@NonNull String error) {
                            errorMessage.postValue(error);
                            Log.e(TAG, "❌ Update error: " + error);
                            if (callback != null) callback.onError(error);
                        }
                    });
                }

                @Override
                public void onWorkflowError(@NonNull String error) {
                    errorMessage.postValue(error);
                    Log.e(TAG, "❌ Load error: " + error);
                    if (callback != null) callback.onError(error);
                }
            });
        } catch (JSONException e) {
            String error = "JSON error: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, "❌ " + error, e);
            if (callback != null) callback.onError(error);
        } catch (Exception e) {
            String error = "Error: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, "❌ " + error, e);
            if (callback != null) callback.onError(error);
        }
    }

    // ==================== Delete Workflow ====================

    /**
     * Delete a workflow by ID
     * @param workflowId ID to delete
     * @param callback Optional callback
     */
    public void deleteWorkflow(long workflowId, @Nullable WorkflowOperationCallback callback) {
        if (workflowId <= 0) {
            String error = "Invalid workflow ID";
            errorMessage.postValue(error);
            if (callback != null) callback.onError(error);
            return;
        }

        repository.delete(workflowId, new WorkflowRepository.DeleteCallback() {
            @Override
            public void onDeleteComplete(boolean success) {
                if (success) {
                    loadWorkflows();
                    String successMsg = "Workflow deleted";
                    successMessage.postValue(successMsg);
                    Log.d(TAG, "✅ " + successMsg);
                    if (callback != null) callback.onSuccess(successMsg);
                } else {
                    String error = "Delete failed";
                    errorMessage.postValue(error);
                    if (callback != null) callback.onError(error);
                }
            }

            @Override
            public void onDeleteError(@NonNull String error) {
                errorMessage.postValue(error);
                Log.e(TAG, "❌ Delete error: " + error);
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void deleteWorkflow(long workflowId) {
        deleteWorkflow(workflowId, null);
    }

    // ==================== Toggle Workflow Enabled State ====================

    /**
     * Enable or disable a workflow
     * @param workflowId ID to toggle
     * @param enabled New enabled state
     * @param callback Optional callback
     */
    public void updateWorkflowEnabled(long workflowId, boolean enabled, @Nullable WorkflowOperationCallback callback) {
        if (workflowId <= 0) {
            String error = "Invalid workflow ID";
            errorMessage.postValue(error);
            if (callback != null) callback.onError(error);
            return;
        }

        repository.updateWorkflowEnabled(workflowId, enabled, new WorkflowRepository.UpdateCallback() {
            @Override
            public void onUpdateComplete(boolean success) {
                if (success) {
                    loadWorkflows();
                    String successMsg = "Workflow " + (enabled ? "enabled" : "disabled");
                    successMessage.postValue(successMsg);
                    Log.d(TAG, "✅ " + successMsg);
                    if (callback != null) callback.onSuccess(successMsg);
                } else {
                    String error = "Toggle failed";
                    errorMessage.postValue(error);
                    if (callback != null) callback.onError(error);
                }
            }

            @Override
            public void onUpdateError(@NonNull String error) {
                errorMessage.postValue(error);
                Log.e(TAG, "❌ Toggle error: " + error);
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void updateWorkflowEnabled(long workflowId, boolean enabled) {
        updateWorkflowEnabled(workflowId, enabled, null);
    }

    // ==================== Get Workflow by ID ====================

    /**
     * Get a specific workflow
     * @param workflowId ID to retrieve
     * @param callback Callback with result
     */
    public void getWorkflowById(long workflowId, @NonNull WorkflowByIdCallback callback) {
        if (callback == null) {
            Log.e(TAG, "❌ Callback cannot be null");
            return;
        }

        if (workflowId <= 0) {
            callback.onWorkflowError("Invalid workflow ID");
            return;
        }

        repository.getWorkflowById(workflowId, new WorkflowRepository.WorkflowByIdCallback() {
            @Override
            public void onWorkflowLoaded(@Nullable WorkflowEntity workflow) {
                callback.onWorkflowLoaded(workflow);
                Log.d(TAG, workflow != null ? "✅ Workflow loaded" : "⚠️ Workflow not found");
            }

            @Override
            public void onWorkflowError(@NonNull String error) {
                errorMessage.postValue(error);
                Log.e(TAG, "❌ Load error: " + error);
                callback.onWorkflowError(error);
            }
        });
    }

    // ==================== Trigger Monitoring ====================

    /**
     * Check if a trigger condition is met
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public void checkTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (trigger == null || callback == null) {
            Log.w(TAG, "⚠️ Null trigger or callback");
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
                Log.w(TAG, "⚠️ Unknown trigger type: " + trigger.getType());
                callback.onTriggerFired(trigger, false);
        }
    }

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
                }

                @Override
                public void onScanStopped() {
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    callback.onTriggerFired(trigger, false);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "BLE error", e);
            callback.onTriggerFired(trigger, false);
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void handleLocationTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (!PermissionUtils.hasLocationPermissions(getApplication())) {
            callback.onTriggerFired(trigger, false);
            return;
        }

        try {
            locationManager.getLastLocation(new LocationManager.LocationCallback() {
                @Override
                public void onLocationReceived(@NonNull Location location) {
                    boolean inRange = isInRange(location, trigger.getValue());
                    callback.onTriggerFired(trigger, inRange);
                }

                @Override
                public void onLocationError(@NonNull String errorMessage) {
                    callback.onTriggerFired(trigger, false);
                }

                @Override
                public void onPermissionDenied() {
                    callback.onTriggerFired(trigger, false);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Location error", e);
            callback.onTriggerFired(trigger, false);
        }
    }

    private void handleTimeTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        try {
            if (trigger.getValue() == null || trigger.getValue().trim().isEmpty()) {
                callback.onTriggerFired(trigger, false);
                return;
            }

            long targetTime = Long.parseLong(trigger.getValue().trim());
            long currentTime = System.currentTimeMillis();
            boolean isTriggered = currentTime >= targetTime &&
                    (currentTime - targetTime) <= Constants.TIME_WINDOW_MS;

            callback.onTriggerFired(trigger, isTriggered);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Time format error", e);
            callback.onTriggerFired(trigger, false);
        }
    }

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
        } catch (Exception e) {
            Log.e(TAG, "WiFi error", e);
            callback.onTriggerFired(trigger, false);
        }
    }

    private boolean hasWiFiPermissions() {
        return ActivityCompat.checkSelfPermission(getApplication(),
                Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isInRange(@NonNull Location location, @Nullable String value) {
        if (location == null || value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            String[] parts = value.split(",");
            if (parts.length < 2) return false;

            double targetLat = Double.parseDouble(parts[0].trim());
            double targetLng = Double.parseDouble(parts[1].trim());
            float radius = parts.length > 2 ?
                    Float.parseFloat(parts[2].trim()) :
                    Constants.LOCATION_DEFAULT_RADIUS;

            float[] results = new float[1];
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    targetLat, targetLng, results
            );

            return results[0] <= radius;
        } catch (Exception e) {
            Log.e(TAG, "Location parse error", e);
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public void stopAllTriggers() {
        try {
            if (bleManager != null) bleManager.stopScanning();
            if (wifiManager != null) wifiManager.stopMonitoring();
            Log.d(TAG, "✅ All triggers stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping triggers", e);
        }
    }

    // ==================== Callback Interfaces ====================

    public interface WorkflowOperationCallback {
        void onSuccess(@NonNull String message);
        void onError(@NonNull String error);
    }

    public interface WorkflowByIdCallback {
        void onWorkflowLoaded(@Nullable WorkflowEntity workflow);
        void onWorkflowError(@NonNull String error);
    }

    public interface TriggerCallback {
        void onTriggerFired(@NonNull Trigger trigger, boolean isFired);
    }

    // ==================== Lifecycle Management ====================

    @SuppressLint("MissingPermission")
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "🧹 Cleaning up ViewModel");

        try {
            if (bleManager != null) bleManager.cleanup();
            if (wifiManager != null) wifiManager.cleanup();
            if (locationManager != null) locationManager.cleanup();
            if (repository != null) repository.cleanup();
            Log.d(TAG, "✅ Cleanup complete");
        } catch (Exception e) {
            Log.e(TAG, "Cleanup error", e);
        }
    }
}
