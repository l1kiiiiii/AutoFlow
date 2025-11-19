package com.example.autoflow.ui.state

import com.example.autoflow.model.SavedBluetoothDevice
import com.example.autoflow.model.SavedWiFiNetwork

/**
 * Consolidated UI state for TaskCreationScreen
 * Implements MVI (Model-View-Intent) pattern
 * Reduces state management complexity from 30+ variables to a single data class
 */
data class TaskCreationUiState(
    // Task info
    val taskName: String = "",
    val taskNameError: String? = null,
    
    // Location trigger
    val locationTriggerExpanded: Boolean = false,
    val locationName: String = "",
    val locationDetailsInput: String = "",
    val radiusValue: Float = 100f,
    val triggerOnOption: String = "Entry",
    
    // Time trigger
    val timeTriggerExpanded: Boolean = false,
    val timeValue: String = "",
    
    // WiFi trigger
    val wifiTriggerExpanded: Boolean = false,
    val wifiState: String = "On",
    val selectedWifiNetwork: SavedWiFiNetwork? = null,
    val wifiSsid: String = "",
    val wifiTriggerType: String = "connect",
    
    // Bluetooth trigger
    val bluetoothDeviceTriggerExpanded: Boolean = false,
    val bluetoothDeviceAddress: String = "",
    val selectedBluetoothDevice: SavedBluetoothDevice? = null,
    val bluetoothMacAddress: String = "",
    val bluetoothDeviceName: String = "",
    val bluetoothTriggerType: String = "connect",
    
    // Notification action
    val sendNotificationActionExpanded: Boolean = false,
    val notificationTitle: String = "",
    val notificationMessage: String = "",
    val notificationPriority: String = "Normal",
    
    // Toggle settings action
    val toggleSettingsActionExpanded: Boolean = false,
    val toggleSetting: String = "WiFi",
    
    // Script action
    val runScriptActionExpanded: Boolean = false,
    val scriptText: String = "",
    
    // Sound mode action
    val setSoundModeActionExpanded: Boolean = false,
    val soundMode: String = "Normal",
    
    // Block apps action
    val blockAppsActionExpanded: Boolean = false,
    val selectedAppsToBlock: List<String> = emptyList(),
    
    // Unblock apps action
    val unblockAppsActionExpanded: Boolean = false,
    
    // UI feedback
    val showErrorDialog: Boolean = false,
    val errorMessage: String = "",
    val showSuccessSnackbar: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * UI Events for TaskCreationScreen
 * Represents user actions/intents
 */
sealed class TaskCreationEvent {
    data class UpdateTaskName(val name: String) : TaskCreationEvent()
    data class ToggleTrigger(val triggerType: TriggerType) : TaskCreationEvent()
    data class ToggleAction(val actionType: ActionType) : TaskCreationEvent()
    data class UpdateLocationName(val name: String) : TaskCreationEvent()
    data class UpdateLocationDetails(val details: String) : TaskCreationEvent()
    data class UpdateRadius(val radius: Float) : TaskCreationEvent()
    data class UpdateTriggerOnOption(val option: String) : TaskCreationEvent()
    data class UpdateTimeValue(val time: String) : TaskCreationEvent()
    data class UpdateWifiState(val state: String) : TaskCreationEvent()
    data class UpdateBluetoothAddress(val address: String) : TaskCreationEvent()
    data class UpdateNotificationTitle(val title: String) : TaskCreationEvent()
    data class UpdateNotificationMessage(val message: String) : TaskCreationEvent()
    data class UpdateNotificationPriority(val priority: String) : TaskCreationEvent()
    data class UpdateToggleSetting(val setting: String) : TaskCreationEvent()
    data class UpdateScriptText(val script: String) : TaskCreationEvent()
    data class UpdateSoundMode(val mode: String) : TaskCreationEvent()
    data class UpdateSelectedApps(val apps: List<String>) : TaskCreationEvent()
    object SaveTask : TaskCreationEvent()
    object DismissError : TaskCreationEvent()
    object DismissSuccess : TaskCreationEvent()
}

enum class TriggerType {
    LOCATION, TIME, WIFI, BLUETOOTH
}

enum class ActionType {
    NOTIFICATION, TOGGLE_SETTINGS, SCRIPT, SOUND_MODE, BLOCK_APPS, UNBLOCK_APPS
}
