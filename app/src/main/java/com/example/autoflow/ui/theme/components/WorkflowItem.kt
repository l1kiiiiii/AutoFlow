package com.example.autoflow.ui.components // Original package

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.autoflow.data.WorkflowEntity // Import WorkflowEntity
import com.example.autoflow.model.Action // Keep for helper functions
import com.example.autoflow.model.Trigger // Keep for helper functions
import com.example.autoflow.util.Constants // Keep for helper functions
import org.json.JSONException // Keep for helper functions
import org.json.JSONObject // Keep for helper functions
import java.text.SimpleDateFormat // Keep for helper functions
import java.util.* // Keep for helper functions

@Composable
fun WorkflowItem(
    workflow: WorkflowEntity,
    onEdit: (WorkflowEntity) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onToggle: (Long, Boolean) -> Unit = { _, _ -> } // Added default for consistency
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Adjusted padding from your example
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workflow.workflowName ?: "Unnamed Workflow", // Handle possible null name
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Status: ${if (workflow.isEnabled) "Active" else "Inactive"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (workflow.isEnabled) Color.Green else Color.Gray // Consider theme colors
                )
            }

            Row {
                // Toggle button
                IconButton(onClick = { onToggle(workflow.id, !workflow.isEnabled) }) {
                    Icon(
                        imageVector = if (workflow.isEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                        contentDescription = if (workflow.isEnabled) "Disable" else "Enable",
                        tint = if (workflow.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant // Adjusted colors
                    )
                }

                // Edit button
                IconButton(onClick = { onEdit(workflow) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete button
                IconButton(onClick = { onDelete(workflow.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error // Use theme error color
                    )
                }
            }
        }
    }
}

// Helper function to format trigger display text (kept for potential future use)
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
            } catch (e: Exception) {
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
        // Add case for TRIGGER_WIFI_STATE if you want display text for it
        Constants.TRIGGER_WIFI -> {
            try {
                if (trigger.value.isNullOrEmpty()) return "WiFi: State not specified"
                 val params = JSONObject(trigger.value)
                 val targetState = params.optString(Constants.JSON_KEY_WIFI_TARGET_STATE, "Unknown")
                "WiFi: Target state $targetState"
            } catch (e: JSONException) {
                "WiFi: Error parsing details"
            }
        }
        else -> "Trigger: Unknown type"
    }
}

// Helper function to format action display text (kept for potential future use)
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
        Constants.ACTION_TOGGLE_SETTINGS -> { // Added
            val settingToToggle = action.value?.takeIf {it.isNotEmpty()} ?: "Setting N/A"
            "Action: Toggle Setting ($settingToToggle)"
        }
        Constants.ACTION_RUN_SCRIPT -> { // Added
             "Action: Run Script"
        }
        else -> "Action: Unknown type"
    }
}