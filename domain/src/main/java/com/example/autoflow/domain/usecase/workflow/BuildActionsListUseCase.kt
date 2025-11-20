package com.example.autoflow.domain.usecase.workflow

import com.example.autoflow.domain.model.Action
import com.example.autoflow.ui.state.TaskCreationUiState

/**
 * Use Case for building actions list from UI state
 * Encapsulates the business logic of converting UI state to domain actions
 */
class BuildActionsListUseCase {
    
    fun execute(state: TaskCreationUiState): Result<List<Action>> {
        try {
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
            
            return Result.success(actions)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
