package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoflow.model.ModeTemplate
import com.example.autoflow.util.PredefinedModes
import com.example.autoflow.viewmodel.WorkflowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModesScreen(
    viewModel: WorkflowViewModel,
    onModeSelected: (ModeTemplate) -> Unit,
    onNavigateBack: () -> Unit
) {
    // ✅ Observe workflows to check running status
    val workflows by viewModel.workflows.observeAsState(emptyList())

    // ✅ Count running modes
    val runningModesCount = workflows.count { it.isEnabled }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Automation Modes")
                        if (runningModesCount > 0) {
                            Text(
                                text = "$runningModesCount modes running",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                },
                // ✅ Stop All button in top bar
                actions = {
                    if (runningModesCount > 0) {
                        TextButton(
                            onClick = {
                                // Stop all running modes
                                workflows.filter { it.isEnabled }.forEach { workflow ->
                                    viewModel.updateWorkflowEnabled(
                                        workflowId = workflow.id,
                                        enabled = false,
                                        callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                            override fun onSuccess(message: String) {}
                                            override fun onError(error: String) {}
                                        }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Stop All",
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STOP ALL",
                                color = Color(0xFFF44336),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ✅ Running modes section at top
            if (runningModesCount > 0) {
                ActiveModesSection(
                    workflows = workflows,
                    onStopMode = { workflowId ->
                        viewModel.updateWorkflowEnabled(
                            workflowId = workflowId,
                            enabled = false,
                            callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                override fun onSuccess(message: String) {}
                                override fun onError(error: String) {}
                            }
                        )
                    }
                )
            }

            // ✅ All modes grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(PredefinedModes.getAllModes()) { mode ->
                    // ✅ Check if this mode is currently running
                    val runningWorkflow = workflows.find { workflow ->
                        workflow.workflowName.contains(mode.name, ignoreCase = true) &&
                                workflow.isEnabled
                    }
                    val isRunning = runningWorkflow != null

                    // ✅ Special handling for Meeting Mode
                    val isMeetingMode = mode.name == "Meeting Mode"

                    ModeCard(
                        mode = mode,
                        isRunning = isRunning,
                        isMeetingMode = isMeetingMode,
                        onModeClick = {
                            if (isMeetingMode && PredefinedModes.isManualMode(mode)) {
                                // ✅ For Meeting Mode, toggle it directly
                                if (isRunning) {
                                    // Stop Meeting Mode
                                    runningWorkflow?.let { workflow ->
                                        viewModel.updateWorkflowEnabled(
                                            workflowId = workflow.id,
                                            enabled = false,
                                            callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                                override fun onSuccess(message: String) {}
                                                override fun onError(error: String) {}
                                            }
                                        )
                                    }
                                } else {
                                    // ✅ Start Meeting Mode using createWorkflowFromMode
                                    viewModel.createWorkflowFromMode(
                                        mode = mode,
                                        callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                            override fun onSuccess(message: String) {}
                                            override fun onError(error: String) {}
                                        }
                                    )
                                }
                            } else {
                                // ✅ For other modes, open configuration
                                onModeSelected(mode)
                            }
                        },
                        onStopClick = {
                            if (isRunning && runningWorkflow != null) {
                                viewModel.updateWorkflowEnabled(
                                    workflowId = runningWorkflow.id,
                                    enabled = false,
                                    callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                        override fun onSuccess(message: String) {}
                                        override fun onError(error: String) {}
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveModesSection(
    workflows: List<com.example.autoflow.data.WorkflowEntity>,
    onStopMode: (Long) -> Unit
) {
    val runningWorkflows = workflows.filter { it.isEnabled }

    if (runningWorkflows.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🟢 CURRENTLY RUNNING",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    Text(
                        text = "${runningWorkflows.size} active",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ✅ List of running modes with individual stop buttons
                runningWorkflows.forEach { workflow ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // ✅ Running indicator dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = workflow.workflowName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // ✅ Individual stop button
                        OutlinedButton(
                            onClick = { onStopMode(workflow.id) },
                            modifier = Modifier.size(width = 70.dp, height = 32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF44336)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFF44336)
                            ),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "STOP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeCard(
    mode: ModeTemplate,
    isRunning: Boolean = false,
    isMeetingMode: Boolean = false,
    onModeClick: () -> Unit,
    onStopClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onModeClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(mode.color))
                .copy(alpha = if (isRunning) 0.25f else 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ✅ Top row with icon and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = mode.icon,
                    style = MaterialTheme.typography.displayMedium
                )

                // ✅ Status indicator
                if (isRunning) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(12.dp)
                    ) {}
                }
            }

            // ✅ Middle - Mode info
            Column {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // ✅ Bottom - Status and action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning) {
                    Text(
                        text = "● RUNNING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                    )

                    // ✅ STOP button on card
                    Button(
                        onClick = onStopClick,
                        modifier = Modifier.size(width = 70.dp, height = 28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "STOP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // ✅ Different text for Meeting Mode vs other modes
                    Text(
                        text = if (isMeetingMode) "Tap to enable" else "Tap to configure",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // ✅ Quick action button for Meeting Mode
                    if (isMeetingMode) {
                        Button(
                            onClick = onModeClick,
                            modifier = Modifier.size(width = 70.dp, height = 28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(android.graphics.Color.parseColor(mode.color))
                            ),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                text = "START",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun ModeCardPreview() {
    ModeCard(
        mode = PredefinedModes.getAllModes()[0],
        onModeClick = {}
    )
}
@Preview(showBackground = true)
@Composable
fun ActiveModesSectionPreview() {
    ActiveModesSection(
        workflows = emptyList(),
        onStopMode = {}
    )
}