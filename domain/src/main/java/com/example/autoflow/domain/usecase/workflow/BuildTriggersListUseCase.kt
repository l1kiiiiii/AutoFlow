package com.example.autoflow.domain.usecase.workflow

import com.example.autoflow.domain.model.Trigger
import com.example.autoflow.domain.model.TriggerHelpers
import com.example.autoflow.ui.state.TaskCreationUiState

/**
 * Use Case for building triggers list from UI state
 * Encapsulates the business logic of converting UI state to domain triggers
 */
class BuildTriggersListUseCase {
    
    fun execute(state: TaskCreationUiState): Result<List<Trigger>> {
        try {
            val triggers = mutableListOf<Trigger>()
            
            // Location trigger
            if (state.locationTriggerExpanded && state.locationDetailsInput.isNotBlank()) {
                val parts = state.locationDetailsInput.split(",").map { it.trim() }
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        triggers.add(
                            TriggerHelpers.createLocationTrigger(
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
                    TriggerHelpers.createTimeTrigger(
                        state.timeValue,
                        emptyList()
                    )
                )
            }
            
            // WiFi trigger
            if (state.wifiTriggerExpanded) {
                triggers.add(
                    TriggerHelpers.createWifiTrigger(
                        null,
                        state.wifiState
                    )
                )
            }
            
            // Bluetooth trigger
            if (state.bluetoothDeviceTriggerExpanded && state.bluetoothDeviceAddress.isNotBlank()) {
                triggers.add(
                    TriggerHelpers.createBluetoothTrigger(
                        state.bluetoothDeviceAddress,
                        null
                    )
                )
            }
            
            return Result.success(triggers)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
