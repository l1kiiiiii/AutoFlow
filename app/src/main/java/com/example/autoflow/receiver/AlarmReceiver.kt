package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toActions
import com.example.autoflow.data.toTriggers
import com.example.autoflow.model.Action
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import com.example.autoflow.util.TriggerEvaluator
import com.example.autoflow.util.TriggerParser
import com.example.autoflow.worker.TimeTriggerWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ✅ COMPLETELY FIXED AlarmReceiver - Handles scheduled workflow triggers
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        // ✅ FIXED: Define the missing constant here
        const val KEY_WORKFLOW_ID = "workflow_id"
        const val EXTRA_WORKFLOW_ID = "workflow_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 Alarm received: ${intent.action}")

        // ✅ FIXED: Get workflow ID from intent using defined constant
        val workflowId = intent.getLongExtra(EXTRA_WORKFLOW_ID, -1L)

        if (workflowId == -1L) {
            Log.e(TAG, "❌ Invalid workflow ID in alarm intent")
            return
        }

        Log.d(TAG, "⏰ Processing scheduled trigger for workflow: $workflowId")

        // Process the workflow in a coroutine scope
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processScheduledWorkflow(context, workflowId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing scheduled workflow", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * ✅ Process scheduled workflow with proper trigger evaluation
     */
    private suspend fun processScheduledWorkflow(context: Context, workflowId: Long) {
        try {
            val database = AppDatabase.getDatabase(context)
            val workflow = database.workflowDao().getByIdSync(workflowId)

            if (workflow == null) {
                Log.w(TAG, "⚠️ Workflow not found: $workflowId")
                return
            }

            if (!workflow.isEnabled) {
                Log.w(TAG, "⚠️ Workflow disabled: ${workflow.workflowName}")
                return
            }

            Log.d(TAG, "📋 Processing workflow: ${workflow.workflowName}")

            // Get triggers and evaluate conditions
            val triggers = workflow.toTriggers()
            val triggerEvaluator = TriggerEvaluator.getInstance()

            // Build current system states
            val currentStates = triggerEvaluator.buildCurrentStates(
                context,
                triggers,
                CoroutineScope(Dispatchers.IO)
            )

            // Evaluate triggers with workflow logic
            val shouldExecute = triggerEvaluator.evaluateTriggers(
                triggers,
                workflow.triggerLogic,
                currentStates,
                CoroutineScope(Dispatchers.IO)
            )

            if (shouldExecute) {
                Log.d(TAG, "✅ Trigger conditions met - executing workflow")
                executeWorkflowActions(context, workflow)
            } else {
                Log.d(TAG, "⏭️ Trigger conditions not met - skipping execution")
            }

            // Schedule next occurrence if recurring
            scheduleNextOccurrence(context, workflow)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing scheduled workflow: $workflowId", e)
        }
    }

    /**
     * ✅ Execute workflow actions with proper coroutine handling
     */
    private suspend fun executeWorkflowActions(context: Context, workflow: WorkflowEntity) {
        try {
            val actionExecutor = ActionExecutor.getInstance()

            // ✅ FIXED: Use executeWorkflow method instead of individual actions
            val success = actionExecutor.executeWorkflow(context, workflow)

            if (success) {
                Log.d(TAG, "✅ Workflow executed successfully: ${workflow.workflowName}")
            } else {
                Log.e(TAG, "❌ Workflow execution failed: ${workflow.workflowName}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing workflow actions", e)
        }
    }

    /**
     * ✅ Schedule next occurrence for recurring workflows
     */
    private fun scheduleNextOccurrence(context: Context, workflow: WorkflowEntity) {
        try {
            val triggers = workflow.toTriggers()
            val timeTriggersFound = triggers.any { it.type == Constants.TRIGGER_TIME }

            if (timeTriggersFound) {
                // Schedule using WorkManager for next day (24 hours later)
                val workData = Data.Builder()
                    .putLong(KEY_WORKFLOW_ID, workflow.id) // ✅ FIXED: Use defined constant
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<TimeTriggerWorker>()
                    .setInputData(workData)
                    .setInitialDelay(24, TimeUnit.HOURS) // Next day
                    .addTag("time_trigger_${workflow.id}")
                    .build()

                WorkManager.getInstance(context)
                    .enqueue(workRequest)

                Log.d(TAG, "⏰ Scheduled next occurrence for workflow: ${workflow.workflowName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling next occurrence", e)
        }
    }
}
