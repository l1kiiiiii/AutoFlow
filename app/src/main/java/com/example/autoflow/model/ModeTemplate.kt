package com.example.autoflow.model

data class ModeTemplate(
    val name: String,
    val icon: String,
    val color: String,
    val description: String,
    val defaultTriggers: List<TriggerTemplate>,
    val defaultActions: List<ActionTemplate>
)

data class TriggerTemplate(
    val type: String,
    val config: Map<String, Any>
)

data class ActionTemplate(
    val type: String,
    val config: Map<String, Any>
)
