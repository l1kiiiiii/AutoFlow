package com.example.autoflow.util

import com.example.autoflow.model.ActionTemplate
import com.example.autoflow.model.ModeTemplate
import com.example.autoflow.model.TriggerTemplate

object PredefinedModes {

    val MEETING_MODE = ModeTemplate(
        name = "Meeting Mode",
        icon = "🤝",
        color = "#2196F3",
        description = "Instantly silence your phone. Tap to deactivate",
        defaultTriggers = listOf(
            TriggerTemplate("MANUAL", mapOf("type" to "quick_action"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "DND"))
        )
    )

    val OFFICE_MODE = ModeTemplate(
        name = "Office Mode",
        icon = "💼",
        color = "#4CAF50",
        description = "Optimize for work",
        defaultTriggers = listOf(
            TriggerTemplate("LOCATION", mapOf("type" to "office"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "Vibrate")),
            ActionTemplate("TOGGLE_WIFI", mapOf("value" to "ON"))
        )
    )

    val CLASS_MODE = ModeTemplate(
        name = "Class Mode",
        icon = "📚",
        color = "#FF9800",
        description = "Silent during classes",
        defaultTriggers = listOf(
            TriggerTemplate("LOCATION", mapOf("type" to "school"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "DND"))
        )
    )

    val HOME_MODE = ModeTemplate(
        name = "Home Mode",
        icon = "🏠",
        color = "#9C27B0",
        description = "Relax at home",
        defaultTriggers = listOf(
            TriggerTemplate("LOCATION", mapOf("type" to "home"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "Normal")),
            ActionTemplate("TOGGLE_WIFI", mapOf("value" to "ON"))
        )
    )

    val FUN_MODE = ModeTemplate(
        name = "Fun Mode",
        icon = "🎉",
        color = "#E91E63",
        description = "Weekend leisure time",
        defaultTriggers = listOf(
            TriggerTemplate("TIME", mapOf("time" to "18:00"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "Normal"))
        )
    )

    val SLEEP_MODE = ModeTemplate(
        name = "Sleep Mode",
        icon = "😴",
        color = "#607D8B",
        description = "Quiet mode for bedtime",
        defaultTriggers = listOf(
            TriggerTemplate("TIME", mapOf("time" to "22:00"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "DND")),
            ActionTemplate("TOGGLE_WIFI", mapOf("value" to "OFF"))
        )
    )

    val DRIVING_MODE = ModeTemplate(
        name = "Driving Mode",
        icon = "🚗",
        color = "#FF5722",
        description = "Safe while driving",
        defaultTriggers = listOf(
            TriggerTemplate("BLUETOOTH", mapOf("deviceType" to "car"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "Vibrate"))
        )
    )

    val GYM_MODE = ModeTemplate(
        name = "Gym Mode",
        icon = "💪",
        color = "#F44336",
        description = "Workout time",
        defaultTriggers = listOf(
            TriggerTemplate("LOCATION", mapOf("type" to "gym"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "Vibrate"))
        )
    )
    fun isManualMode(mode: ModeTemplate): Boolean {
        return mode.defaultTriggers.any { it.type == "MANUAL" }
    }
    fun getAllModes(): List<ModeTemplate> = listOf(
        MEETING_MODE,
        OFFICE_MODE,
        CLASS_MODE,
        HOME_MODE,
        FUN_MODE,
        SLEEP_MODE,
        DRIVING_MODE,
        GYM_MODE
    )
}
