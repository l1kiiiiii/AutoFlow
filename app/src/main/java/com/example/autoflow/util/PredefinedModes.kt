package com.example.autoflow.util

import com.example.autoflow.model.ActionTemplate
import com.example.autoflow.model.ModeTemplate
import com.example.autoflow.model.TriggerTemplate

object PredefinedModes {

    val MEETING_MODE = ModeTemplate(
        name = "Meeting Mode",
        icon = "🤝",
        color = "#2196F3",
        description = "Silence your phone and auto-reply during meetings.",
        defaultTriggers = listOf(
            TriggerTemplate("MANUAL", mapOf("type" to "quick_action"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "dnd")),
            ActionTemplate("AUTO_REPLY", mapOf(
                "message" to "I'm currently in a meeting and will get back to you soon."
            )),
            ActionTemplate("SHOW_NOTIFICATION", mapOf(
                "title" to "Meeting Mode Activated",
                "message" to "Your phone is silenced and auto-reply is on."
            ))
        )
    )
    val OFFICE_MODE = ModeTemplate(
        name = "Office Mode",
        icon = "💼",
        color = "#4CAF50",
        description = "Vibrate for work",
        defaultTriggers = listOf(
            TriggerTemplate("LOCATION", mapOf("type" to "office"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "vibrate")),
            ActionTemplate("TOGGLE_WIFI", mapOf("value" to "ON"))
        )
    )

    val CLASS_MODE = ModeTemplate(
        name = "Class Mode",
        icon = "📚",
        color = "#FF9800",
        description = "Complete DND during classes",
        defaultTriggers = listOf(
            TriggerTemplate("LOCATION", mapOf("type" to "school"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "silent"))
        )
    )

    val HOME_MODE = ModeTemplate(
        name = "Home Mode",
        icon = "🏠",
        color = "#9C27B0",
        description = "Normal sound at home",
        defaultTriggers = listOf(
            TriggerTemplate("LOCATION", mapOf("type" to "home"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "normal")),
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
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "normal"))
        )
    )

    val SLEEP_MODE = ModeTemplate(
        name = "Sleep Mode",
        icon = "😴",
        color = "#37474F",
        description = "Complete SILENT for bedtime",
        defaultTriggers = listOf(
            TriggerTemplate("TIME", mapOf("time" to "22:00"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "silent")),  // ✅ DND for sleep
            ActionTemplate("TOGGLE_WIFI", mapOf("value" to "OFF"))
        )
    )

    val DRIVING_MODE = ModeTemplate(
        name = "Driving Mode",
        icon = "🚗",
        color = "#FF5722",
        description = "Vibrate while driving",
        defaultTriggers = listOf(
            TriggerTemplate("BLUETOOTH", mapOf("deviceType" to "car"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "vibrate"))
        )
    )

    val GYM_MODE = ModeTemplate(
        name = "Gym Mode",
        icon = "💪",
        color = "#F44336",
        description = "Vibrate during workout",
        defaultTriggers = listOf(
            TriggerTemplate("LOCATION", mapOf("type" to "gym"))
        ),
        defaultActions = listOf(
            ActionTemplate("SET_SOUND_MODE", mapOf("value" to "vibrate"))
        )
    )

    fun isManualMode(mode: ModeTemplate): Boolean {
        return mode.defaultTriggers.any { it.type == "MANUAL" }
    }

    fun getAllModes(): List<ModeTemplate> = listOf(
        MEETING_MODE,    // 🤝 Manual DND (the only manual mode)
        OFFICE_MODE,     // 💼 Vibrate + WiFi
        CLASS_MODE,      // 📚 Automatic DND
        HOME_MODE,       // 🏠 Normal sound
        FUN_MODE,        // 🎉 Normal sound
        SLEEP_MODE,      // 😴 Automatic DND
        DRIVING_MODE,    // 🚗 Vibrate
        GYM_MODE         // 💪 Vibrate
    )
}

