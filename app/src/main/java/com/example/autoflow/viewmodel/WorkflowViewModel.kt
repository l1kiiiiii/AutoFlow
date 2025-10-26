package com.example.autoflow.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toActions
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
import com.example.autoflow.integrations.PhoneStateManager
import com.example.autoflow.model.ActionTemplate
import com.example.autoflow.model.ModeTemplate
import com.example.autoflow.model.TriggerTemplate
import com.example.autoflow.util.AutoReplyManager
import com.example.autoflow.util.TriggerParser
import com.example.autoflow.model.TriggerHelpers
import com.example.autoflow.util.ActionExecutor

/**
 * ✅ Fixed WorkflowViewModel using TriggerParser approach
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
    // When user taps on a manual workflow (Meeting Mode), execute actions immediately
    fun executeManualWorkflow(workflowId: Long) {
        Log.d(TAG, "🎯 Executing manual workflow: $workflowId")

        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                workflow?.let { w ->
                    if (w.isEnabled) {
                        Log.d(TAG, "▶️ Starting manual execution: ${w.workflowName}")

                        // Execute all actions immediately
                        val actions = w.toActions()
                        var successCount = 0

                        actions.forEach { action ->
                            Log.d(TAG, "🔧 Executing action: ${action.type} = ${action.value}")
                            val success = ActionExecutor.executeAction(getApplication(), action)
                            if (success) {
                                successCount++
                                Log.d(TAG, "✅ Action succeeded: ${action.type}")
                            } else {
                                Log.e(TAG, "❌ Action failed: ${action.type}")
                            }
                        }

                        Log.d(TAG, "🎉 Manual execution complete: $successCount/${actions.size} actions succeeded")
                    } else {
                        Log.w(TAG, "⚠️ Workflow disabled: ${w.workflowName}")
                    }
                }
            }

            override fun onWorkflowError(error: String) {
                Log.e(TAG, "❌ Error loading manual workflow: $error")
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
            // ✅ FIXED: Build triggers JSON array using new approach
            val triggersJsonArray = JSONArray()
            triggers.forEach { trigger ->
                val triggerJson = JSONObject().apply {
                    put("type", trigger.type)
                    put("value", trigger.value)
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
            _errorMessage.value = error
            callback?.onError(error)
            return
        }

        repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
            override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                if (workflow != null) {
                    try {
                        // ✅ FIXED: Clean up using toTriggers() extension
                        val triggers = workflow.toTriggers()

                        triggers.forEach { trigger ->
                            when (trigger.type) {
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
                            _successMessage.value = msg
                            callback?.onSuccess(msg)
                        } else {
                            val error = "Delete failed"
                            _errorMessage.value = error
                            callback?.onError(error)
                        }
                    }

                    override fun onDeleteError(error: String) {
                        _errorMessage.value = error
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
                            _successMessage.value = msg
                            callback?.onSuccess(msg)
                        } else {
                            val error = "Delete failed"
                            _errorMessage.value = error
                            callback?.onError(error)
                        }
                    }

                    override fun onDeleteError(error: String) {
                        _errorMessage.value = error
                        callback?.onError(error)
                    }
                })
            }
        })
    }

    /**
     * ✅ Update workflow enabled state with alarm handling
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

        // ✅ SMART STATE MANAGEMENT FOR MANUAL WORKFLOWS
        if (enabled) {
            repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
                override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                    workflow?.let { wf ->
                        Log.d(TAG, "📋 Checking workflow: ${wf.workflowName}")

                        // Check if this is a manual workflow
                        val triggers = wf.toTriggers()
                        val isManualWorkflow = triggers.any { trigger -> trigger.type == "MANUAL" }

                        if (isManualWorkflow) {
                            Log.d(TAG, "🤝 Manual workflow detected - saving current state first")

                            // ✅ SAVE CURRENT STATE BEFORE ENABLING DND
                            val context = getApplication<Application>().applicationContext
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                            // Save current ringer mode
                            val currentRingerMode = audioManager.ringerMode

                            // Save current DND state
                            var currentDndState = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                currentDndState = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                            }

                            // Store previous state in SharedPreferences
                            val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putInt("previous_ringer_mode", currentRingerMode)
                                .putBoolean("previous_dnd_state", currentDndState)
                                .putBoolean("manual_meeting_mode", true)
                                .apply()

                            Log.d(TAG, "💾 Saved previous state: Ringer=$currentRingerMode, DND=$currentDndState")


                            //  EXECUTE ACTIONS IMMEDIATELY - Enhanced with direct execution
                            Log.d(TAG, "🎯 Starting manual workflow execution...")

                            val actions = wf.toActions()
                            Log.d(TAG, "🎯 Found ${actions.size} actions to execute")

                            var successCount = 0
                            actions.forEach { action ->
                                Log.d(TAG, "🔧 Executing action: ${action.type} = ${action.value}")
                                val actionSuccess = ActionExecutor.executeAction(context, action)
                                if (actionSuccess) {
                                    successCount++
                                    Log.d(TAG, "✅ Action succeeded: ${action.type}")
                                } else {
                                    Log.e(TAG, "❌ Action failed: ${action.type}")
                                }
                            }

                            Log.d(TAG, "📱 Manual workflow execution complete: $successCount/${actions.size} actions")
                            val success = successCount > 0

                            if (success) {
                                Log.d(TAG, "✅ Meeting Mode DND enabled successfully")
                            } else {
                                Log.e(TAG, "❌ Failed to execute Meeting Mode actions")
                            }


                            if (success) {
                                Log.d(TAG, "✅ Meeting Mode DND enabled successfully")
                            } else {
                                Log.e(TAG, "❌ Failed to execute Meeting Mode actions")
                            }
                        }

                        // Schedule any time-based triggers

                        AlarmScheduler.scheduleWorkflow(getApplication<Application>().applicationContext, wf)

                    }
                }

                override fun onWorkflowError(error: String) {
                    Log.e(TAG, "❌ Error loading workflow: $error")
                    callback?.onError(error)
                }
            })
        } else {
            // ✅ SMART RESTORATION WHEN DISABLING
            repository.getWorkflowById(workflowId, object : WorkflowRepository.WorkflowByIdCallback {
                override fun onWorkflowLoaded(workflow: WorkflowEntity?) {
                    workflow?.let { wf ->
                        val triggers = wf.toTriggers()
                        val isManualWorkflow = triggers.any { trigger -> trigger.type == "MANUAL" }

                        if (isManualWorkflow) {
                            Log.d(TAG, "🤝 Manual workflow disabled - restoring previous state")

                            val context = getApplication<Application>().applicationContext
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)

                            // ✅ RESTORE PREVIOUS RINGER MODE (not just normal)
                            val previousRingerMode = prefs.getInt("previous_ringer_mode", AudioManager.RINGER_MODE_NORMAL)
                            audioManager.ringerMode = previousRingerMode
                            Log.d(TAG, "🔊 Restored previous ringer mode: $previousRingerMode")

                            // ✅ RESTORE PREVIOUS DND STATE
                            val previousDndState = prefs.getBoolean("previous_dnd_state", false)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                try {
                                    if (notificationManager.isNotificationPolicyAccessGranted) {
                                        val targetFilter = if (previousDndState) {
                                            NotificationManager.INTERRUPTION_FILTER_PRIORITY
                                        } else {
                                            NotificationManager.INTERRUPTION_FILTER_ALL
                                        }
                                        notificationManager.setInterruptionFilter(targetFilter)
                                        Log.d(TAG, "🔔 Restored previous DND state: $previousDndState")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error restoring DND state: ${e.message}")
                                }
                            }

                            // Clear manual meeting mode flag
                            prefs.edit()
                                .putBoolean("manual_meeting_mode", false)
                                .apply()

                            Log.d(TAG, "✅ Previous state fully restored")
                        }
                    }
                }

                override fun onWorkflowError(error: String) {
                    Log.e(TAG, "❌ Error loading workflow for cleanup: $error")
                }
            })

            // Cancel alarms
            AlarmScheduler.cancelWorkflowAlarms(
                getApplication<Application>().applicationContext,
                workflowId
            )
            Log.d(TAG, "⏰ Cancelled alarms for disabled workflow $workflowId")
        }

        // ✅ Update database
        repository.updateWorkflowEnabled(workflowId, enabled, object : WorkflowRepository.UpdateCallback {
            override fun onUpdateComplete(success: Boolean) {
                if (success) {
                    loadWorkflows()
                    val msg = "Workflow ${if (enabled) "enabled" else "disabled"}"
                    _successMessage.postValue(msg)
                    callback?.onSuccess(msg)
                    Log.d(TAG, "✅ $msg successfully")
                } else {
                    val error = "Toggle failed"
                    _errorMessage.postValue(error)
                    callback?.onError(error)
                    Log.e(TAG, "❌ $error")
                }
            }

            override fun onUpdateError(error: String) {
                _errorMessage.postValue(error)
                callback?.onError(error)
                Log.e(TAG, "❌ Update error: $error")
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

    // ✅ TRIGGER MONITORING - FIXED using TriggerParser

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
            // ✅ FIXED: Parse Bluetooth data from JSON
            val bluetoothData = TriggerParser.parseBluetoothData(trigger)
            if (bluetoothData == null) {
                callback.onTriggerFired(trigger, false)
                return
            }

            bleManager.startScanning(object : BLEManager.BLECallback {
                override fun onDeviceDetected(deviceAddress: String, deviceName: String) {
                    val matched = deviceAddress == bluetoothData.deviceAddress ||
                            (bluetoothData.deviceName != null && deviceName == bluetoothData.deviceName)
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
            // ✅ FIXED: Parse location data from JSON
            val locationData = TriggerParser.parseLocationData(trigger)
            if (locationData == null) {
                callback.onTriggerFired(trigger, false)
                return
            }

            locationManager.getLastLocation(object : LocationManager.Callback {
                override fun onLocationReceived(location: Location) {
                    val inRange = isInRange(location, locationData)
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
            // ✅ FIXED: Parse time data from JSON
            val timeData = TriggerParser.parseTimeData(trigger)
            if (timeData == null) {
                callback.onTriggerFired(trigger, false)
                return
            }

            val (targetTime, days) = timeData
            val currentTime = System.currentTimeMillis()

            // Convert time string to milliseconds for comparison
            val timeParts = targetTime.split(":")
            if (timeParts.size != 2) {
                callback.onTriggerFired(trigger, false)
                return
            }

            val targetHour = timeParts[0].toIntOrNull() ?: 0
            val targetMinute = timeParts[1].toIntOrNull() ?: 0

            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            calendar.set(java.util.Calendar.MINUTE, targetMinute)
            calendar.set(java.util.Calendar.SECOND, 0)

            val targetTimeMs = calendar.timeInMillis
            val isTriggered = currentTime >= targetTimeMs &&
                    (currentTime - targetTimeMs) <= Constants.TIME_WINDOW_MS

            callback.onTriggerFired(trigger, isTriggered)
        } catch (e: Exception) {
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
            // ✅ FIXED: Parse WiFi data from JSON
            val wifiData = TriggerParser.parseWifiData(trigger)
            if (wifiData == null) {
                callback.onTriggerFired(trigger, false)
                return
            }

            val wifiState = wifiManager.isWiFiEnabled()
            val expectedState = Constants.WIFI_STATE_ON.equals(wifiData.state, ignoreCase = true)
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

    // ✅ FIXED: Updated location range checking
    private fun isInRange(location: Location, locationData: com.example.autoflow.util.LocationData): Boolean {
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                locationData.latitude, locationData.longitude, results
            )

            results[0] <= locationData.radius
        } catch (e: Exception) {
            Log.e(TAG, "Location range error", e)
            false
        }
    }

    // ✅ FIXED: Mode creation using TriggerHelpers
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

                    // ✅ FIXED: Register triggers using TriggerParser
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
                            "TIME" -> {
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

    // ✅ CALLBACK INTERFACES

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
     * ✅ FIXED: Restore all geofences using TriggerParser
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
                                if (trigger.type == "LOCATION") {
                                    val locationData = TriggerParser.parseLocationData(trigger)
                                    locationData?.let { data ->
                                        val success = GeofenceManager.addGeofence(
                                            context = getApplication<Application>().applicationContext,
                                            workflowId = workflow.id,
                                            latitude = data.latitude,
                                            longitude = data.longitude,
                                            radius = data.radius.toFloat(),
                                            triggerOnEntry = data.triggerOnEntry,
                                            triggerOnExit = data.triggerOnExit
                                        )

                                        if (success) {
                                            restoredCount++
                                            Log.d(TAG, "✅ Restored geofence for workflow ${workflow.id}: ${workflow.workflowName}")
                                        }
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

    // ✅ LIFECYCLE

    fun createMeetingModeWorkflow(
        workflowName: String = "Meeting Mode",
        autoReplyMessage: String = Constants.DEFAULT_AUTO_REPLY_MESSAGE,
        callback: WorkflowOperationCallback? = null
    ) {
        val triggers = listOf<Trigger>() // Manual trigger - user enables it

        val actions = listOf(
            // ✅ FIXED: Use explicit constructor with proper parameters
            Action(
                type = Constants.ACTION_SET_SOUND_MODE,
                title = "Set Sound Mode",
                message = "DND mode activated",
                priority = "high"
            ).apply {
                value = "DND" // Set value after construction
            },

            Action(
                type = Constants.ACTION_AUTO_REPLY_SMS,
                title = "Auto-reply enabled",
                message = autoReplyMessage,
                priority = "normal"
            ).apply {
                value = "true" // Enable auto-reply
            }
        )

        addWorkflow(workflowName, triggers, actions, "AND")
    }

    // ✅ NEW: Toggle auto-reply for existing workflows
    fun toggleAutoReply(enabled: Boolean, message: String = Constants.DEFAULT_AUTO_REPLY_MESSAGE) {
        val prefs = getApplication<Application>().getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(Constants.PREF_AUTO_REPLY_ENABLED, enabled)
            .putString(Constants.PREF_AUTO_REPLY_MESSAGE, message)
            .apply()

        val phoneStateManager = PhoneStateManager.getInstance(getApplication())
        if (enabled) {
            phoneStateManager.startListening()
            _successMessage.postValue("Auto-reply enabled: \"$message\"")
        } else {
            phoneStateManager.stopListening()
            _successMessage.postValue("Auto-reply disabled")
        }
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
