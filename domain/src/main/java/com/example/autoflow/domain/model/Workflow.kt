package com.example.autoflow.domain.model

/**
 * Domain model representing a workflow
 * This is independent of the data layer implementation
 */
data class Workflow(
    val id: Long = 0,
    val workflowName: String,
    val triggers: List<Trigger>,
    val actions: List<Action>,
    val triggerLogic: String = "AND",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modeId: Long? = null,
    val isModeWorkflow: Boolean = false
)
