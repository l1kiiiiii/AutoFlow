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
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.data.toActions
import com.example.autoflow.data.toTriggers
import com.example.autoflow.geofence.GeofenceManager
import com.example.autoflow.integrations.BLEManager
import com.example.autoflow.integrations.LocationManager
import com.example.autoflow.integrations.WiFiManager
import com.example.autoflow.integrations.PhoneStateManager
import com.example.autoflow.model.Action
import com.example.autoflow.model.ActionTemplate
import com.example.autoflow.model.ModeTemplate
import com.example.autoflow.model.NotificationType
import com.example.autoflow.model.Trigger
import com.example.autoflow.model.TriggerHelpers
import com.example.autoflow.model.TriggerTemplate
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.AlarmScheduler
import com.example.autoflow.util.AutoReplyManager
import com.example.autoflow.util.Constants
import com.example.autoflow.util.InAppNotificationManager
import com.example.autoflow.util.PermissionUtils
import com.example.autoflow.util.TriggerParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * ✅ Refactored WorkflowViewModel using Kotlin Coroutines
 * Manages workflow CRUD operations and trigger monitoring without Callbacks.
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

    // ============================================================================================
    // WORKFLOW CRUD OPERATIONS (Refactored to Coroutines)
    // ============================================================================================

    fun loadWorkflows() {
        viewModelScope.launch {
            try {
                val list = repository.getAllWorkflowsList()
                Log.d(TAG, "✅ Loaded ${list.size} workflows from database")
                list.forEach {
                    Log.d(TAG, " - ${it.workflowName} (ID: ${it.id})")
                }
                _workflows.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load workflows", e)
                _errorMessage.value = "Failed to load workflows: ${e.message}"
            }
        }
    }

    fun executeManualWorkflow(workflowId: Long) {
        viewModelScope.launch {
            Log.d(TAG, "🎯 Executing manual workflow: $workflowId")

            val workflow = repository.getWorkflowById(workflowId)

            if (workflow != null) {
                if (workflow.isEnabled) {
                    Log.d(TAG, "▶️ Starting manual execution: ${workflow.workflowName}")

                    val actions = workflow.toActions()
                    var successCount = 0

                    withContext(Dispatchers.IO) {
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
                    }

                    Log.d(TAG, "🎉 Manual execution complete: $successCount/${actions.size} actions succeeded")
                } else {
                    Log.w(TAG, "⚠️ Workflow disabled: ${workflow.workflowName}")
                }
            } else {
                Log.e(TAG, "❌ Error loading manual workflow: ID $workflowId not found")
            }
        }
    }

    fun addWorkflow(
        workflowName: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        triggerLogic: String = "AND",
        onWorkflowCreated: ((Long) -> Unit)? = null
    ) {
        if (workflowName.isBlank()) {
            _errorMessage.value = "Workflow name cannot be empty"
            return
        }
        if (triggers.isEmpty()) {
            _errorMessage.value = "At least one trigger is required"
            return
        }
        if (actions.isEmpty()) {
            _errorMessage.value = "At least one action is required"
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔵 addWorkflow - Name: $workflowName")

                val workflowEntity = WorkflowEntity.fromTriggersAndActions(
                    workflowName = workflowName.trim(),
                    isEnabled = true,
                    triggers = triggers,
                    actions = actions,
                    triggerLogic = triggerLogic
                )

                if (workflowEntity == null) {
                    _errorMessage.value = "Failed to create workflow entity"
                    return@launch
                }

                val insertedId = repository.insert(workflowEntity)
                Log.d(TAG, "🎉 Workflow inserted - ID: $insertedId")

                val savedWorkflow = workflowEntity.copy(id = insertedId)

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
                                if (success) Log.d(TAG, "✅ Geofence registered for workflow $insertedId")
                            }
                        }
                        "TIME" -> Log.d(TAG, "⏰ Time trigger will be scheduled by AlarmScheduler")
                        "WIFI" -> Log.d(TAG, "📶 WiFi trigger registered for workflow $insertedId")
                        "BLUETOOTH" -> Log.d(TAG, "📡 Bluetooth trigger registered for workflow $insertedId")
                        "BATTERY" -> Log.d(TAG, "🔋 Battery trigger registered for workflow $insertedId")
                    }
                }

                AlarmScheduler.scheduleWorkflow(
                    getApplication<Application>().applicationContext,
                    savedWorkflow
                )

                loadWorkflows()
                _successMessage.value = "Workflow '$workflowName' created successfully"

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating workflow", e)
                _errorMessage.value = "Error creating workflow: ${e.message}"
            }
        }
    }

    fun updateWorkflow(
        workflowId: Long,
        workflowName: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        callback: WorkflowOperationCallback? = null,
        triggerLogic: String = "AND"
    ) {
        if (workflowId <= 0) {
            val error = "Invalid workflow ID"
            _errorMessage.value = error
            callback?.onError(error)
            return
        }

        viewModelScope.launch {
            try {
                val triggersJsonArray = JSONArray()
                triggers.forEach { trigger ->
                    val triggerJson = JSONObject().apply {
                        put("type", trigger.type)
                        put("value", trigger.value)
                    }
                    triggersJsonArray.put(triggerJson)
                }

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

                val workflow = repository.getWorkflowById(workflowId)

                if (workflow == null) {
                    val error = "Workflow not found"
                    _errorMessage.value = error
                    callback?.onError(error)
                    return@launch
                }

                workflow.workflowName = workflowName.trim()
                workflow.triggerDetails = triggersJsonArray.toString()
                workflow.actionDetails = actionsJsonArray.toString()
                workflow.triggerLogic = triggerLogic
                workflow.isEnabled = true

                repository.update(workflow)

                AlarmScheduler.cancelWorkflowAlarms(getApplication<Application>().applicationContext, workflowId)
                AlarmScheduler.scheduleWorkflow(getApplication<Application>().applicationContext, workflow)

                loadWorkflows()
                val msg = "Workflow updated successfully"
                _successMessage.value = msg
                callback?.onSuccess(msg)

            } catch (e: Exception) {
                val error = "Error: ${e.message}"
                _errorMessage.value = error
                callback?.onError(error)
            }
        }
    }

    fun deleteWorkflow(workflowId: Long, callback: WorkflowOperationCallback? = null) {
        if (workflowId <= 0) return

        viewModelScope.launch {
            try {
                val workflow = repository.getWorkflowById(workflowId)

                if (workflow != null) {
                    val triggers = workflow.toTriggers()
                    triggers.forEach { trigger ->
                        when (trigger.type) {
                            Constants.TRIGGER_LOCATION -> {
                                GeofenceManager.removeGeofence(getApplication<Application>().applicationContext, workflow.id)
                            }
                            Constants.TRIGGER_TIME -> {
                                AlarmScheduler.cancelWorkflowAlarms(getApplication<Application>().applicationContext, workflow.id)
                            }
                        }
                    }
                }

                repository.delete(workflowId)

                loadWorkflows()
                val msg = "Workflow deleted"
                _successMessage.value = msg
                callback?.onSuccess(msg)

            } catch (e: Exception) {
                val error = "Delete failed: ${e.message}"
                _errorMessage.value = error
                callback?.onError(error)
            }
        }
    }

    fun updateWorkflowEnabled(
        workflowId: Long,
        enabled: Boolean,
        callback: WorkflowOperationCallback? = null
    ) {
        if (workflowId <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workflow = repository.getWorkflowById(workflowId)

                if (workflow != null) {
                    val triggers = workflow.toTriggers()
                    val isManualWorkflow = triggers.any { it.type == "MANUAL" }
                    val context = getApplication<Application>().applicationContext

                    if (enabled) {
                        if (isManualWorkflow) {
                            Log.d(TAG, "🤝 Manual workflow detected - executing immediately")
                            handleManualWorkflowActivation(context, workflow)
                            ActionExecutor.executeWorkflow(context, workflow)
                        } else {
                            AlarmScheduler.scheduleWorkflow(context, workflow)
                        }
                    } else {
                        if (isManualWorkflow) {
                            Log.d(TAG, "🤝 Manual workflow disabled - restoring previous state")
                            handleManualWorkflowDeactivation(context, workflow)
                        }
                        AlarmScheduler.cancelWorkflowAlarms(context, workflowId)
                    }

                    repository.updateWorkflowEnabled(workflowId, enabled)

                    withContext(Dispatchers.Main) {
                        loadWorkflows()
                        val msg = "Workflow ${if (enabled) "enabled" else "disabled"}"
                        _successMessage.value = msg
                        callback?.onSuccess(msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val error = "Toggle failed: ${e.message}"
                    _errorMessage.value = error
                    callback?.onError(error)
                }
            }
        }
    }

    private fun handleManualWorkflowActivation(context: Context, wf: WorkflowEntity) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("previous_ringer_mode", audioManager.ringerMode)
            .putBoolean("previous_dnd_state", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL else false)
            .apply()

        if (wf.workflowName.contains("Meeting Mode", ignoreCase = true)) {
            prefs.edit()
                .putBoolean(Constants.PREF_AUTO_REPLY_ENABLED, true)
                .putBoolean(Constants.PREF_MANUAL_MEETING_MODE, true)
                .apply()
            PhoneStateManager.getInstance(context).startListening()
        }
    }

    private fun handleManualWorkflowDeactivation(context: Context, wf: WorkflowEntity) {
        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val prevRinger = prefs.getInt("previous_ringer_mode", AudioManager.RINGER_MODE_NORMAL)
        audioManager.ringerMode = prevRinger

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && notificationManager.isNotificationPolicyAccessGranted) {
            val prevDnd = prefs.getBoolean("previous_dnd_state", false)
            notificationManager.setInterruptionFilter(if (prevDnd) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        if (wf.workflowName.contains("Meeting Mode", ignoreCase = true)) {
            prefs.edit()
                .putBoolean(Constants.PREF_AUTO_REPLY_ENABLED, false)
                .putBoolean(Constants.PREF_MANUAL_MEETING_MODE, false)
                .apply()
            PhoneStateManager.getInstance(context).stopListening()

            InAppNotificationManager.getInstance(context).addNotification(
                NotificationType.SUCCESS, "🔔 Meeting Mode Deactivated", "Sound mode restored."
            )
        }
    }

    suspend fun getWorkflowById(workflowId: Long): WorkflowEntity? {
        return repository.getWorkflowById(workflowId)
    }

    fun getWorkflowById(workflowId: Long, callback: WorkflowByIdCallback) {
        viewModelScope.launch {
            val wf = repository.getWorkflowById(workflowId)
            if (wf != null) callback.onWorkflowLoaded(wf)
            else callback.onWorkflowError("Not found")
        }
    }

    // ============================================================================================
    // TRIGGER MONITORING
    // ============================================================================================

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION])
    fun checkTrigger(trigger: Trigger, callback: TriggerCallback) {
        when (trigger.type) {
            Constants.TRIGGER_BLE -> handleBleTrigger(trigger, callback)
            Constants.TRIGGER_LOCATION -> handleLocationTrigger(trigger, callback)
            Constants.TRIGGER_TIME -> handleTimeTrigger(trigger, callback)
            Constants.TRIGGER_WIFI -> handleWiFiTrigger(trigger, callback)
            else -> callback.onTriggerFired(trigger, false)
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun handleBleTrigger(trigger: Trigger, callback: TriggerCallback) {
        if (!PermissionUtils.hasBluetoothPermissions(getApplication())) {
            callback.onTriggerFired(trigger, false)
            return
        }
        try {
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
                override fun onError(errorMessage: String) { callback.onTriggerFired(trigger, false) }
            })
        } catch (e: Exception) { callback.onTriggerFired(trigger, false) }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun handleLocationTrigger(trigger: Trigger, callback: TriggerCallback) {
        if (!PermissionUtils.hasLocationPermissions(getApplication())) {
            callback.onTriggerFired(trigger, false)
            return
        }
        val locationData = TriggerParser.parseLocationData(trigger) ?: return callback.onTriggerFired(trigger, false)

        locationManager.getLastLocation(object : LocationManager.Callback {
            override fun onLocationReceived(location: Location) {
                callback.onTriggerFired(trigger, isInRange(location, locationData))
            }
            override fun onLocationError(errorMessage: String) { callback.onTriggerFired(trigger, false) }
            override fun onPermissionDenied() { callback.onTriggerFired(trigger, false) }
        })
    }

    private fun handleTimeTrigger(trigger: Trigger, callback: TriggerCallback) {
        val timeData = TriggerParser.parseTimeData(trigger) ?: return callback.onTriggerFired(trigger, false)
        val (targetTime, _) = timeData
        val currentTime = System.currentTimeMillis()
        val timeParts = targetTime.split(":")
        if (timeParts.size != 2) return callback.onTriggerFired(trigger, false)

        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        calendar.set(java.util.Calendar.MINUTE, timeParts[1].toInt())
        calendar.set(java.util.Calendar.SECOND, 0)

        val targetTimeMs = calendar.timeInMillis
        callback.onTriggerFired(trigger, currentTime >= targetTimeMs && (currentTime - targetTimeMs) <= Constants.TIME_WINDOW_MS)
    }

    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    private fun handleWiFiTrigger(trigger: Trigger, callback: TriggerCallback) {
        if (!hasWiFiPermissions()) return callback.onTriggerFired(trigger, false)
        val wifiData = TriggerParser.parseWifiData(trigger) ?: return callback.onTriggerFired(trigger, false)
        val wifiState = wifiManager.isWiFiEnabled()
        val expectedState = Constants.WIFI_STATE_ON.equals(wifiData.state, ignoreCase = true)
        callback.onTriggerFired(trigger, wifiState == expectedState)
    }

    private fun hasWiFiPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
    }

    private fun isInRange(location: Location, locationData: com.example.autoflow.util.LocationData): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, locationData.latitude, locationData.longitude, results)
        return results[0] <= locationData.radius
    }

    // ============================================================================================
    // MODE CREATION
    // ============================================================================================

    fun createWorkflowFromMode(mode: ModeTemplate, callback: WorkflowOperationCallback? = null) {
        viewModelScope.launch {
            try {
                val triggers = mode.defaultTriggers.map { convertTemplateToTrigger(it) }
                val actions = mode.defaultActions.map { convertTemplateToAction(it) }

                if (triggers.isEmpty() || actions.isEmpty()) {
                    _errorMessage.value = "Mode must have triggers and actions"
                    callback?.onError("Mode must have triggers and actions")
                    return@launch
                }

                val workflowEntity = WorkflowEntity.fromTriggersAndActions(
                    workflowName = mode.name,
                    isEnabled = true,
                    triggers = triggers,
                    actions = actions,
                    triggerLogic = "AND"
                ).apply { this?.isModeWorkflow = true }

                if (workflowEntity == null) return@launch

                val insertedId = repository.insert(workflowEntity)
                val savedWorkflow = workflowEntity.copy(id = insertedId)

                val hasManualTrigger = triggers.any { it.type == "MANUAL" }
                val hasTimeTrigger = triggers.any { it.type == "TIME" }

                if (hasManualTrigger) {
                    ActionExecutor.executeWorkflow(getApplication(), savedWorkflow)
                } else if (hasTimeTrigger) {
                    AlarmScheduler.scheduleWorkflow(getApplication(), savedWorkflow)
                }

                registerWorkflowTriggers(savedWorkflow, triggers)

                loadWorkflows()
                val msg = "Mode '${mode.name}' created!"
                _successMessage.value = msg
                callback?.onSuccess(msg)

            } catch (e: Exception) {
                _errorMessage.value = "Error creating mode: ${e.message}"
                callback?.onError(e.message ?: "Error")
            }
        }
    }

    private fun registerWorkflowTriggers(workflow: WorkflowEntity, triggers: List<Trigger>) {
        triggers.forEach { trigger ->
            if (trigger.type == "LOCATION") {
                val locationData = TriggerParser.parseLocationData(trigger)
                locationData?.let { data ->
                    GeofenceManager.addGeofence(
                        getApplication(),
                        workflow.id,
                        data.latitude, data.longitude, data.radius.toFloat(), data.triggerOnEntry, data.triggerOnExit)
                }
            }
        }
    }

    private fun convertTemplateToTrigger(template: TriggerTemplate): Trigger {
        return when (template.type) {
            "TIME" -> {
                val time = template.config["time"] as? String ?: "00:00"
                val days = (template.config["days"] as? String)?.split(",")?.map { it.trim() } ?: emptyList()
                TriggerHelpers.createTimeTrigger(time, days)
            }
            "LOCATION" -> {
                val lat = (template.config["latitude"] as? Number)?.toDouble() ?: 0.0
                val lng = (template.config["longitude"] as? Number)?.toDouble() ?: 0.0
                val rad = (template.config["radius"] as? Number)?.toDouble() ?: 100.0
                TriggerHelpers.createLocationTrigger(template.config["locationName"] as? String ?: "Loc", lat, lng, rad, true, false)
            }
            "MANUAL" -> Trigger.ManualTrigger(template.config["type"] as? String ?: "quick_action")
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    private fun convertTemplateToAction(template: ActionTemplate): Action {
        return Action(template.type, template.config["value"] as? String).apply {
            title = template.config["title"] as? String
            message = template.config["message"] as? String
            priority = template.config["priority"] as? String ?: "Normal"
            if (template.type == "AUTO_REPLY" && message == null) message = "Busy."
        }
    }

    fun stopAllTriggers() {
        try {
            bleManager.stopScanning()
            wifiManager.stopMonitoring()
            Log.d(TAG, "✅ All triggers stopped")
        } catch (e: Exception) { Log.e(TAG, "Error stopping triggers", e) }
    }

    fun restoreGeofences() {
        viewModelScope.launch {
            val workflows = repository.getAllWorkflowsList()
            workflows.forEach { workflow ->
                if (workflow.isEnabled) registerWorkflowTriggers(workflow, workflow.toTriggers())
            }
        }
    }

    fun createMeetingModeWorkflow(workflowName: String = "Meeting Mode", autoReplyMessage: String = Constants.DEFAULT_AUTO_REPLY_MESSAGE, callback: WorkflowOperationCallback? = null) {
        val triggers = listOf(Trigger.ManualTrigger(actionType = "quick_action"))
        val actions = listOf(
            Action(Constants.ACTION_SET_SOUND_MODE, "DND").apply { title = "Set Sound Mode"; message = "DND mode activated" },
            Action(Constants.ACTION_AUTO_REPLY_SMS, "true").apply { title = "Auto-reply enabled"; message = autoReplyMessage }
        )
        addWorkflow(workflowName, triggers, actions, "AND")
    }

    fun toggleAutoReply(enabled: Boolean, message: String = Constants.DEFAULT_AUTO_REPLY_MESSAGE) {
        val prefs = getApplication<Application>().getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PREF_AUTO_REPLY_ENABLED, enabled).putString(Constants.PREF_AUTO_REPLY_MESSAGE, message).apply()
        if (enabled) PhoneStateManager.getInstance(getApplication()).startListening()
        else PhoneStateManager.getInstance(getApplication()).stopListening()
        _successMessage.value = if (enabled) "Auto-reply enabled" else "Auto-reply disabled"
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.cleanup()
        wifiManager.cleanup()
        locationManager.cleanup()
        PhoneStateManager.getInstance(getApplication()).cleanup()
        AutoReplyManager.getInstance(getApplication()).cleanup()
    }

    // Callback interfaces
    interface WorkflowOperationCallback { fun onSuccess(message: String); fun onError(error: String) }
    interface WorkflowByIdCallback { fun onWorkflowLoaded(workflow: WorkflowEntity?); fun onWorkflowError(error: String) }
    interface TriggerCallback { fun onTriggerFired(trigger: Trigger, isFired: Boolean) }
}