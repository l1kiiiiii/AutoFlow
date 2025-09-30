package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.ui.components.WorkflowItem
import com.example.autoflow.data.WorkflowViewModel
import com.example.autoflow.util.Constants
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToEditTask: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: WorkflowViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val workflows: List<WorkflowEntity>? by viewModel.getWorkflows().observeAsState(null)

    // State for showing task details dialog
    var selectedWorkflow by remember { mutableStateOf<WorkflowEntity?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Active Tasks",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (workflows != null && workflows!!.isNotEmpty()) {
            workflows!!.forEach { workflowEntity ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Show details dialog when card is clicked
                            selectedWorkflow = workflowEntity
                            showDetailsDialog = true
                        },
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
                                text = workflowEntity.workflowName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Status: ${if (workflowEntity.isEnabled) "Active" else "Inactive"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (workflowEntity.isEnabled) Color.Green else Color.Gray
                            )
                        }

                        Row {
                            // Toggle button
                            IconButton(
                                onClick = {
                                    viewModel.updateWorkflowEnabled(
                                        workflowEntity.id,
                                        !workflowEntity.isEnabled
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (workflowEntity.isEnabled)
                                        Icons.Default.ToggleOn
                                    else
                                        Icons.Default.ToggleOff,
                                    contentDescription = if (workflowEntity.isEnabled) "Disable" else "Enable",
                                    tint = if (workflowEntity.isEnabled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Edit button
                            IconButton(onClick = { onNavigateToEditTask(workflowEntity.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Delete button
                            IconButton(onClick = { viewModel.deleteWorkflow(workflowEntity.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "No active tasks found.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToCreateTask,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New Task")
        }
    }

    // Task Details Dialog
    if (showDetailsDialog && selectedWorkflow != null) {
        TaskDetailsDialog(
            workflow = selectedWorkflow!!,
            onDismiss = { showDetailsDialog = false },
            onEdit = {
                showDetailsDialog = false
                onNavigateToEditTask(selectedWorkflow!!.id)
            }
        )
    }
}

@Composable
fun TaskDetailsDialog(
    workflow: WorkflowEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val triggerInfo = getTriggerDisplayInfo(workflow)
    val actionInfo = getActionDisplayInfo(workflow)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Task Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Task Name
                Text(
                    text = workflow.workflowName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (workflow.isEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (workflow.isEnabled) Color.Green else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (workflow.isEnabled) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (workflow.isEnabled) Color.Green else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trigger Section
                Text(
                    text = "Trigger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = triggerInfo.first,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (triggerInfo.second.isNotEmpty()) {
                            Text(
                                text = triggerInfo.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Section
                Text(
                    text = "Action",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = actionInfo.first,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (actionInfo.second.isNotEmpty()) {
                            Text(
                                text = actionInfo.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onEdit) {
                        Text("Edit Task")
                    }
                }
            }
        }
    }
}

// Helper functions (keep your existing ones)
private fun getTriggerDisplayInfo(workflow: WorkflowEntity): Pair<String, String> {
    // Your existing getTriggerDisplayInfo code
    return try {
        val triggerDetails = workflow.triggerDetails
        if (triggerDetails.isNotEmpty()) {
            val triggerJson = JSONObject(triggerDetails)
            val triggerType = triggerJson.optString("type", "Unknown")

            when (triggerType) {
                Constants.TRIGGER_TIME -> {
                    val timeValue = triggerJson.optString("value", "")
                    val displayTime = if (timeValue.isNotEmpty()) {
                        try {
                            val timestamp = timeValue.toLong()
                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(timestamp))
                        } catch (e: Exception) {
                            "Scheduled time"
                        }
                    } else {
                        "Scheduled time"
                    }
                    Pair("Time Trigger", displayTime)
                }
                Constants.TRIGGER_LOCATION -> Pair("Location Trigger", "Location-based")
                Constants.TRIGGER_WIFI -> Pair("WiFi Trigger", "WiFi state change")
                Constants.TRIGGER_BLE -> Pair("Bluetooth Trigger", "Bluetooth device")
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
    // Your existing getActionDisplayInfo code
    return try {
        val actionDetails = workflow.actionDetails
        if (actionDetails.isNotEmpty()) {
            val actionJson = JSONObject(actionDetails)
            val actionType = actionJson.optString("type", "Unknown")

            when (actionType) {
                Constants.ACTION_SEND_NOTIFICATION -> {
                    val title = actionJson.optString("title", "Notification")
                    Pair("Send Notification", title)
                }
                Constants.ACTION_TOGGLE_WIFI -> Pair("Toggle Settings", "WiFi control")
                "RUN_SCRIPT" -> Pair("Run Script", "Execute JavaScript")
                else -> Pair("Unknown", "Custom action")
            }
        } else {
            Pair("None", "No action configured")
        }
    } catch (e: Exception) {
        Pair("Unknown", "Invalid action data")
    }
}
