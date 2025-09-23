package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
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
    val scrollState = rememberScrollState()
    // Adjust to handle List<WorkflowEntity>? from LiveData
    val workflows: List<WorkflowEntity>? by viewModel.getWorkflows().observeAsState(null)

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Active Profiles",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (workflows != null && workflows!!.isNotEmpty()) {
            workflows!!.forEach { workflowEntity ->
                val trigger = workflowEntity.toTrigger()
                val action = workflowEntity.toAction()

                if (trigger != null && action != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        WorkflowItem(
                            trigger = trigger,
                            action = action,
                            onClick = { /* Handle workflow click (e.g., edit or details for workflowEntity.id) */ }
                        )
                    }
                } else {
                    // Optional: Display an error or skip this item if parsing failed
                    Text(
                        text = "Error loading workflow: ${workflowEntity.workflowName}",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            Text(
                text = "No active profiles found.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recent Executions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
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
        Spacer(modifier = Modifier.height(200.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AutoFlowTheme {
        HomeScreen(
            onNavigateToCreateTask = { println("Preview: Navigate to Create Task") },
            onNavigateToProfile = { println("Preview: Navigate to Profile") }
            // Preview won't have a real ViewModel, so workflows will be null/empty
        )
    }
}