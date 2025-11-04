package com.example.autoflow.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.geofence.GeofenceManager
import com.example.autoflow.integrations.BLEManager
import com.example.autoflow.integrations.LocationManager
import com.example.autoflow.integrations.WiFiManager
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.util.AlarmScheduler
import com.example.autoflow.integrations.PhoneStateManager
import com.example.autoflow.model.ActionTemplate
import com.example.autoflow.model.ModeTemplate
import com.example.autoflow.model.TriggerTemplate
import com.example.autoflow.util.AutoReplyManager
import com.example.autoflow.util.TriggerParser
import com.example.autoflow.model.TriggerHelpers
import com.example.autoflow.policy.BlockPolicy
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ✅ Fixed WorkflowViewModel using TriggerParser approach
 * Manages workflow CRUD operations and trigger monitoring
 */
class WorkflowViewModel(application: Application) : AndroidViewModel(application) {

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

    private val autoReplyManager = AutoReplyManager.getInstance(application)

    companion object {
        private const val TAG = "WorkflowViewModel"
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WorkflowRepository(database.workflowDao())
        loadWorkflows()
        Log.d(TAG, "ViewModel initialized")
    }

    // ✅ WORKFLOW CRUD OPERATIONS

    fun loadWorkflows() {
        repository.getAllWorkflows(object : WorkflowRepository.WorkflowCallback {
            override fun onWorkflowsLoaded(workflows: MutableList<WorkflowEntity>) {
                Log.d(TAG, "✅ Loaded ${workflows.size} workflows from database")
                workflows.forEach {
                    Log.d(TAG, " - ${it.workflowName} (ID: ${it.id})")
                }  // ← ADD THIS CLOSING BRACE
                _workflows.postValue(workflows)
            }

            override fun onWorkflowsError(error: String) {
                Log.e(TAG, "Failed to load workflows: $error")
                _errorMessage.postValue(error)
            }
        })
    }
    /**
     * ✅ FIXED: Add workflow using TriggerHelpers
     */
    fun addWorkflow(
        workflowName: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        triggerLogic: String = "AND"
    ) {
        // ✅ Validate inputs FIRST
        if (workflowName.isBlank()) {
            _errorMessage.postValue("Workflow name cannot be empty")
            return
        }

        if (triggers.isEmpty()) {
            _errorMessage.postValue("At least one trigger is required")
            return
        }

        if (actions.isEmpty()) {
            _errorMessage.postValue("At least one action is required")
            return
        }

        Log.d(TAG, "🔵 addWorkflow - Name: $workflowName")
        Log.d(TAG, "  Triggers: ${triggers.size}")
        Log.d(TAG, "  Actions: ${actions.size}")

        try {
            // Create workflow entity (ID will be 0 initially)
            val workflowEntity = WorkflowEntity.fromTriggersAndActions(
                workflowName = workflowName.trim(),
                isEnabled = true,
                triggers = triggers,
                actions = actions,
                triggerLogic = triggerLogic
            )

            if (workflowEntity == null) {
                _errorMessage.postValue("Failed to create workflow entity")
                return
            }

            // Insert workflow first
            repository.insert(workflowEntity, object : WorkflowRepository.InsertCallback {
                override fun onInsertComplete(insertedId: Long) {
                    Log.d(TAG, "🎉 Workflow inserted - ID: $insertedId")

                    // ✅ Create workflow with REAL ID
                    val savedWorkflow = workflowEntity.copy(id = insertedId)

                    // ✅ FIXED: Register triggers using TriggerParser
                    triggers.forEach { trigger ->
                        when (trigger.type) {
                            "LOCATION" -> {
                                val locationData = TriggerParser.parseLocationData(trigger)
                                locationData?.let { data ->
                                    val success = GeofenceManager.addGeofence(
                                        context = getApplication<Application>().applicationContext,
                                        workflowId = insertedId,
                                        latitude = data.latitude,
                                        longitude = data.longitude,
                                        radius = data.radius.toFloat(),
                                        triggerOnEntry = data.triggerOnEntry,
                                        triggerOnExit = data.triggerOnExit
                                    )
                                    if (success) {
                                        Log.d(TAG, "✅ Geofence registered for workflow $insertedId")
                                    } else {
                                        Log.e(TAG, "❌ Failed to register geofence for workflow $insertedId")
                                    }
                                }
                            }
                            "TIME" -> {
                                // Time triggers handled by AlarmScheduler
                                Log.d(TAG, "⏰ Time trigger will be scheduled by AlarmScheduler")
                            }
                            "WIFI" -> {
                                // WiFi triggers handled by TriggerMonitoringService
                                Log.d(TAG, "📶 WiFi trigger registered for workflow $insertedId")
                            }
                            "BLUETOOTH" -> {
                                // Bluetooth triggers handled by BLETriggerWorker
                                Log.d(TAG, "📡 Bluetooth trigger registered for workflow $insertedId")
                            }
                            "BATTERY" -> {
                                // Battery triggers handled by TriggerMonitoringService
                                Log.d(TAG, "🔋 Battery trigger registered for workflow $insertedId")
                            }
                        }
                    }

                    // ✅ Schedule alarms for time-based triggers (with correct ID)
                    AlarmScheduler.scheduleWorkflow(
                        getApplication<Application>().applicationContext,
                        savedWorkflow
                    )

                    // Reload workflows and show success
                    loadWorkflows()
                    _successMessage.postValue(
                        "Workflow '$workflowName' created with ${triggers.size} trigger(s) and ${actions.size} action(s)"
                    )
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

    /**
     * ✅ FIXED: Update workflow using TriggerParser approach
     */
    // ✅ FIXED: Update workflow using proper callbacks
    fun updateWorkflow(workflowId: Long, isEnabled: Boolean) {
        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                workflow?.let { wf ->
                    val wasEnabled = wf.isEnabled
                    val updatedWorkflow = wf.copy(isEnabled = isEnabled)

                    repository.updateWorkflow(updatedWorkflow, object : WorkflowRepository.UpdateCallback {
                        override fun onUpdateComplete(success: Boolean) {
                            if (success) {
                                Log.d(TAG, "✅ Workflow updated: ${wf.workflowName} -> enabled: $isEnabled")

                                // ✅ NEW: Auto-unblock when workflow is disabled
                                if (wasEnabled && !isEnabled) {
                                    viewModelScope.launch {
                                        try {
                                            val unblockedApps = BlockPolicy.unblockAppsForWorkflow(
                                                getApplication(),
                                                workflowId
                                            )

                                            if (unblockedApps.isNotEmpty()) {
                                                Log.d(TAG, "🔓 Auto-unblocked ${unblockedApps.size} apps due to workflow disable")
                                                _successMessage.postValue(
                                                    "Workflow disabled and ${unblockedApps.size} apps unblocked"
                                                )
                                            } else {
                                                _successMessage.postValue("Workflow disabled")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error auto-unblocking apps", e)
                                            _successMessage.postValue("Workflow disabled")
                                        }
                                    }
                                } else {
                                    _successMessage.postValue(
                                        "Workflow ${if (isEnabled) "enabled" else "disabled"}"
                                    )
                                }

                                loadWorkflows()
                            } else {
                                _errorMessage.postValue("Failed to update workflow")
                            }
                        }

                        override fun onUpdateError(error: String) {
                            Log.e(TAG, "Error updating workflow: $error")
                            _errorMessage.postValue("Failed to update workflow: $error")
                        }
                    })
                }
            }

            override fun onWorkflowError(error: String) {
                Log.e(TAG, "Error loading workflow for update: $error")
                _errorMessage.postValue("Failed to load workflow: $error")
            }
        })
    }



    // ✅ FIXED: Update workflow enabled state with proper callbacks
    fun updateWorkflowEnabled(workflowId: Long, enabled: Boolean, callback: WorkflowOperationCallback? = null) {
        // Validate workflow ID
        if (workflowId <= 0) {
            val error = "Invalid workflow ID: $workflowId"
            _errorMessage.postValue(error)
            callback?.onError(error)
            Log.w(TAG, error)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (enabled) {
                // ENABLING WORKFLOW
                repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
                    override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                        workflow?.let { wf ->
                            Log.d(TAG, "Enabling workflow: ${wf.workflowName}")
                            // ... rest of your enabling logic ...
                        }
                    }

                    override fun onWorkflowError(error: String) {
                        Log.e(TAG, "Error loading workflow: $error")
                        callback?.onError(error)
                    }
                })
            } else {
                // DISABLING WORKFLOW
                repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
                    override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                        workflow?.let { wf ->
                            Log.d(TAG, "Disabling workflow: ${wf.workflowName}")
                            // ... rest of your disabling logic ...
                        }
                    }

                    override fun onWorkflowError(error: String) {
                        Log.e(TAG, "Error loading workflow for cleanup: $error")
                    }
                })
            }

            // Update database
            repository.updateWorkflowEnabled(workflowId, enabled, object : WorkflowRepository.UpdateCallback {
                override fun onUpdateComplete(success: Boolean) {
                    if (success) {
                        loadWorkflows()
                        val msg = "Workflow ${if (enabled) "enabled" else "disabled"} successfully"
                        _successMessage.postValue(msg)
                        callback?.onSuccess(msg)
                        Log.d(TAG, msg)
                    } else {
                        val error = "Toggle failed"
                        _errorMessage.postValue(error)
                        callback?.onError(error)
                        Log.e(TAG, error)
                    }
                }

                override fun onUpdateError(error: String) {
                    _errorMessage.postValue(error)
                    callback?.onError(error)
                    Log.e(TAG, "Update error: $error")
                }
            })
        }
    }
    fun deleteWorkflow(workflowId: Long) {
        viewModelScope.launch {
            try {
                // First, unblock any apps blocked by this workflow
                val unblockedApps = BlockPolicy.unblockAppsForWorkflow(getApplication(), workflowId)

                // Then delete the workflow
                repository.deleteWorkflow(workflowId, object : WorkflowRepository.DeleteCallback {
                    override fun onDeleteComplete() { // ✅ FIXED: Correct method name
                        Log.d(TAG, "✅ Workflow deleted")

                        if (unblockedApps.isNotEmpty()) {
                            _successMessage.postValue(
                                "Workflow deleted and ${unblockedApps.size} apps unblocked"
                            )
                        } else {
                            _successMessage.postValue("Workflow deleted")
                        }

                        loadWorkflows()
                    }

                    override fun onDeleteError(error: String) { // ✅ FIXED: Correct method name
                        Log.e(TAG, "Error deleting workflow: $error")
                        _errorMessage.postValue("Failed to delete workflow: $error")
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error in workflow deletion process", e)
                _errorMessage.postValue("Failed to delete workflow: ${e.message}")
            }
        }
    }





    private fun hasWiFiPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    //  Mode creation using TriggerHelpers
    fun createWorkflowFromMode(mode: ModeTemplate, callback: WorkflowOperationCallback? = null) {
        try {
            // Convert templates to actual triggers and actions
            val triggers = mode.defaultTriggers.map { convertTemplateToTrigger(it) }
            val actions = mode.defaultActions.map { convertTemplateToAction(it) }

            // Validate
            if (triggers.isEmpty() || actions.isEmpty()) {
                val error = "Mode must have at least one trigger and one action"
                _errorMessage.postValue(error)
                callback?.onError(error)
                return
            }

            // Create workflow entity
            val workflowEntity = WorkflowEntity.fromTriggersAndActions(
                workflowName = mode.name,
                isEnabled = true,
                triggers = triggers,
                actions = actions,
                triggerLogic = "AND"
            )

            if (workflowEntity == null) {
                val error = "Failed to create workflow from mode"
                _errorMessage.postValue(error)
                callback?.onError(error)
                return
            }

            // Mark as mode workflow
            workflowEntity.isModeWorkflow = true

            // Insert workflow
            repository.insert(workflowEntity, object : WorkflowRepository.InsertCallback {
                override fun onInsertComplete(insertedId: Long) {
                    Log.d(TAG, "🎉 Mode workflow created - ID: $insertedId")

                    // Create workflow with real ID
                    val savedWorkflow = workflowEntity.copy(id = insertedId)

                    // ✅ CRITICAL FIX: Check for MANUAL triggers and execute immediately
                    val hasManualTrigger = triggers.any { it.type == "MANUAL" }
                    val hasTimeTrigger = triggers.any { it.type == "TIME" }

                    when {
                        hasManualTrigger -> {
                            Log.d(TAG, "🎯 MANUAL trigger detected - executing immediately")

                            // ✅ Execute the workflow actions RIGHT NOW
                            val context = getApplication<Application>().applicationContext
                            val executionSuccess = try {
                                ActionExecutor.getInstance().executeWorkflow(context, savedWorkflow)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error executing workflow", e)
                                false
                            }

                            if (executionSuccess) {
                                Log.d(TAG, "✅ Manual workflow executed successfully")
                            } else {
                                Log.e(TAG, "❌ Manual workflow execution failed")
                            }
                        }

                        hasTimeTrigger -> {
                            Log.d(TAG, "⏰ Time trigger for mode workflow")

                            // Schedule time-based triggers
                            AlarmScheduler.scheduleWorkflow(
                                getApplication<Application>().applicationContext,
                                savedWorkflow
                            )
                        }

                        else -> {
                            Log.d(TAG, "🔧 Other trigger types detected")
                        }
                    }
                    // ✅ Register triggers for location/other types
                    triggers.forEach { trigger ->
                        when (trigger.type) {
                            "LOCATION" -> {
                                val locationData = TriggerParser.parseLocationData(trigger)
                                locationData?.let { data ->
                                    GeofenceManager.addGeofence(
                                        context = getApplication<Application>().applicationContext,
                                        workflowId = insertedId,
                                        latitude = data.latitude,
                                        longitude = data.longitude,
                                        radius = data.radius.toFloat(),
                                        triggerOnEntry = data.triggerOnEntry,
                                        triggerOnExit = data.triggerOnExit
                                    )
                                }
                            }
                            "MANUAL" -> {
                                Log.d(TAG, "📝 Manual trigger registered for future toggles")
                            }
                            else -> {
                                Log.d(TAG, "🔧 Registering trigger: ${trigger.type}")
                            }
                        }
                    }

                    // Reload workflows and notify success
                    loadWorkflows()
                    val msg = "Mode '${mode.name}' created and ${if (hasManualTrigger) "activated" else "scheduled"}!"
                    _successMessage.postValue(msg)
                    callback?.onSuccess(msg)
                }

                override fun onInsertError(error: String) {
                    _errorMessage.postValue("Failed to create mode: $error")
                    callback?.onError(error)
                }
            })
        } catch (e: Exception) {
            val error = "Error creating mode: ${e.message}"
            _errorMessage.postValue(error)
            callback?.onError(error)
            Log.e(TAG, "❌ Error creating mode", e)
        }
    }

    // ✅ FIXED: Convert template to trigger using TriggerHelpers
    private fun convertTemplateToTrigger(template: TriggerTemplate): Trigger {
        return when (template.type) {
            "TIME" -> {
                val time = template.config["time"] as? String ?: "00:00"
                val daysString = template.config["days"] as? String ?: ""
                val days = if (daysString.isBlank()) emptyList() else daysString.split(",").map { it.trim() }
                TriggerHelpers.createTimeTrigger(time, days)
            }
            "LOCATION" -> {
                val locationName = template.config["locationName"] as? String ?: "Set Location"
                val latitude = when (val lat = template.config["latitude"]) {
                    is Number -> lat.toDouble()
                    is String -> lat.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                val longitude = when (val lng = template.config["longitude"]) {
                    is Number -> lng.toDouble()
                    is String -> lng.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                val radius = when (val rad = template.config["radius"]) {
                    is Number -> rad.toDouble()
                    is String -> rad.toDoubleOrNull() ?: 100.0
                    else -> 100.0
                }
                val triggerOnEntry = template.config["triggerOnEntry"] as? Boolean ?: true
                val triggerOnExit = template.config["triggerOnExit"] as? Boolean ?: false
                TriggerHelpers.createLocationTrigger(
                    locationName, latitude, longitude, radius, triggerOnEntry, triggerOnExit
                )
            }
            "WIFI" -> {
                val ssid = template.config["ssid"] as? String
                val state = template.config["state"] as? String ?: "connected"
                TriggerHelpers.createWifiTrigger(ssid, state)
            }
            "BLUETOOTH" -> {
                val deviceAddress = template.config["deviceAddress"] as? String ?: ""
                val deviceName = template.config["deviceName"] as? String
                TriggerHelpers.createBluetoothTrigger(deviceAddress, deviceName)
            }
            "MANUAL" -> {
                // ✅ FIXED: Added support for MANUAL trigger type
                // Manual triggers are user-activated - create a simple trigger with MANUAL type
                Trigger.ManualTrigger(  // ✅ ADD THIS!
                    actionType = template.config["type"] as? String ?: "quick_action"
                )
            }
            else -> {
                throw IllegalArgumentException("Unknown trigger type: ${template.type}")
            }
        }
    }

    private fun convertTemplateToAction(template: ActionTemplate): Action {
        return Action(
            type = template.type,
            value = template.config["value"] as? String
        ).apply {
            // ✅ Extract all properties from template config
            this.title = template.config["title"] as? String
            this.message = template.config["message"] as? String
            this.priority = template.config["priority"] as? String ?: "Normal"

            // ✅ SPECIAL CASE: For AUTO_REPLY, ensure we have message
            if (template.type == "AUTO_REPLY" && this.message == null) {
                this.message = "I'm currently in a meeting and will get back to you soon."
            }

            // ✅ SPECIAL CASE: For SHOW_NOTIFICATION, ensure we have defaults
            if (template.type == "SHOW_NOTIFICATION") {
                if (this.title == null) this.title = "Meeting Mode Active"
                if (this.message == null) this.message = "Your phone is silenced and auto-reply is on."
            }

            Log.d(TAG, "🔧 Converted ActionTemplate:")
            Log.d(TAG, "   Type: ${template.type}")
            Log.d(TAG, "   Value: ${template.config["value"]}")
            Log.d(TAG, "   Title: ${this.title}")
            Log.d(TAG, "   Message: ${this.message}")
            Log.d(TAG, "   Priority: ${this.priority}")
        }
    }



    // ✅ CALLBACK INTERFACES

    interface WorkflowOperationCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    // ✅ SINGLE onCleared() method combining both implementations
    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Cleaning up ViewModel")

        try {
            // Clean up integration managers
            bleManager.cleanup()
            wifiManager.cleanup()
            locationManager.cleanup()
            repository.cleanup()

            // Clean up phone state and auto-reply managers
            PhoneStateManager.getInstance(getApplication()).cleanup()
            AutoReplyManager.getInstance(getApplication()).cleanup()

            Log.d(TAG, "✅ Cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cleanup error", e)
        }
    }
}
