// Updated PredefinedModes.kt
// Fix: Added SHOW_NOTIFICATION action in Meeting Mode template
package com.example.autoflow.util

class PredefinedModes {
    fun getModes(): List<ModeTemplate> = listOf(
        ModeTemplate(
            name = "Meeting Mode",
            description = "Silence your phone and auto-reply during meetings.",
            icon = R.drawable.ic_action,
            triggers = listOf(Trigger(type = "MANUAL", value = "manual")),
            actions = listOf(
                Action(type = "SET_SOUND_MODE", value = "dnd"),
                // ADD THIS ACTION
                Action(
                    type = "SHOW_NOTIFICATION",
                    title = "Meeting Mode Activated",
                    message = "Your phone is silenced and auto-reply is on."
                )
            )
        )
    )
}

class ModeTemplate(val name: String, val description: String, val icon: Int, val triggers: List<Trigger>, val actions: List<Action>)
class Trigger(val type: String, val value: String)
class Action(val type: String, val value: String? = null, val title: String? = null, val message: String? = null)
object R { object drawable { const val ic_action = 0 } }