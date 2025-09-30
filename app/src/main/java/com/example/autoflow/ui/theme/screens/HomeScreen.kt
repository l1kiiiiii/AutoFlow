package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.util.Constants
import com.example.autoflow.viewmodel.WorkflowViewModel
import org.json.JSONObject
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: WorkflowViewModel = viewModel()
) {
    val workflows by viewModel.getWorkflows().observeAsState(emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }
    var workflowToDelete by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Tasks Section
        Text(
            text = "Active Tasks",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (workflows.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(workflows) { workflow ->
                    WorkflowItemCard(
                        workflow = workflow,
                        onEdit = {
                            // TODO: Navigate to edit screen
                        },
                        onDelete = { workflowId ->
                            workflowToDelete = workflowId
                            showDeleteDialog = true
                        },
                        onToggle = { workflowId, enabled ->
                            viewModel.toggleWorkflow(workflowId, enabled)
                        }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No tasks found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create your first automation task!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Quick Actions Section
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onNavigateToCreateTask,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New Task")
        }

        Button(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Profile")
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && workflowToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        workflowToDelete?.let { id ->
                            viewModel.removeWorkflow(id)
                        }
                        showDeleteDialog = false
                        workflowToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkflowItemCard(
    workflow: WorkflowEntity,
    onEdit: (WorkflowEntity) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onToggle: (Long, Boolean) -> Unit = { _, _ -> }
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(workflow) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with name and toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workflow.workflowName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = workflow.isEnabled,
                    onCheckedChange = { onToggle(workflow.id, it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trigger info
            val triggerInfo = getTriggerDisplayInfo(workflow)
            Text(
                text = "Trigger: ${triggerInfo.first}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (triggerInfo.second.isNotEmpty()) {
                Text(
                    text = triggerInfo.second,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action info
            val actionInfo = getActionDisplayInfo(workflow)
            Text(
                text = "Action: ${actionInfo.first}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (actionInfo.second.isNotEmpty()) {
                Text(
                    text = actionInfo.second,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onEdit(workflow) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { onDelete(workflow.id) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Add these functions to your HomeScreen.kt file
private fun getTriggerDisplayInfo(workflow: WorkflowEntity): Pair<String, String> {
    return try {
        val triggerDetails = workflow.getTriggerDetails() // Use Java getter method
        if (triggerDetails.isNotEmpty()) {
            // Parse the JSON to determine trigger type
            val triggerJson = org.json.JSONObject(triggerDetails)
            val triggerType = triggerJson.optString("type", "Unknown")

            when (triggerType) {
                Constants.TRIGGER_LOCATION -> {
                    val locationValue = triggerJson.optString("value", "")
                    val locationName = if (locationValue.contains("locationName")) {
                        try {
                            val locationJson = org.json.JSONObject(locationValue)
                            locationJson.optString("locationName", "Unknown Location")
                        } catch (e: Exception) {
                            "Unknown Location"
                        }
                    } else {
                        "Location trigger"
                    }
                    Pair("Location", locationName)
                }

                Constants.TRIGGER_TIME -> {
                    val timeValue = triggerJson.optString("value", "")
                    val displayTime = if (timeValue.isNotEmpty()) {
                        try {
                            val timestamp = timeValue.toLong()
                            java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(timestamp))
                        } catch (e: Exception) {
                            "Scheduled time"
                        }
                    } else {
                        "Scheduled time"
                    }
                    Pair("Time", displayTime)
                }

                Constants.TRIGGER_WIFI -> {
                    val wifiState = triggerJson.optString("value", "state")
                    Pair("WiFi", "WiFi $wifiState")
                }

                Constants.TRIGGER_BLE -> {
                    val deviceName = triggerJson.optString("value", "Device")
                    Pair("Bluetooth", deviceName)
                }

                else -> Pair("Unknown", "Custom trigger")
            }
        } else {
            Pair("None", "No trigger configured")
        }
    } catch (e: Exception) {
        Pair("Unknown", "Invalid trigger data")
    }
}

private fun getActionDisplayInfo(workflow: WorkflowEntity): Pair<String, String> {
    return try {
        val actionDetails = workflow.getActionDetails() // Use Java getter method
        if (actionDetails.isNotEmpty()) {
            // Parse the JSON to determine action type
            val actionJson = org.json.JSONObject(actionDetails)
            val actionType = actionJson.optString("type", "Unknown")

            when (actionType) {
                Constants.ACTION_SEND_NOTIFICATION -> {
                    val title = actionJson.optString(Constants.JSON_KEY_NOTIFICATION_TITLE, "Notification")
                    Pair("Notification", title)
                }

                Constants.ACTION_TOGGLE_WIFI -> {
                    val setting = actionJson.optString("value", "WiFi")
                    Pair("Toggle Settings", "Toggle $setting")
                }

                "RUN_SCRIPT" -> {
                    Pair("Run Script", "Execute JavaScript")
                }

                else -> Pair("Unknown", "Custom action")
            }
        } else {
            Pair("None", "No action configured")
        }
    } catch (e: Exception) {
        Pair("Unknown", "Invalid action data")
    }
}



@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AutoFlowTheme {
        HomeScreen(
            onNavigateToCreateTask = { println("Preview: Navigate to Create Task") },
            onNavigateToProfile = { println("Preview: Navigate to Profile") }
        )
    }
}
