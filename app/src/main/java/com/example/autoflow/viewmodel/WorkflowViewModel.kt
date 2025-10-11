package com.example.autoflow.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.geofence.GeofenceManager  // ✅ ADD THIS IMPORT
import com.example.autoflow.integrations.BLEManager
import com.example.autoflow.integrations.LocationManager
import com.example.autoflow.integrations.WiFiManager
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.util.Constants
import com.example.autoflow.util.PermissionUtils
import org.json.JSONObject

/**
 * Modern WorkflowViewModel using Coroutines
 * Manages workflow CRUD operations and trigger monitoring
 */
class WorkflowViewModel(application: Application) : AndroidViewModel(application) {

    // Pass AppDatabase to repository
    private val repository: WorkflowRepository

    // LiveData
    private val _workflows = MutableLiveData<List<WorkflowEntity>>()
    val workflows: LiveData<List<WorkflowEntity>> = _workflows

    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Integration managers
    private val bleManager = BLEManager(application)
    private val locationManager = LocationManager(application)
    private val wifiManager = WiFiManager(application)

    companion object {
        private const val TAG = "WorkflowViewModel"
    }

    init {
        //  Get database instance first
        val database = AppDatabase.getDatabase(application)
        repository = WorkflowRepository(database)

        loadWorkflows()
        Log.d(TAG, "ViewModel initialized")
    }

    // ========== WORKFLOW CRUD OPERATIONS ==========

    fun loadWorkflows() {
        // : Use correct method name
        repository.getAllWorkflows(object : WorkflowRepository.WorkflowCallback {
            override fun onWorkflowsLoaded(workflows: MutableList<WorkflowEntity>) {
                _workflows.postValue(workflows)
                Log.d(TAG, "✅ Loaded ${workflows.size} workflows")
            }

            override fun onWorkflowsError(error: String) {
                _errorMessage.postValue("Failed to load workflows: $error")
                Log.e(TAG, "❌ Load error: $error")
            }
        })
    }

    fun addWorkflow(workflowName: String, trigger: Trigger, action: Action) {
        // Validate inputs FIRST
        if (workflowName.isBlank()) {
            _errorMessage.postValue("Workflow name cannot be empty")
            return
        }

        Log.d(TAG, "🔵 addWorkflow - Name: $workflowName")
        Log.d(TAG, "  Trigger: ${trigger.type} = ${trigger.value}")
        Log.d(TAG, "  Action: ${action.type}")

        try {
            // Create workflow entity
            val workflowEntity = WorkflowEntity.fromTriggerAndAction(
                workflowName = workflowName.trim(),
                isEnabled = true,
                trigger = trigger,
                action = action
            )

            if (workflowEntity == null) {
                _errorMessage.postValue("Failed to create workflow entity")
                return
            }

            // Insert workflow and register geofence with actual database ID
            repository.insert(workflowEntity, object : WorkflowRepository.InsertCallback {
                override fun onInsertComplete(insertedId: Long) {
                    Log.d(TAG, "🎉 Workflow inserted - ID: $insertedId")

                    // Register geofence with ACTUAL database ID (not 0)
                    if (trigger is Trigger.LocationTrigger) {
                        val success = GeofenceManager.addGeofence(  // ✅ FIXED: Use GeofenceManager object
                            context = getApplication<Application>().applicationContext,
                            workflowId = insertedId,  // Use actual ID from database
                            latitude = trigger.latitude,
                            longitude = trigger.longitude,
                            radius = trigger.radius.toFloat(),
                            triggerOnEntry = trigger.triggerOnEntry,
                            triggerOnExit = trigger.triggerOnExit
                        )

                        if (success) {
                            Log.d(TAG, "✅ Geofence registered for workflow $insertedId")
                        } else {
                            Log.e(TAG, "❌ Failed to register geofence for workflow $insertedId")
                        }
                    }

                    // Reload workflows and show success message
                    loadWorkflows()
                    _successMessage.postValue("Workflow '$workflowName' created")
                }

                override fun onInsertError(error: String) {
                    _errorMessage.postValue("Failed to create workflow: $error")
                    Log.e(TAG, "❌ Insert error: $error")
                }
            })
        } catch (e: Exception) {
            _errorMessage.postValue("Error creating workflow: ${e.message}")
            Log.e(TAG, "❌ Error", e)
        }
    }


