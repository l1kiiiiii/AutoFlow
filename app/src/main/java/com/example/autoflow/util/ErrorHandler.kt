package com.example.autoflow.util

import android.content.Context
import android.util.Log
import android.widget.Toast

object ErrorHandler {

    fun handleError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, "❌ $message", throwable)

        // Show user-friendly message
        Toast.makeText(context, "Error: $message", Toast.LENGTH_SHORT).show()
    }

    fun handleWorkflowError(context: Context, workflowName: String, error: String) {
        Log.e("Workflow", "❌ Error in '$workflowName': $error")
        Toast.makeText(context, "Workflow '$workflowName' failed: $error", Toast.LENGTH_LONG).show()
    }
}
