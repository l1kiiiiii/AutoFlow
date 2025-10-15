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
import com.example.autoflow.geofence.GeofenceManager
import com.example.autoflow.integrations.BLEManager
import com.example.autoflow.integrations.LocationManager
import com.example.autoflow.integrations.WiFiManager
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.util.Constants
import com.example.autoflow.util.PermissionUtils
import org.json.JSONObject
import com.example.autoflow.util.AlarmScheduler
import org.json.JSONArray
import com.example.autoflow.data.toTriggers
import com.example.autoflow.model.ActionTemplate
import com.example.autoflow.model.ModeTemplate
import com.example.autoflow.model.TriggerTemplate


/**
 * Modern WorkflowViewModel using Coroutines
 * Manages workflow CRUD operations and trigger monitoring
 */
@Suppress("unused", "MemberVisibilityCanBePrivate") // Public API for UI components
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

    companion object {
        private const val TAG = "WorkflowViewModel"
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WorkflowRepository(database.workflowDao())  
        loadWorkflows()
        Log.d(TAG, "ViewModel initialized")
    }

    //  WORKFLOW CRUD OPERATIONS 

    fun loadWorkflows() {
        repository.getAllWorkflows(object : WorkflowRepository.WorkflowCallback {
            override fun onWorkflowsLoaded(workflows: MutableList<WorkflowEntity>) {
                Log.d(TAG, "✅ Loaded ${workflows.size} workflows from database")
                workflows.forEach {
                    Log.d(TAG, "  - ${it.workflowName} (ID: ${it.id})")
                }
                _workflows.postValue(workflows)
            }

            override fun onWorkflowsError(error: String) {
                Log.e(TAG, "Failed to load workflows: $error")
            }
        })
    }




    /**
     *  Add workflow with proper ID handling
     * Geofences and alarms are now registered AFTER insertion
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

                    // ✅ Register triggers with correct ID
                    triggers.forEach { trigger ->
                        when (trigger) {
                            is Trigger.LocationTrigger -> {
                                val success = GeofenceManager.addGeofence(
                                    context = getApplication<Application>().applicationContext,
                                    workflowId = insertedId,  // ✅ Use real ID
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
                            is Trigger.TimeTrigger -> {
                                // Time triggers handled by AlarmScheduler
                                Log.d(TAG, "⏰ Time trigger will be scheduled by AlarmScheduler")
                            }
                            is Trigger.WiFiTrigger -> {
                                // WiFi triggers handled by TriggerMonitoringService
                                Log.d(TAG, "📶 WiFi trigger registered for workflow $insertedId")
                            }
                            is Trigger.BluetoothTrigger -> {
                                // Bluetooth triggers handled by BLETriggerWorker
                                Log.d(TAG, "📡 Bluetooth trigger registered for workflow $insertedId")
                            }
                            is Trigger.BatteryTrigger -> {
                                // Battery triggers handled by TriggerMonitoringService
                                Log.d(TAG, "🔋 Battery trigger registered for workflow $insertedId")
                            }
                        }
                    }

                    // ✅ Schedule alarms for time-based triggers (with correct ID)
                    AlarmScheduler.scheduleWorkflow(
                        getApplication<Application>().applicationContext,
                        savedWorkflow  // ✅ Has real ID, not 0
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
     * ✅ FIXED: Update workflow with proper validation
     */
    fun updateWorkflow(
        workflowId: Long,
        workflowName: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        callback: WorkflowOperationCallback? = null,
        triggerLogic: String = "AND"
    ) {
        // ✅ Validate workflow ID
        if (workflowId <= 0) {
            val error = "Invalid workflow ID: $workflowId"
            _errorMessage.postValue(error)
            callback?.onError(error)
            Log.w(TAG, "⚠️ $error")
            return
        }

        if (workflowName.isBlank()) {
            val error = "Workflow name cannot be empty"
            _errorMessage.postValue(error)
            callback?.onError(error)
            return
        }

        if (triggers.isEmpty()) {
            val error = "At least one trigger is required"
            _errorMessage.postValue(error)
            callback?.onError(error)
            return
        }

        if (actions.isEmpty()) {
            val error = "At least one action is required"
            _errorMessage.postValue(error)
            callback?.onError(error)
            return
        }

        try {
            // Build triggers JSON array
            val triggersJsonArray = JSONArray()
            triggers.forEach { trigger ->
                val triggerJson = JSONObject().apply {
                    put("type", trigger.type)
                    put("value", trigger.value)

                    when (trigger) {
                        is Trigger.LocationTrigger -> {
                            put("locationName", trigger.locationName)
                            put("latitude", trigger.latitude)
                            put("longitude", trigger.longitude)
                            put("radius", trigger.radius)
                            put("triggerOnEntry", trigger.triggerOnEntry)
                            put("triggerOnExit", trigger.triggerOnExit)
                        }
                        is Trigger.TimeTrigger -> {
                            put("time", trigger.time)
                            put("days", JSONArray(trigger.days))
                        }
                        is Trigger.WiFiTrigger -> {
                            trigger.ssid?.let { put("ssid", it) }
                            put("state", trigger.state)
                        }
                        is Trigger.BluetoothTrigger -> {
                            put("deviceAddress", trigger.deviceAddress)
                            trigger.deviceName?.let { put("deviceName", it) }
                        }
                        is Trigger.BatteryTrigger -> {
                            put("level", trigger.level)
                            put("condition", trigger.condition)
                        }
                    }
                }
                triggersJsonArray.put(triggerJson)
            }

            // Build actions JSON array
            val actionsJsonArray = JSONArray()
            actions.forEach { action ->
                val actionJson = JSONObject().apply {
                    put("type", action.type)
                    action.value?.let { put("value", it) }
                    action.title?.let { put("title", it) }
                    action.message?.let { put("message", it) }
                    action.priority?.let { put("priority", it) }
                }
                actionsJsonArray.put(actionJson)
            }

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
                    workflow.triggerDetails = triggersJsonArray.toString()
                    workflow.actionDetails = actionsJsonArray.toString()
                    workflow.triggerLogic = triggerLogic
                    workflow.isEnabled = true

                    // Save
                    repository.update(workflow, object : WorkflowRepository.UpdateCallback {
                        override fun onUpdateComplete(success: Boolean) {
                            if (success) {
                                // ✅ Reschedule alarms after update
                                AlarmScheduler.cancelWorkflowAlarms(
                                    getApplication<Application>().applicationContext,
                                    workflowId
                                )
                                AlarmScheduler.scheduleWorkflow(
                                    getApplication<Application>().applicationContext,
                                    workflow
                                )

                                loadWorkflows()
                                val msg = "Workflow updated with ${triggers.size} trigger(s) and ${actions.size} action(s)"
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

    /**
     * ✅ FIXED: Delete workflow with proper cleanup
     */
    fun deleteWorkflow(workflowId: Long, callback: WorkflowOperationCallback? = null) {
        if (workflowId <= 0) {
            val error = "Invalid workflow ID"
            _errorMessage.value = error  // ✅ Use .value
            callback?.onError(error)
            return
        }

        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                if (workflow != null) {
                    try {
                        // ✅ FIXED: Parse as JSONArray, not JSONObject
                        val triggersArray = org.json.JSONArray(workflow.triggerDetails)

                        // Clean up each trigger
                        for (i in 0 until triggersArray.length()) {
                            val triggerJson = triggersArray.getJSONObject(i)
                            val triggerType = triggerJson.optString("type")

                            when (triggerType) {
                                Constants.TRIGGER_LOCATION -> {
                                    GeofenceManager.removeGeofence(
                                        getApplication<Application>().applicationContext,
                                        workflow.id
                                    )
                                    Log.d(TAG, "🚫 Removed geofence for workflow ${workflow.id}")
                                }
                                Constants.TRIGGER_TIME -> {
                                    AlarmScheduler.cancelWorkflowAlarms(
                                        getApplication<Application>().applicationContext,
                                        workflow.id
                                    )
                                    Log.d(TAG, "🚫 Cancelled alarms for workflow ${workflow.id}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error cleaning up triggers: ${e.message}", e)
                    }
                }

                // Now delete the workflow
                repository.delete(workflowId, object : WorkflowRepository.DeleteCallback {
                    override fun onDeleteComplete(success: Boolean) {
                        if (success) {
                            loadWorkflows()
                            val msg = "Workflow deleted"
                            _successMessage.value = msg  // ✅ Use .value
                            callback?.onSuccess(msg)
                        } else {
                            val error = "Delete failed"
                            _errorMessage.value = error  // ✅ Use .value
                            callback?.onError(error)
                        }
                    }

                    override fun onDeleteError(error: String) {
                        _errorMessage.value = error  // ✅ Use .value
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
                            _successMessage.value = msg  // ✅ Use .value
                            callback?.onSuccess(msg)
                        } else {
                            val error = "Delete failed"
                            _errorMessage.value = error  // ✅ Use .value
                            callback?.onError(error)
                        }
                    }

                    override fun onDeleteError(error: String) {
                        _errorMessage.value = error  // ✅ Use .value
                        callback?.onError(error)
                    }
                })
            }
        })
    }


    /**
     * ✅ FIXED: Update workflow enabled state with alarm handling
     */
    fun updateWorkflowEnabled(
        workflowId: Long,
        enabled: Boolean,
        callback: WorkflowOperationCallback? = null
    ) {
        // ✅ Validate workflow ID
        if (workflowId <= 0) {
            val error = "Invalid workflow ID: $workflowId"
            _errorMessage.postValue(error)
            callback?.onError(error)
            Log.w(TAG, "⚠️ $error")
            return
        }

        // Cancel/schedule alarms based on enabled state
        if (!enabled) {
            AlarmScheduler.cancelWorkflowAlarms(
                getApplication<Application>().applicationContext,
                workflowId
            )
            Log.d(TAG, "⏰ Cancelled alarms for disabled workflow $workflowId")
        } else {
            // Re-enable: need to reschedule
            repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
                override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                    workflow?.let {
                        AlarmScheduler.scheduleWorkflow(
                            getApplication<Application>().applicationContext,
                            it
                        )
                        Log.d(TAG, "⏰ Rescheduled alarms for enabled workflow $workflowId")
                    }
                }

                override fun onWorkflowError(error: String) {
                    Log.e(TAG, "Error rescheduling alarms: $error")
                }
            })
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
    // TRIGGER MONITORING
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
                isEnabled = true, // ✅ ADDED: Missing parameter
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

                    // Register triggers with correct ID
                    triggers.forEach { trigger ->
                        when (trigger) {
                            is Trigger.LocationTrigger -> {
                                GeofenceManager.addGeofence(
                                    context = getApplication<Application>().applicationContext,
                                    workflowId = insertedId,
                                    latitude = trigger.latitude,
                                    longitude = trigger.longitude,
                                    radius = trigger.radius.toFloat(),
                                    triggerOnEntry = trigger.triggerOnEntry,
                                    triggerOnExit = trigger.triggerOnExit
                                )
                            }
                            is Trigger.TimeTrigger -> {
                                // Time triggers handled by AlarmScheduler
                                Log.d(TAG, "⏰ Time trigger for mode workflow")
                            }
                            else -> {
                                Log.d(TAG, "Other trigger type: ${trigger.type}")
                            }
                        }
                    }

                    // Schedule alarms
                    AlarmScheduler.scheduleWorkflow(
                        getApplication<Application>().applicationContext,
                        savedWorkflow
                    )

                    // Reload and notify
                    loadWorkflows()
                    val msg = "Mode '${mode.name}' created successfully"
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

    private fun convertTemplateToTrigger(template: TriggerTemplate): Trigger {
        return when (template.type) {
            "TIME" -> {
                val time = template.config["time"] as? String ?: "00:00"
                val daysString = template.config["days"] as? String ?: ""
                val days = if (daysString.isBlank()) emptyList()
                else daysString.split(",").map { it.trim() }

                Trigger.TimeTrigger(
                    time = time,
                    days = days
                )
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
                    is Number -> rad.toDouble() // ✅ CHANGED: Convert to Double, not Int
                    is String -> rad.toDoubleOrNull() ?: 100.0
                    else -> 100.0
                }

                Trigger.LocationTrigger(
                    locationName = locationName,
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius, // Now it's Double
                    triggerOnEntry = template.config["triggerOnEntry"] as? Boolean ?: true,
                    triggerOnExit = template.config["triggerOnExit"] as? Boolean ?: false,
                    triggerOn = template.config["triggerOn"] as? String ?: "enter"
                )
            }
            "WIFI" -> {
                Trigger.WiFiTrigger(
                    ssid = template.config["ssid"] as? String,
                    state = template.config["state"] as? String ?: "connected"
                )
            }
            "BLUETOOTH" -> {
                Trigger.BluetoothTrigger(
                    deviceAddress = template.config["deviceAddress"] as? String ?: "",
                    deviceName = template.config["deviceName"] as? String
                )
            }
            else -> throw IllegalArgumentException("Unknown trigger type: ${template.type}")
        }
    }
    private fun convertTemplateToAction(template: ActionTemplate): Action {
        return when (template.type) {
            "SEND_NOTIFICATION" -> {
                // Use 4-parameter constructor for notifications
                Action(
                    type = template.type,
                    title = template.config["title"] as? String ?: "AutoFlow",
                    message = template.config["message"] as? String ?: "Notification",
                    priority = template.config["priority"] as? String ?: "default"
                )
            }
            "SET_SOUND_MODE", "TOGGLE_WIFI", "TOGGLE_BLUETOOTH" -> {
                // Use simple constructor, then set value manually
                val action = Action(type = template.type)
                action.value = template.config["value"] as? String
                action
            }
            else -> {
                // Default: simple constructor with optional value
                val action = Action(type = template.type)
                action.value = template.config["value"] as? String
                action
            }
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

    // CALLBACK INTERFACES

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

    /**
     * Restore all geofences for enabled workflows
     * Call this ONLY when app starts (e.g., in MainActivity.onCreate())
     */
    /**
     * Restore all geofences for enabled workflows
     * Call this ONLY when app starts (e.g., in MainActivity.onCreate())
     */
    fun restoreGeofences() {
        Log.d(TAG, "🔄 Restoring geofences for all enabled workflows...")

        repository.getAllWorkflows(object : WorkflowRepository.WorkflowCallback {
            override fun onWorkflowsLoaded(workflows: MutableList<WorkflowEntity>) {
                var restoredCount = 0

                workflows.forEach { workflow ->
                    // ✅ Only restore if workflow is enabled AND has valid ID
                    if (workflow.isEnabled && workflow.id > 0) {
                        try {
                            // ✅ Use toTriggers() extension function
                            val triggers = workflow.toTriggers()

                            triggers.forEach { trigger ->
                                if (trigger is Trigger.LocationTrigger) {
                                    val success = GeofenceManager.addGeofence(
                                        context = getApplication<Application>().applicationContext,
                                        workflowId = workflow.id,
                                        latitude = trigger.latitude,
                                        longitude = trigger.longitude,
                                        radius = trigger.radius.toFloat(),
                                        triggerOnEntry = trigger.triggerOnEntry,
                                        triggerOnExit = trigger.triggerOnExit
                                    )

                                    if (success) {
                                        restoredCount++
                                        Log.d(TAG, "✅ Restored geofence for workflow ${workflow.id}: ${workflow.workflowName}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error restoring geofence for workflow ${workflow.id}: ${e.message}", e)
                        }
                    }
                }

                Log.d(TAG, "✅ Restored $restoredCount geofences")
            }

            override fun onWorkflowsError(error: String) {
                Log.e(TAG, "❌ Failed to restore geofences: $error")
            }
        })
    }
    // LIFECYCLE

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
