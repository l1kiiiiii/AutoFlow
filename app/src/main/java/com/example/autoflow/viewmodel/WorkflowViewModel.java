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

import org.json.JSONObject;

import java.util.List;

/**
 * Merged WorkflowViewModel - combines workflow management with trigger monitoring
 * Supports CRUD operations on workflows and real-time trigger checking
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
     * Constructor - initializes repository, LiveData, and integration managers
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

        // Initialize integration managers for trigger monitoring
        bleManager = new BLEManager(application);
        locationManager = new LocationManager(application);
        wifiManager = new WiFiManager(application);

        // Load initial workflows
        loadWorkflows();
    }

    // ==================== LiveData Getters ====================

    /**
     * Get LiveData of all workflows
     */
    public LiveData<List<WorkflowEntity>> getWorkflows() {
        return workflows;
    }

    /**
     * Get LiveData for success messages
     */
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    /**
     * Get LiveData for error messages
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // ==================== Workflow Loading ====================

    /**
     * Load all workflows from repository
     * Includes error handling for load failures
     */
    private void loadWorkflows() {
        repository.getAllWorkflows(new WorkflowRepository.WorkflowCallback() {
            @Override
            public void onWorkflowsLoaded(@NonNull List<WorkflowEntity> loadedWorkflows) {
                workflows.postValue(loadedWorkflows);
                Log.d(TAG, "Loaded " + loadedWorkflows.size() + " workflows");
            }

            @Override
            public void onWorkflowsError(@NonNull String error) {
                errorMessage.postValue("Failed to load workflows: " + error);
                Log.e(TAG, "Failed to load workflows: " + error);
            }
        });
    }

    // ==================== Add Workflow with Custom Name ====================

    /**
     * Add a new workflow with custom name
     * @param workflowName Custom name for the workflow
     * @param trigger The trigger condition
     * @param action The action to execute
     * @param callback Optional callback for operation result
     */
    public void addWorkflow(@NonNull String workflowName,
                            @NonNull Trigger trigger,
                            @NonNull Action action,
                            @Nullable WorkflowOperationCallback callback) {
        // Validate inputs
        if (trigger == null || action == null) {
            String error = "Trigger and Action cannot be null";
            errorMessage.postValue(error);
            if (callback != null) {
                callback.onError(error);
            }
            return;
        }

        // Validate trigger configuration
        if (!trigger.isValid()) {
            String error = "Invalid trigger configuration";
            errorMessage.postValue(error);
            if (callback != null) {
                callback.onError(error);
            }
            return;
        }

        try {
            // Use provided name or generate default if empty
            String finalWorkflowName = (workflowName != null && !workflowName.trim().isEmpty())
                    ? workflowName.trim()
                    : "Workflow_" + System.currentTimeMillis();

            WorkflowEntity workflow = WorkflowEntity.fromTriggerAndAction(
                    finalWorkflowName,  // Use the custom name
                    true,
                    trigger,
                    action
            );

            repository.insert(workflow, new WorkflowRepository.InsertCallback() {
                @Override
                public void onInsertComplete(long insertedId) {
                    loadWorkflows();
                    String success = "Workflow '" + finalWorkflowName + "' created with ID: " + insertedId;
                    successMessage.postValue(success);
                    Log.d(TAG, success);
                    if (callback != null) {
                        callback.onSuccess("Workflow created successfully with ID: " + insertedId);
                    }
                }
                public void onInsertError(@NonNull String error) {
                    String errorMsg = "Failed to create workflow: " + error;
                    errorMessage.postValue(errorMsg);
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            });
        } catch (Exception e) {
            String error = "Error creating workflow: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }

    // Overloaded version with custom name but no callback
    public void addWorkflow(@NonNull String workflowName, @NonNull Trigger trigger, @NonNull Action action) {
        addWorkflow(workflowName, trigger, action, null);
    }

    // Keep existing methods for backward compatibility
    public void addWorkflow(@NonNull Trigger trigger, @NonNull Action action, @Nullable WorkflowOperationCallback callback) {
        addWorkflow("Workflow_" + System.currentTimeMillis(), trigger, action, callback);
    }

    public void addWorkflow(@NonNull Trigger trigger, @NonNull Action action) {
        addWorkflow("Workflow_" + System.currentTimeMillis(), trigger, action, null);
    }


    // ==================== Delete/Remove Workflow ====================

    /**
     * Delete a workflow by ID with callback
     *
     * @param workflowId The workflow ID to delete
     * @param callback Optional callback for operation result
     */
    public void deleteWorkflow(long workflowId, @Nullable WorkflowOperationCallback callback) {
        repository.delete(workflowId, new WorkflowRepository.DeleteCallback() {
            @Override
            public void onDeleteComplete(boolean success) {
                if (success) {
                    loadWorkflows();
                    String successMsg = "Workflow deleted successfully";
                    successMessage.postValue(successMsg);
                    Log.d(TAG, successMsg + " - ID: " + workflowId);
                    if (callback != null) {
                        callback.onSuccess(successMsg);
                    }
                } else {
                    String error = "Failed to delete workflow with ID: " + workflowId;
                    errorMessage.postValue(error);
                    Log.e(TAG, error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            }

            @Override
            public void onDeleteError(@NonNull String error) {
                String errorMsg = "Failed to delete workflow: " + error;
                errorMessage.postValue(errorMsg);
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
            }
        });
    }

    /**
     * Delete workflow without callback (convenience method)
     */
    public void deleteWorkflow(long workflowId) {
        deleteWorkflow(workflowId, null);
    }

    /**
     * Remove workflow - alias for deleteWorkflow for compatibility
     */
    public void removeWorkflow(long workflowId, @Nullable WorkflowOperationCallback callback) {
        deleteWorkflow(workflowId, callback);
    }

    /**
     * Remove workflow without callback - alias for deleteWorkflow
     */
    public void removeWorkflow(long workflowId) {
        deleteWorkflow(workflowId, null);
    }

    // ==================== Update Workflow ====================

    /**
     * Update an existing workflow's details
     *
     * @param workflowId The ID of the workflow to update
     * @param workflowName New workflow name
     * @param trigger New trigger configuration
     * @param action New action configuration
     * @param callback Optional callback for operation result
     */
    public void updateWorkflow(long workflowId,
                               String workflowName,
                               Trigger trigger,
                               Action action,
                               @Nullable WorkflowOperationCallback callback) {
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
                public void onWorkflowLoaded(@Nullable WorkflowEntity existingWorkflow) {
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
                                    String successMsg = "Workflow updated successfully";
                                    successMessage.postValue(successMsg);
                                    Log.d(TAG, successMsg + " - ID: " + workflowId);
                                    if (callback != null) {
                                        callback.onSuccess(successMsg);
                                    }
                                }
                            }

                            @Override
                            public void onUpdateError(@NonNull String error) {
                                String errorMsg = "Failed to update workflow: " + error;
                                errorMessage.postValue(errorMsg);
                                Log.e(TAG, errorMsg);
                                if (callback != null) {
                                    callback.onError(error);
                                }
                            }
                        });
                    } else {
                        String error = "Workflow not found";
                        errorMessage.postValue(error);
                        Log.e(TAG, error + " - ID: " + workflowId);
                        if (callback != null) {
                            callback.onError(error);
                        }
                    }
                }

                @Override
                public void onWorkflowError(@NonNull String error) {
                    String errorMsg = "Error loading workflow: " + error;
                    errorMessage.postValue(errorMsg);
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            });
        } catch (Exception e) {
            String error = "Error updating workflow: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onError(error);
            }
        }
    }

    // ==================== Toggle/Update Workflow Enabled State ====================

    /**
     * Update workflow enabled state with callback
     *
     * @param workflowId The workflow ID to update
     * @param enabled Whether to enable or disable the workflow
     * @param callback Optional callback for operation result
     */
    public void updateWorkflowEnabled(long workflowId, boolean enabled, @Nullable WorkflowOperationCallback callback) {
        repository.updateWorkflowEnabled(workflowId, enabled, new WorkflowRepository.UpdateCallback() {
            @Override
            public void onUpdateComplete(boolean success) {
                if (success) {
                    loadWorkflows();
                    String successMsg = "Workflow " + (enabled ? "enabled" : "disabled") + " successfully";
                    successMessage.postValue(successMsg);
                    Log.d(TAG, successMsg + " - ID: " + workflowId);
                    if (callback != null) {
                        callback.onSuccess(successMsg);
                    }
                } else {
                    String error = "Failed to update workflow with ID: " + workflowId;
                    errorMessage.postValue(error);
                    Log.e(TAG, error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            }

            @Override
            public void onUpdateError(@NonNull String error) {
                String errorMsg = "Failed to update workflow: " + error;
                errorMessage.postValue(errorMsg);
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
            }
        });
    }

    /**
     * Update workflow enabled state without callback
     */
    public void updateWorkflowEnabled(long workflowId, boolean enabled) {
        updateWorkflowEnabled(workflowId, enabled, null);
    }

    /**
     * Toggle workflow - alias for updateWorkflowEnabled for compatibility
     */
    public void toggleWorkflow(long workflowId, boolean enabled, @Nullable WorkflowOperationCallback callback) {
        updateWorkflowEnabled(workflowId, enabled, callback);
    }

    /**
     * Toggle workflow without callback - alias for updateWorkflowEnabled
     */
    public void toggleWorkflow(long workflowId, boolean enabled) {
        updateWorkflowEnabled(workflowId, enabled, null);
    }

    // ==================== Get Workflow by ID ====================

    /**
     * Get a specific workflow by its ID
     *
     * @param workflowId The workflow ID to retrieve
     * @param callback Callback to receive the workflow or error
     */
    public void getWorkflowById(long workflowId, @NonNull WorkflowByIdCallback callback) {
        repository.getWorkflowById(workflowId, new WorkflowRepository.WorkflowByIdCallback() {
            @Override
            public void onWorkflowLoaded(@Nullable WorkflowEntity workflow) {
                callback.onWorkflowLoaded(workflow);
                if (workflow != null) {
                    Log.d(TAG, "Workflow loaded - ID: " + workflowId);
                } else {
                    Log.w(TAG, "Workflow not found - ID: " + workflowId);
                }
            }

            @Override
            public void onWorkflowError(@NonNull String error) {
                String errorMsg = "Failed to load workflow: " + error;
                errorMessage.postValue(errorMsg);
                Log.e(TAG, errorMsg);
                callback.onWorkflowError(error);
            }
        });
    }

    // ==================== Trigger Checking and Monitoring ====================

    /**
     * Check if a trigger condition is met
     * Requires appropriate permissions based on trigger type
     *
     * @param trigger The trigger to check
     * @param callback Callback to receive trigger result
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public void checkTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (trigger == null || callback == null) {
            Log.w(TAG, "checkTrigger called with null trigger or callback");
            return;
        }

        Log.d(TAG, "Checking trigger type: " + trigger.getType());

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
                Log.w(TAG, "Unknown trigger type: " + trigger.getType());
                callback.onTriggerFired(trigger, false);
                break;
        }
    }

    /**
     * Handle BLE (Bluetooth Low Energy) triggers
     * Checks for specific device by address or name
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    private void handleBleTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (!PermissionUtils.hasBluetoothPermissions(getApplication())) {
            Log.w(TAG, "BLE permissions not granted");
            errorMessage.postValue("Bluetooth permissions not granted");
            callback.onTriggerFired(trigger, false);
            return;
        }

        try {
            Log.d(TAG, "Starting BLE scan for: " + trigger.getValue());
            bleManager.startScanning(new BLEManager.BLECallback() {
                @Override
                public void onDeviceDetected(@NonNull String deviceAddress, @NonNull String deviceName) {
                    boolean matched = deviceAddress.equals(trigger.getValue()) ||
                            deviceName.equals(trigger.getValue());
                    Log.d(TAG, "BLE device detected - Address: " + deviceAddress +
                            ", Name: " + deviceName + ", Matched: " + matched);
                    callback.onTriggerFired(trigger, matched);
                }

                @Override
                public void onScanStarted() {
                    Log.d(TAG, "BLE scan started");
                }

                @Override
                public void onScanStopped() {
                    Log.d(TAG, "BLE scan stopped");
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    Log.e(TAG, "BLE scan error: " + errorMessage);
                    WorkflowViewModel.this.errorMessage.postValue("BLE scan error: " + errorMessage);
                    callback.onTriggerFired(trigger, false);
                }
            });
        } catch (SecurityException e) {
            String error = "BLE scanning permission denied: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            callback.onTriggerFired(trigger, false);
        } catch (Exception e) {
            String error = "BLE scanning error: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            callback.onTriggerFired(trigger, false);
        }
    }

    /**
     * Handle location-based triggers
     * Checks if current location is within specified radius of target location
     */
    private void handleLocationTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (!PermissionUtils.hasLocationPermissions(getApplication())) {
            Log.w(TAG, "Location permissions not granted");
            errorMessage.postValue("Location permissions not granted");
            callback.onTriggerFired(trigger, false);
            return;
        }

        try {
            Log.d(TAG, "Getting location for trigger: " + trigger.getValue());
            locationManager.getLastLocation(new LocationManager.LocationCallback() {
                @Override
                public void onLocationReceived(@NonNull Location location) {
                    boolean inRange = isInRange(location, trigger.getValue());
                    Log.d(TAG, "Location received - Lat: " + location.getLatitude() +
                            ", Lng: " + location.getLongitude() + ", In range: " + inRange);
                    callback.onTriggerFired(trigger, inRange);
                }

                @Override
                public void onLocationError(@NonNull String errorMessage) {
                    String error = "Location error: " + errorMessage;
                    WorkflowViewModel.this.errorMessage.postValue(error);
                    Log.e(TAG, error);
                    callback.onTriggerFired(trigger, false);
                }

                @Override
                public void onPermissionDenied() {
                    String error = "Location permission denied";
                    WorkflowViewModel.this.errorMessage.postValue(error);
                    Log.w(TAG, error);
                    callback.onTriggerFired(trigger, false);
                }
            });
        } catch (SecurityException e) {
            String error = "Location permission denied: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            callback.onTriggerFired(trigger, false);
        } catch (Exception e) {
            String error = "Location error: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            callback.onTriggerFired(trigger, false);
        }
    }

    /**
     * Handle time-based triggers
     * Checks if current time matches target time within specified window
     */
    private void handleTimeTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        try {
            if (trigger.getValue() == null || trigger.getValue().trim().isEmpty()) {
                Log.w(TAG, "Time trigger has no value");
                callback.onTriggerFired(trigger, false);
                return;
            }

            long targetTime = Long.parseLong(trigger.getValue().trim());
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - targetTime);
            boolean isTriggered = currentTime >= targetTime &&
                    timeDiff <= Constants.TIME_WINDOW_MS;

            Log.d(TAG, "Time trigger check - Target: " + targetTime +
                    ", Current: " + currentTime + ", Diff: " + timeDiff +
                    ", Triggered: " + isTriggered);
            callback.onTriggerFired(trigger, isTriggered);
        } catch (NumberFormatException e) {
            String error = "Invalid time format in trigger: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            callback.onTriggerFired(trigger, false);
        }
    }

    /**
     * Handle WiFi-based triggers
     * Checks WiFi state (on/off)
     */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    private void handleWiFiTrigger(@NonNull Trigger trigger, @NonNull TriggerCallback callback) {
        if (!hasWiFiPermissions()) {
            Log.w(TAG, "WiFi permissions not granted");
            errorMessage.postValue("WiFi permissions not granted");
            callback.onTriggerFired(trigger, false);
            return;
        }

        try {
            boolean wifiState = wifiManager.isWiFiEnabled();
            boolean expectedState = Constants.WIFI_STATE_ON.equalsIgnoreCase(trigger.getValue());
            boolean matched = wifiState == expectedState;

            Log.d(TAG, "WiFi trigger check - Current state: " + wifiState +
                    ", Expected: " + expectedState + ", Matched: " + matched);
            callback.onTriggerFired(trigger, matched);
        } catch (SecurityException e) {
            String error = "WiFi permission denied: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            callback.onTriggerFired(trigger, false);
        } catch (Exception e) {
            String error = "WiFi error: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
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
     * Location format: "latitude,longitude,radius" (radius optional, default used if not provided)
     *
     * @param location Current location
     * @param value Target location string
     * @return true if within range, false otherwise
     */
    private boolean isInRange(@NonNull Location location, @Nullable String value) {
        if (location == null || value == null || value.trim().isEmpty()) {
            Log.w(TAG, "isInRange called with null or empty parameters");
            return false;
        }

        try {
            // Parse location string format: "lat,lng,radius"
            String[] parts = value.split(",");
            if (parts.length < 2) {
                Log.w(TAG, "Invalid location format - requires at least lat,lng");
                return false;
            }

            double targetLat = Double.parseDouble(parts[0].trim());
            double targetLng = Double.parseDouble(parts[1].trim());
            float radius = parts.length > 2 ?
                    Float.parseFloat(parts[2].trim()) :
                    Constants.LOCATION_DEFAULT_RADIUS;

            // Calculate distance between current and target location
            float[] results = new float[1];
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    targetLat, targetLng, results
            );

            boolean inRange = results[0] <= radius;
            Log.d(TAG, "Location range check - Distance: " + results[0] +
                    "m, Radius: " + radius + "m, In range: " + inRange);
            return inRange;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            String error = "Invalid location format: " + e.getMessage();
            errorMessage.postValue(error);
            Log.e(TAG, error, e);
            return false;
        }
    }

    /**
     * Stop all active trigger monitoring
     * Should be called when monitoring is no longer needed
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public void stopAllTriggers() {
        Log.d(TAG, "Stopping all triggers");

        // Stop BLE scanning
        if (PermissionUtils.hasBluetoothPermissions(getApplication())) {
            try {
                bleManager.stopScanning();
                Log.d(TAG, "BLE scanning stopped");
            } catch (SecurityException e) {
                String error = "Failed to stop BLE scanning: " + e.getMessage();
                errorMessage.postValue(error);
                Log.e(TAG, error, e);
            } catch (Exception e) {
                String error = "Error stopping BLE scanning: " + e.getMessage();
                errorMessage.postValue(error);
                Log.e(TAG, error, e);
            }
        }

        // Stop WiFi monitoring
        if (wifiManager != null) {
            try {
                wifiManager.stopMonitoring();
                Log.d(TAG, "WiFi monitoring stopped");
            } catch (Exception e) {
                String error = "Error stopping WiFi monitoring: " + e.getMessage();
                errorMessage.postValue(error);
                Log.e(TAG, error, e);
            }
        }
    }

    // ==================== Callback Interfaces ====================

    /**
     * Callback interface for workflow CRUD operations
     */
    public interface WorkflowOperationCallback {
        /**
         * Called when operation succeeds
         * @param message Success message
         */
        void onSuccess(@NonNull String message);

        /**
         * Called when operation fails
         * @param error Error message
         */
        void onError(@NonNull String error);
    }

    /**
     * Callback interface for single workflow retrieval
     */
    public interface WorkflowByIdCallback {
        /**
         * Called when workflow is loaded
         * @param workflow The loaded workflow, or null if not found
         */
        void onWorkflowLoaded(@Nullable WorkflowEntity workflow);

        /**
         * Called when workflow loading fails
         * @param error Error message
         */
        void onWorkflowError(@NonNull String error);
    }

    /**
     * Callback interface for trigger firing events
     */
    public interface TriggerCallback {
        /**
         * Called when trigger check completes
         * @param trigger The checked trigger
         * @param isFired Whether the trigger condition was met
         */
        void onTriggerFired(@NonNull Trigger trigger, boolean isFired);
    }

    // ==================== Lifecycle Management ====================

    /**
     * Cleanup when ViewModel is destroyed
     * Stops all monitoring and releases resources
     */
    @SuppressLint("MissingPermission")
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel being cleared - cleaning up resources");

        try {
            // Cleanup BLE manager
            if (bleManager != null) {
                bleManager.cleanup();
                Log.d(TAG, "BLE manager cleaned up");
            }

            // Cleanup WiFi manager
            if (wifiManager != null) {
                wifiManager.cleanup();
                Log.d(TAG, "WiFi manager cleaned up");
            }

            // Cleanup location manager
            if (locationManager != null) {
                locationManager.cleanup();
                Log.d(TAG, "Location manager cleaned up");
            }

            // Cleanup repository
            if (repository != null) {
                repository.cleanup();
                Log.d(TAG, "Repository cleaned up");
            }
        } catch (Exception e) {
            // Log cleanup errors but don't crash the app
            // Cannot post to errorMessage here as ViewModel is being cleared
            Log.e(TAG, "Error during cleanup: " + e.getMessage(), e);
        }
    }
}
