package com.example.autoflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.autoflow.model.Trigger
import com.example.autoflow.model.Action
import com.example.autoflow.util.Constants
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkflowItem(
    trigger: Trigger,
    action: Action,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Workflow Title (Trigger Type + Summary)
            Text(
                text = getTriggerDisplayText(trigger),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Action Summary
            Text(
                text = getActionDisplayText(action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper function to format trigger display text
private fun getTriggerDisplayText(trigger: Trigger): String {
    return when (trigger.type) {
        Constants.TRIGGER_TIME -> {
            try {
                val timestamp = trigger.value?.toLongOrNull()
                if (timestamp != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    "Time: ${dateFormat.format(Date(timestamp))}"
                } else {
                    "Time: Invalid time value"
                }
            } catch (e: Exception) { // Catch generic exception for safety
                "Time: Error parsing time"
            }
        }
        Constants.TRIGGER_BLE -> {
            if (!trigger.value.isNullOrEmpty()) {
                "BLE: ${trigger.value}"
            } else {
                "BLE: Device not specified"
            }
        }
        Constants.TRIGGER_LOCATION -> {
            try {
                if (trigger.value.isNullOrEmpty()) return "Location: Details not set"
                val params = JSONObject(trigger.value)
                val locationName = params.optString("locationName", "Unnamed Location")
                val radius = params.optDouble("radius", 0.0)
                val onEntry = params.optBoolean("triggerOnEntry", false)
                val onExit = params.optBoolean("triggerOnExit", false)

                var details = " (${radius.toInt()}m"
                details += if (onEntry && onExit) ", Entry/Exit)"
                else if (onEntry) ", On Entry)"
                else if (onExit) ", On Exit)"
                else ")"
                "Location: $locationName$details"
            } catch (e: JSONException) {
                "Location: Error parsing details"
            }
        }
        else -> "Trigger: Unknown type"
    }
}

// Helper function to format action display text
private fun getActionDisplayText(action: Action): String {
    return when (action.type) {
        Constants.ACTION_TOGGLE_WIFI -> {
            val state = action.value?.takeIf { it.isNotEmpty() } ?: "State N/A"
            "Action: Toggle Wi-Fi ($state)"
        }
        Constants.ACTION_SEND_NOTIFICATION -> {
            val title = action.title?.takeIf { it.isNotEmpty() } ?: "Notification"
            "Action: Send '$title'"
        }
        else -> "Action: Unknown type"
    }
}