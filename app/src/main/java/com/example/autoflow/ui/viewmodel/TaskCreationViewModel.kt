package com.example.autoflow.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.repository.WorkflowRepositoryImpl
import com.example.autoflow.domain.model.Action
import com.example.autoflow.domain.model.Trigger
import com.example.autoflow.domain.usecase.SaveWorkflowUseCase
import com.example.autoflow.domain.usecase.ValidateWorkflowUseCase
import com.example.autoflow.ui.state.TaskCreationEvent
import com.example.autoflow.ui.state.TaskCreationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for TaskCreationScreen
 * Follows clean architecture principles:
 * - Manages UI state only
 * - Delegates business logic to UseCases
 * - Uses coroutines instead of callbacks
 */
class TaskCreationViewModel(application: Application) : AndroidViewModel(application) {
    
    // Dependencies
    private val repository: WorkflowRepositoryImpl
    private val saveWorkflowUseCase: SaveWorkflowUseCase
    private val validateWorkflowUseCase: ValidateWorkflowUseCase
    
    // UI State
    private val _uiState = MutableStateFlow(TaskCreationUiState())
    val uiState: StateFlow<TaskCreationUiState> = _uiState.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        val workflowDao = database.workflowDao()
        repository = WorkflowRepositoryImpl(workflowDao)
        saveWorkflowUseCase = SaveWorkflowUseCase(repository)
        validateWorkflowUseCase = ValidateWorkflowUseCase()
    }
    
    /**
     * Handle UI events
     */
    fun onEvent(event: TaskCreationEvent) {
        when (event) {
            is TaskCreationEvent.UpdateTaskName -> updateTaskName(event.name)
            is TaskCreationEvent.ToggleTrigger -> toggleTrigger(event.triggerType)
            is TaskCreationEvent.ToggleAction -> toggleAction(event.actionType)
            is TaskCreationEvent.UpdateLocationName -> updateLocationName(event.name)
            is TaskCreationEvent.UpdateLocationDetails -> updateLocationDetails(event.details)
            is TaskCreationEvent.UpdateRadius -> updateRadius(event.radius)
            is TaskCreationEvent.UpdateTriggerOnOption -> updateTriggerOnOption(event.option)
            is TaskCreationEvent.UpdateTimeValue -> updateTimeValue(event.time)
            is TaskCreationEvent.UpdateWifiState -> updateWifiState(event.state)
            is TaskCreationEvent.UpdateBluetoothAddress -> updateBluetoothAddress(event.address)
            is TaskCreationEvent.UpdateNotificationTitle -> updateNotificationTitle(event.title)
            is TaskCreationEvent.UpdateNotificationMessage -> updateNotificationMessage(event.message)
            is TaskCreationEvent.UpdateNotificationPriority -> updateNotificationPriority(event.priority)
            is TaskCreationEvent.UpdateToggleSetting -> updateToggleSetting(event.setting)
            is TaskCreationEvent.UpdateScriptText -> updateScriptText(event.script)
            is TaskCreationEvent.UpdateSoundMode -> updateSoundMode(event.mode)
            is TaskCreationEvent.UpdateSelectedApps -> updateSelectedApps(event.apps)
            TaskCreationEvent.SaveTask -> saveTask()
            TaskCreationEvent.DismissError -> dismissError()
            TaskCreationEvent.DismissSuccess -> dismissSuccess()
        }
    }
    
    private fun updateTaskName(name: String) {
        _uiState.value = _uiState.value.copy(
            taskName = name,
            taskNameError = null
        )
    }
    
    private fun toggleTrigger(triggerType: com.example.autoflow.ui.state.TriggerType) {
        val currentState = _uiState.value
        _uiState.value = when (triggerType) {
            com.example.autoflow.ui.state.TriggerType.LOCATION -> currentState.copy(
                locationTriggerExpanded = !currentState.locationTriggerExpanded
            )
            com.example.autoflow.ui.state.TriggerType.TIME -> currentState.copy(
                timeTriggerExpanded = !currentState.timeTriggerExpanded
            )
            com.example.autoflow.ui.state.TriggerType.WIFI -> currentState.copy(
                wifiTriggerExpanded = !currentState.wifiTriggerExpanded
            )
            com.example.autoflow.ui.state.TriggerType.BLUETOOTH -> currentState.copy(
                bluetoothDeviceTriggerExpanded = !currentState.bluetoothDeviceTriggerExpanded
            )
        }
    }
    
    private fun toggleAction(actionType: com.example.autoflow.ui.state.ActionType) {
        val currentState = _uiState.value
        _uiState.value = when (actionType) {
            com.example.autoflow.ui.state.ActionType.NOTIFICATION -> currentState.copy(
                sendNotificationActionExpanded = !currentState.sendNotificationActionExpanded
            )
            com.example.autoflow.ui.state.ActionType.TOGGLE_SETTINGS -> currentState.copy(
                toggleSettingsActionExpanded = !currentState.toggleSettingsActionExpanded
            )
            com.example.autoflow.ui.state.ActionType.SCRIPT -> currentState.copy(
                runScriptActionExpanded = !currentState.runScriptActionExpanded
            )
            com.example.autoflow.ui.state.ActionType.SOUND_MODE -> currentState.copy(
                setSoundModeActionExpanded = !currentState.setSoundModeActionExpanded
            )
            com.example.autoflow.ui.state.ActionType.BLOCK_APPS -> currentState.copy(
                blockAppsActionExpanded = !currentState.blockAppsActionExpanded
            )
            com.example.autoflow.ui.state.ActionType.UNBLOCK_APPS -> currentState.copy(
                unblockAppsActionExpanded = !currentState.unblockAppsActionExpanded
            )
        }
    }
    
    private fun updateLocationName(name: String) {
        _uiState.value = _uiState.value.copy(locationName = name)
    }
    
    private fun updateLocationDetails(details: String) {
        _uiState.value = _uiState.value.copy(locationDetailsInput = details)
    }
    
    private fun updateRadius(radius: Float) {
        _uiState.value = _uiState.value.copy(radiusValue = radius)
    }
    
    private fun updateTriggerOnOption(option: String) {
        _uiState.value = _uiState.value.copy(triggerOnOption = option)
    }
    
    private fun updateTimeValue(time: String) {
        _uiState.value = _uiState.value.copy(timeValue = time)
    }
    
    private fun updateWifiState(state: String) {
        _uiState.value = _uiState.value.copy(wifiState = state)
    }
    
    private fun updateBluetoothAddress(address: String) {
        _uiState.value = _uiState.value.copy(bluetoothDeviceAddress = address)
    }
    
    private fun updateNotificationTitle(title: String) {
        _uiState.value = _uiState.value.copy(notificationTitle = title)
    }
    
    private fun updateNotificationMessage(message: String) {
        _uiState.value = _uiState.value.copy(notificationMessage = message)
    }
    
    private fun updateNotificationPriority(priority: String) {
        _uiState.value = _uiState.value.copy(notificationPriority = priority)
    }
    
    private fun updateToggleSetting(setting: String) {
        _uiState.value = _uiState.value.copy(toggleSetting = setting)
    }
    
    private fun updateScriptText(script: String) {
        _uiState.value = _uiState.value.copy(scriptText = script)
    }
    
    private fun updateSoundMode(mode: String) {
        _uiState.value = _uiState.value.copy(soundMode = mode)
    }
    
    private fun updateSelectedApps(apps: List<String>) {
        _uiState.value = _uiState.value.copy(selectedAppsToBlock = apps)
    }
    
    private fun dismissError() {
        _uiState.value = _uiState.value.copy(
            showErrorDialog = false,
            errorMessage = ""
        )
    }
    
    private fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(showSuccessSnackbar = false)
    }
    
    /**
     * Save workflow using UseCases
     * Business logic is delegated to domain layer
     */
    private fun saveTask() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val state = _uiState.value
                
                // Build triggers list
                val triggers = buildTriggersList(state)
                
                // Build actions list
                val actions = buildActionsList(state)
                
                // Validate workflow
                val validationResult = validateWorkflowUseCase.execute(
                    workflowName = state.taskName,
                    triggers = triggers,
                    actions = actions
                )
                
                if (validationResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showErrorDialog = true,
                        errorMessage = validationResult.exceptionOrNull()?.message ?: "Validation failed"
                    )
                    return@launch
                }
                
                // Save workflow
                val saveResult = saveWorkflowUseCase.execute(
                    workflowName = state.taskName,
                    triggers = triggers,
                    actions = actions
                )
                
                if (saveResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showSuccessSnackbar = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showErrorDialog = true,
                        errorMessage = saveResult.exceptionOrNull()?.message ?: "Failed to save workflow"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showErrorDialog = true,
                    errorMessage = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Build triggers list from UI state
     * This logic is still in the ViewModel but is now much simpler
     */
    private fun buildTriggersList(state: TaskCreationUiState): List<Trigger> {
        val triggers = mutableListOf<Trigger>()
        
        // Location trigger
        if (state.locationTriggerExpanded && state.locationDetailsInput.isNotBlank()) {
            val parts = state.locationDetailsInput.split(",").map { it.trim() }
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                if (lat != null && lng != null) {
                    triggers.add(
                        com.example.autoflow.domain.model.TriggerHelpers.createLocationTrigger(
                            locationName = state.locationName.ifEmpty { "Unnamed Location" },
                            latitude = lat,
                            longitude = lng,
                            radius = state.radiusValue.toDouble(),
                            triggerOnEntry = state.triggerOnOption == "Entry" || state.triggerOnOption == "Both",
                            triggerOnExit = state.triggerOnOption == "Exit" || state.triggerOnOption == "Both"
                        )
                    )
                }
            }
        }
        
        // Time trigger
        if (state.timeTriggerExpanded && state.timeValue.isNotBlank()) {
            triggers.add(
                com.example.autoflow.domain.model.TriggerHelpers.createTimeTrigger(
                    state.timeValue,
                    emptyList()
                )
            )
        }
        
        // WiFi trigger
        if (state.wifiTriggerExpanded) {
            triggers.add(
                com.example.autoflow.domain.model.TriggerHelpers.createWifiTrigger(
                    null,
                    state.wifiState
                )
            )
        }
        
        // Bluetooth trigger
        if (state.bluetoothDeviceTriggerExpanded && state.bluetoothDeviceAddress.isNotBlank()) {
            triggers.add(
                com.example.autoflow.domain.model.TriggerHelpers.createBluetoothTrigger(
                    state.bluetoothDeviceAddress,
                    null
                )
            )
        }
        
        return triggers
    }
    
    /**
     * Build actions list from UI state
     */
    private fun buildActionsList(state: TaskCreationUiState): List<Action> {
        val actions = mutableListOf<Action>()
        
        // Notification action
        if (state.sendNotificationActionExpanded && state.notificationTitle.isNotBlank()) {
            actions.add(
                Action.createNotificationAction(
                    state.notificationTitle,
                    state.notificationMessage,
                    state.notificationPriority
                )
            )
        }
        
        // Toggle settings action
        if (state.toggleSettingsActionExpanded) {
            when (state.toggleSetting) {
                "WiFi" -> actions.add(Action(Action.TYPE_WIFI_TOGGLE, "ON"))
                "Bluetooth" -> actions.add(Action(Action.TYPE_BLUETOOTH_TOGGLE, "ON"))
            }
        }
        
        // Script action
        if (state.runScriptActionExpanded && state.scriptText.isNotBlank()) {
            actions.add(Action.createScriptAction(state.scriptText))
        }
        
        // Sound mode action
        if (state.setSoundModeActionExpanded) {
            actions.add(Action.createSoundModeAction(state.soundMode))
        }
        
        // Block apps action
        if (state.blockAppsActionExpanded && state.selectedAppsToBlock.isNotEmpty()) {
            actions.add(
                Action.createBlockAppsAction(
                    state.selectedAppsToBlock.joinToString(",")
                )
            )
        }
        
        return actions
    }
}
