package com.example.autoflow.domain.usecase

import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.WorkflowRepository
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * UseCase for saving workflows to the database
 * Encapsulates business logic for workflow creation
 * Follows Single Responsibility Principle
 */
class SaveWorkflowUseCase(private val repository: WorkflowRepository) {
    
    /**
     * Save a workflow with triggers and actions
     * @return Workflow ID on success, null on failure
     */
    suspend fun execute(
        workflowName: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        triggerLogic: String = "AND"
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Validate inputs
            if (workflowName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Workflow name cannot be empty"))
            }
            
            if (triggers.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("At least one trigger is required"))
            }
            
            if (actions.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("At least one action is required"))
            }
            
            // Build triggers JSON
            val triggersJsonArray = JSONArray()
            triggers.forEach { trigger ->
                val triggerJson = JSONObject().apply {
                    put("type", trigger.type)
                    put("value", trigger.value)
                }
                triggersJsonArray.put(triggerJson)
            }
            
            // Build actions JSON
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
            
            // Create workflow entity
            val workflow = WorkflowEntity(
                workflowName = workflowName.trim(),
                triggerDetails = triggersJsonArray.toString(),
                actionDetails = actionsJsonArray.toString(),
                triggerLogic = triggerLogic,
                isEnabled = true
            )
            
            // Save to database
            val workflowId = repository.insertSuspend(workflow)
            Result.success(workflowId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
