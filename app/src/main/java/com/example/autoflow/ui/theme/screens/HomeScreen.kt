package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.util.Constants
import com.example.autoflow.viewmodel.WorkflowViewModel
import org.json.JSONObject

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

    val activeWorkflows = workflows?.filter { it.isEnabled } ?: emptyList()
    val totalWorkflows = workflows?.size ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle with count
        Text(
            text = "Your Flows (${activeWorkflows.size}/$totalWorkflows active)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Workflow Cards
        if (workflows != null && workflows!!.isNotEmpty()) {
            workflows!!.forEach { workflow ->
                WorkflowCard(
                    workflow = workflow,
                    onToggle = {
                        viewModel.updateWorkflowEnabled(workflow.id, !workflow.isEnabled)
                    },
                    onEdit = { onNavigateToEditTask(workflow.id) }
                )
            }
        } else {
            // Empty state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No workflows yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Space for bottom nav
    }

    // Floating Action Button
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onNavigateToCreateTask,
            modifier = Modifier.padding(end = 20.dp, bottom = 90.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Flow"
            )
        }
    }
}

@Composable
fun WorkflowCard(
    workflow: WorkflowEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val triggerInfo = getWorkflowIcon(workflow)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon and Content
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(triggerInfo.second),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = triggerInfo.first,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Text Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = workflow.workflowName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = getTriggerDescription(workflow),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            // Toggle Switch
            Switch(
                checked = workflow.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

private fun getWorkflowIcon(workflow: WorkflowEntity): Pair<ImageVector, Color> {
    return try {
        val triggerDetails = workflow.triggerDetails
        if (triggerDetails.isNotEmpty()) {
            val triggerJson = JSONObject(triggerDetails)
            val triggerType = triggerJson.optString("type", "Unknown")

            when (triggerType) {
                Constants.TRIGGER_LOCATION -> Pair(Icons.Default.LocationOn, Color(0xFF7E57C2))
                Constants.TRIGGER_TIME -> Pair(Icons.Default.WbSunny, Color(0xFFFFB74D))
                Constants.TRIGGER_WIFI -> Pair(Icons.Default.Home, Color(0xFF64B5F6))
                Constants.TRIGGER_BLE -> Pair(Icons.Default.DirectionsCar, Color(0xFF9575CD))
                else -> Pair(Icons.Default.Work, Color(0xFF5C6BC0))
            }
        } else {
            Pair(Icons.Default.Work, Color(0xFF5C6BC0))
        }
    } catch (e: Exception) {
        Pair(Icons.Default.Work, Color(0xFF5C6BC0))
    }
}

private fun getTriggerDescription(workflow: WorkflowEntity): String {
    return try {
        val triggerDetails = workflow.triggerDetails
        val actionDetails = workflow.actionDetails

        val triggerDesc = if (triggerDetails.isNotEmpty()) {
            val triggerJson = JSONObject(triggerDetails)
            val triggerType = triggerJson.optString("type", "Unknown")

            when (triggerType) {
                Constants.TRIGGER_LOCATION -> "At work, "
                Constants.TRIGGER_TIME -> "At 7 AM, "
                Constants.TRIGGER_WIFI -> "If I leave home, "
                Constants.TRIGGER_BLE -> "Bluetooth connects to car, "
                else -> "When triggered, "
            }
        } else {
            ""
        }

        val actionDesc = if (actionDetails.isNotEmpty()) {
            val actionJson = JSONObject(actionDetails)
            val actionType = actionJson.optString("type", "Unknown")

            when (actionType) {
                Constants.ACTION_SEND_NOTIFICATION -> "silence phone & open Slack"
                Constants.ACTION_TOGGLE_WIFI -> "turn off WiFi"
                "SET_DND" -> "enable DND & lower brightness"
                "LAUNCH_APP" -> "launch Maps"
                else -> "perform action"
            }
        } else {
            "perform action"
        }

        triggerDesc + actionDesc
    } catch (e: Exception) {
        "Automated workflow"
    }
}
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        onNavigateToCreateTask = {},
        onNavigateToEditTask = {},
        onNavigateToProfile = {}
    )
}