    fun updateWorkflow(
        workflowId: Long,
        workflowName: String,
        trigger: Trigger,
        action: Action,
        callback: WorkflowOperationCallback? = null
    ) {
        if (workflowId <= 0) {
            val error = "Invalid workflow ID"
            _errorMessage.postValue(error)
            callback?.onError(error)
            return
        }

        if (workflowName.isBlank()) {
            val error = "Workflow name cannot be empty"
            _errorMessage.postValue(error)
            callback?.onError(error)
            return
        }

        try {
            // Build JSON for trigger
            val triggerJson = JSONObject().apply {
                put("type", trigger.type)
                put("value", trigger.value)
            }

            // Build JSON for action
            val actionJson = JSONObject().apply {
                put("type", action.type)
                action.title?.let { put("title", it) }
                action.message?.let { put("message", it) }
                action.priority?.let { put("priority", it) }
            }

            // ✅ FIXED: Use correct method name
            repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
                override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                    if (workflow == null) {
                        val error = "Workflow not found"
                        _errorMessage.postValue(error)
                        callback?.onError(error)
                        return
                    }

                    // Update fields
                    workflow.workflowName = workflowName.trim()
                    workflow.triggerDetails = triggerJson.toString()
                    workflow.actionDetails = actionJson.toString()
                    workflow.isEnabled = true

                    // Save
                    repository.update(workflow, object : WorkflowRepository.UpdateCallback {
                        override fun onUpdateComplete(success: Boolean) {
                            if (success) {
                                loadWorkflows()
                                val msg = "Workflow updated"
                                _successMessage.postValue(msg)
                                callback?.onSuccess(msg)
                            } else {
                                val error = "Update failed"
                                _errorMessage.postValue(error)
                                callback?.onError(error)
                            }
                        }

                        override fun onUpdateError(error: String) {
                            _errorMessage.postValue(error)
                            callback?.onError(error)
                        }
                    })
                }

                override fun onWorkflowError(error: String) {
                    _errorMessage.postValue(error)
                    callback?.onError(error)
                }
            })
        } catch (e: Exception) {
            val error = "Error: ${e.message}"
            _errorMessage.postValue(error)
            callback?.onError(error)
        }
    }

    fun deleteWorkflow(workflowId: Long, callback: WorkflowOperationCallback? = null) {
        if (workflowId <= 0) {
            val error = "Invalid workflow ID"
            _errorMessage.postValue(error)
            callback?.onError(error)
            return
        }

        // ✅ Remove geofence before deleting workflow
        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                if (workflow != null) {
                    // Check if it's a location trigger and remove geofence
                    try {
                        val triggerJson = JSONObject(workflow.triggerDetails)
                        if (triggerJson.optString("type") == "LOCATION") {
                            GeofenceManager.removeGeofence(
                                getApplication<Application>().applicationContext,
                                workflow.id
                            )
                            Log.d(TAG, "🗑️ Removed geofence for workflow ${workflow.id}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing geofence: ${e.message}")
                    }
                }

                // Now delete the workflow
                repository.delete(workflowId, object : WorkflowRepository.DeleteCallback {
                    override fun onDeleteComplete(success: Boolean) {
                        if (success) {
                            loadWorkflows()
                            val msg = "Workflow deleted"
                            _successMessage.postValue(msg)
                            callback?.onSuccess(msg)
                        } else {
                            val error = "Delete failed"
                            _errorMessage.postValue(error)
                            callback?.onError(error)
                        }
                    }

                    override fun onDeleteError(error: String) {
                        _errorMessage.postValue(error)
                        callback?.onError(error)
                    }
                })
            }

            override fun onWorkflowError(error: String) {
                // Still try to delete even if we can't load the workflow
                repository.delete(workflowId, object : WorkflowRepository.DeleteCallback {
                    override fun onDeleteComplete(success: Boolean) {
                        if (success) {
                            loadWorkflows()
                            val msg = "Workflow deleted"
                            _successMessage.postValue(msg)
                            callback?.onSuccess(msg)
                        } else {
                            val error = "Delete failed"
                            _errorMessage.postValue(error)
                            callback?.onError(error)
                        }
                    }

                    override fun onDeleteError(error: String) {
                        _errorMessage.postValue(error)
                        callback?.onError(error)
                    }
                })
            }
        })
    }

    fun updateWorkflowEnabled(
        workflowId: Long,
        enabled: Boolean,
        callback: WorkflowOperationCallback? = null
    ) {
        if (workflowId <= 0) {
            val error = "Invalid workflow ID"
            _errorMessage.postValue(error)
            callback?.onError(error)
            return
        }

        repository.updateWorkflowEnabled(workflowId, enabled, object : WorkflowRepository.UpdateCallback {
            override fun onUpdateComplete(success: Boolean) {
                if (success) {
                    loadWorkflows()
                    val msg = "Workflow ${if (enabled) "enabled" else "disabled"}"
                    _successMessage.postValue(msg)
                    callback?.onSuccess(msg)
                } else {
                    val error = "Toggle failed"
                    _errorMessage.postValue(error)
                    callback?.onError(error)
                }
            }

            override fun onUpdateError(error: String) {
                _errorMessage.postValue(error)
                callback?.onError(error)
            }
        })
    }

    fun getWorkflowById(workflowId: Long, callback: WorkflowByIdCallback) {
        if (workflowId <= 0) {
            callback.onWorkflowError("Invalid workflow ID")
            return
        }

        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                callback.onWorkflowLoaded(workflow)
                Log.d(TAG, if (workflow != null) "✅ Workflow loaded" else "⚠️ Workflow not found")
            }

            override fun onWorkflowError(error: String) {
                _errorMessage.postValue(error)
                callback.onWorkflowError(error)
            }
        })
    }

    // ========== TRIGGER MONITORING ==========

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION])
    fun checkTrigger(trigger: Trigger, callback: TriggerCallback) {
        when (trigger.type) {
            Constants.TRIGGER_BLE -> handleBleTrigger(trigger, callback)
            Constants.TRIGGER_LOCATION -> handleLocationTrigger(trigger, callback)
            Constants.TRIGGER_TIME -> handleTimeTrigger(trigger, callback)
            Constants.TRIGGER_WIFI -> handleWiFiTrigger(trigger, callback)
            else -> {
                Log.w(TAG, "⚠️ Unknown trigger type: ${trigger.type}")
                callback.onTriggerFired(trigger, false)
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun handleBleTrigger(trigger: Trigger, callback: TriggerCallback) {
        if (!PermissionUtils.hasBluetoothPermissions(getApplication())) {
            callback.onTriggerFired(trigger, false)
            return
        }

        try {
            bleManager.startScanning(object : BLEManager.BLECallback {
                override fun onDeviceDetected(deviceAddress: String, deviceName: String) {
                    val matched = deviceAddress == trigger.value || deviceName == trigger.value
                    callback.onTriggerFired(trigger, matched)
                }

                override fun onScanStarted() {}
                override fun onScanStopped() {}

                override fun onError(errorMessage: String) {
                    callback.onTriggerFired(trigger, false)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "BLE error", e)
            callback.onTriggerFired(trigger, false)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun handleLocationTrigger(trigger: Trigger, callback: TriggerCallback) {
        if (!PermissionUtils.hasLocationPermissions(getApplication())) {
            callback.onTriggerFired(trigger, false)
            return
        }

        try {
            locationManager.getLastLocation(object : LocationManager.Callback {
                override fun onLocationReceived(location: Location) {
                    val inRange = isInRange(location, trigger.value)
                    callback.onTriggerFired(trigger, inRange)
                }

                override fun onLocationError(errorMessage: String) {
                    callback.onTriggerFired(trigger, false)
                }

                override fun onPermissionDenied() {
                    callback.onTriggerFired(trigger, false)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Location error", e)
            callback.onTriggerFired(trigger, false)
        }
    }

    private fun handleTimeTrigger(trigger: Trigger, callback: TriggerCallback) {
        try {
            if (trigger.value.isBlank()) {
                callback.onTriggerFired(trigger, false)
                return
            }

            val targetTime = trigger.value.trim().toLong()
            val currentTime = System.currentTimeMillis()
            val isTriggered = currentTime >= targetTime &&
                    (currentTime - targetTime) <= Constants.TIME_WINDOW_MS

            callback.onTriggerFired(trigger, isTriggered)
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Time format error", e)
            callback.onTriggerFired(trigger, false)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    private fun handleWiFiTrigger(trigger: Trigger, callback: TriggerCallback) {
        if (!hasWiFiPermissions()) {
            callback.onTriggerFired(trigger, false)
            return
        }

        try {
            val wifiState = wifiManager.isWiFiEnabled()
            val expectedState = Constants.WIFI_STATE_ON.equals(trigger.value, ignoreCase = true)
            callback.onTriggerFired(trigger, wifiState == expectedState)
        } catch (e: Exception) {
            Log.e(TAG, "WiFi error", e)
            callback.onTriggerFired(trigger, false)
        }
    }

    private fun hasWiFiPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isInRange(location: Location, value: String): Boolean {
        if (value.isBlank()) return false

        return try {
            val parts = value.split(",")
            if (parts.size < 2) return false

            val targetLat = parts[0].trim().toDouble()
            val targetLng = parts[1].trim().toDouble()
            val radius = if (parts.size > 2) parts[2].trim().toFloat()
            else Constants.LOCATION_DEFAULT_RADIUS

            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                targetLat, targetLng, results
            )

            results[0] <= radius
        } catch (e: Exception) {
            Log.e(TAG, "Location parse error", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAllTriggers() {
        try {
            bleManager.stopScanning()
            wifiManager.stopMonitoring()
            Log.d(TAG, "✅ All triggers stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping triggers", e)
        }
    }

    // ========== CALLBACK INTERFACES ==========

    interface WorkflowOperationCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    interface WorkflowByIdCallback {
        fun onWorkflowLoaded(workflow: WorkflowEntity?)
        fun onWorkflowError(error: String)
    }

    interface TriggerCallback {
        fun onTriggerFired(trigger: Trigger, isFired: Boolean)
    }

    // ========== LIFECYCLE ==========

    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Cleaning up ViewModel")

        try {
            bleManager.cleanup()
            wifiManager.cleanup()
            locationManager.cleanup()
            repository.cleanup()
            Log.d(TAG, "✅ Cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error", e)
        }
    }
}
