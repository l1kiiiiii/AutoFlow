// Updated WorkflowViewModel.kt
// Fix: Added autoReplyManager.start() and stop() for Meeting Mode
package com.example.autoflow.viewmodel

class WorkflowViewModel {
    fun updateWorkflowEnabled(workflow: Workflow) {
        if (workflow.isManualMode() && workflow.name == "Meeting Mode") {
            println("🤝 Manual workflow detected - saving current state first")
            saveCurrentPhoneState()
            executeWorkflowActions(workflow)
            // ADD THIS LINE TO START AUTO-REPLY
            autoReplyManager.start(workflow.name, "I'm currently in a meeting and will get back to you soon.")
        }

        if (workflow.isManualMode() && workflow.name == "Meeting Mode" && workflow.isDisabled()) {
            println("🤝 Manual workflow disabled - restoring previous state")
            restorePreviousPhoneState()
            // ADD THIS LINE TO STOP AUTO-REPLY
            autoReplyManager.stop()
        }
    }

    private fun saveCurrentPhoneState() {}
    private fun executeWorkflowActions(workflow: Workflow) {}
    private fun restorePreviousPhoneState() {}
    private val autoReplyManager = AutoReplyManager()
}

class Workflow(val name: String, private val isManual: Boolean, private var enabled: Boolean){
    fun isManualMode() = isManual
    fun isDisabled() = !enabled
}

class AutoReplyManager {
    fun start(name: String, message: String) { println("Auto-reply started: $name - $message") }
    fun stop() { println("Auto-reply stopped") }
}