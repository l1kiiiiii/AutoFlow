package com.example.autoflow.ui.theme.screens

import android.widget.Toast // For showing messages (placeholder)
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.rememberScrollState // Not needed with LazyColumn for workflows
// import androidx.compose.foundation.verticalScroll // Not needed with LazyColumn for workflows
import androidx.compose.foundation.lazy.LazyColumn // Added
import androidx.compose.foundation.lazy.items // Added
import androidx.compose.material3.AlertDialog // Added
import androidx.compose.material3.Button
// import androidx.compose.material3.Card // WorkflowItem handles its own Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton // Added
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf // Added
import androidx.compose.runtime.remember // Added
import androidx.compose.runtime.setValue // Added
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // For Toast (placeholder)
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.data.WorkflowEntity
// import com.example.autoflow.model.Action // Not directly used here anymore
// import com.example.autoflow.model.Trigger // Not directly used here anymore
import com.example.autoflow.ui.components.WorkflowItem
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.viewmodel.WorkflowViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: WorkflowViewModel = viewModel()
) {
    // val scrollState = rememberScrollState() // Replaced by LazyColumn for workflows
    val workflows by viewModel.getWorkflows().observeAsState(emptyList()) // Use emptyList() as initial
    var showDeleteDialog by remember { mutableStateOf(false) }
    var workflowToDelete by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current // For Toast messages

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
        // .verticalScroll(scrollState) // Removed, main column might not need to scroll if LazyColumn handles workflow list
    ) {
        Text(
            text = "Active Tasks", // Changed from "Active Profiles" to match your example
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (workflows.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // Consider adding a fixed height or weight if this LazyColumn is inside another scrollable Column
                // and you want independent scrolling for the list, or ensure the parent Column doesn't scroll.
                // For now, assuming this LazyColumn is the primary scrollable content for tasks.
                modifier = Modifier.weight(1f) // Allow LazyColumn to take available space
            ) {
                items(workflows, key = { it.id }) { workflow -> // Added key for better performance
                    WorkflowItem(
                        workflow = workflow,
                        onEdit = {
                            // Navigate to edit screen - Placeholder
                            // Example: onNavigateToEditTask(workflow.id)
                            Toast.makeText(context, "Edit: ${workflow.workflowName}", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { workflowId ->
                            workflowToDelete = workflowId
                            showDeleteDialog = true
                        },
                        onToggle = { workflowId, enabled ->
                            viewModel.toggleWorkflow(workflowId, enabled, object : WorkflowViewModel.WorkflowOperationCallback {
                                override fun onSuccess(message: String) {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                                override fun onError(error: String) {
                                    Toast.makeText(context, "Error toggling: $error", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    )
                }
            }
        } else {
            Text(
                text = "No tasks found. Create your first automation task!", // Updated text
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray, // Consider MaterialTheme.colorScheme.onSurfaceVariant
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }

        // Delete confirmation dialog
        if (showDeleteDialog && workflowToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    workflowToDelete = null
                },
                title = { Text("Delete Task") },
                text = { Text("Are you sure you want to delete this task? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            workflowToDelete?.let { id ->
                                viewModel.removeWorkflow(id, object : WorkflowViewModel.WorkflowOperationCallback {
                                    override fun onSuccess(message: String) {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onError(error: String) {
                                        Toast.makeText(context, "Error deleting: $error", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            }
                            showDeleteDialog = false
                            workflowToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error) // Use theme color
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        workflowToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // The rest of your HomeScreen content (Recent Executions, Quick Actions)
        // If the LazyColumn above has weight(1f), these might need to be outside
        // or the parent Column needs to handle overall scrolling if LazyColumn's height is not fixed.
        // For simplicity, I'm keeping them as they were, but review layout if scrolling issues occur.

        Spacer(modifier = Modifier.height(16.dp)) // Add some space if LazyColumn is not filling everything

        Text(
            text = "Recent Executions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        // ... (rest of your existing UI for recent executions, quick actions, etc.) ...
        Text(
            text = "No recent executions recorded yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

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

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Profile")
        }
        // Spacer(modifier = Modifier.height(200.dp)) // May not be needed or adjust as per layout
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AutoFlowTheme {
        HomeScreen(
            onNavigateToCreateTask = { println("Preview: Navigate to Create Task") },
            onNavigateToProfile = { println("Preview: Navigate to Profile") }
            // Preview won't have a real ViewModel by default for callbacks to work
        )
    }
}
