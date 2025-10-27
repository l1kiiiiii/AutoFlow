package com.example.autoflow.ui.theme.screens

import android.util.Log
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
import com.example.autoflow.model.TriggerTemplate
import com.example.autoflow.util.PredefinedModes
import com.example.autoflow.viewmodel.WorkflowViewModel
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModesScreen(
    viewModel: WorkflowViewModel,
    onModeSelected: (ModeTemplate) -> Unit,
    onNavigateBack: () -> Unit
) {
    // ✅ Observe workflows to check running status
    val workflows by viewModel.workflows.observeAsState(emptyList())

    // ✅ Find active Meeting Mode specifically
    val activeMeetingMode = workflows.find {
        it.workflowName.contains("Meeting Mode", ignoreCase = true) && it.isEnabled
    }

    // ✅ State for dialog
    var showDialog by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf<ModeTemplate?>(null) }

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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ✅ FIX ISSUE #1: Show Active Meeting Mode with DEACTIVATE button
            if (activeMeetingMode != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "🔇 Meeting Mode Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Auto-reply enabled • DND mode on",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = {
                                // ✅ SOLUTION #1: Disable Meeting Mode from ModesScreen
                                Log.d("ModesScreen", "🔴 Deactivating Meeting Mode from UI")
                                viewModel.updateWorkflowEnabled(
                                    workflowId = activeMeetingMode.id,
                                    enabled = false,
                                    callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                        override fun onSuccess(message: String) {
                                            Log.d("ModesScreen", "✅ Meeting Mode deactivated: $message")
                                        }
                                        override fun onError(error: String) {
                                            Log.e("ModesScreen", "❌ Error deactivating: $error")
                                        }
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("DEACTIVATE")
                        }
                    }
                }
            }

            // ✅ Show other running modes if any (except Meeting Mode)
            if (runningModesCount > 0) {
                ActiveModesSection(
                    workflows = workflows.filter { !it.workflowName.contains("Meeting Mode", ignoreCase = true) },
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

            // ✅ EXISTING: Rest of your modes grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(PredefinedModes.getAllModes()) { mode ->
                    // ✅ Only show modes that aren't currently active
                    val runningWorkflow = workflows.find { workflow ->
                        workflow.workflowName.contains(mode.name, ignoreCase = true) &&
                                workflow.isEnabled
                    }
                    val isRunning = runningWorkflow != null

                    if (!isRunning) {  // ✅ Hide active modes from grid
                        ModeCard(
                            mode = mode,
                            isRunning = false,
                            isMeetingMode = mode.name == "Meeting Mode",
                            onModeClick = {
                                selectedMode = mode
                                showDialog = true
                            },
                            onStopClick = { /* Not needed since not running */ }
                        )
                    }
                }
            }
        }
    }

    // ✅ Show configuration dialog popup for ALL modes
    selectedMode?.let { mode ->
        ModeConfigurationDialog(
            mode = mode,
            isVisible = showDialog,
            onDismiss = {
                showDialog = false
                selectedMode = null
            },
            onSave = { config ->
                when (config) {
                    is ModeConfig.Manual -> {
                        viewModel.createWorkflowFromMode(
                            mode = mode.copy(
                                defaultTriggers = listOf(
                                    TriggerTemplate("MANUAL", mapOf("type" to "quick_action"))
                                )
                            ),
                            callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                override fun onSuccess(message: String) {
                                    Log.d("ModesScreen", "✅ Mode activated: $message")
                                }
                                override fun onError(error: String) {
                                    Log.e("ModesScreen", "❌ Mode activation failed: $error")
                                }
                            }
                        )
                    }
                    is ModeConfig.Scheduled -> {
                        viewModel.createWorkflowFromMode(
                            mode = mode.copy(
                                defaultTriggers = listOf(
                                    TriggerTemplate("TIME", mapOf(
                                        "startTime" to config.startTime,
                                        "endTime" to config.endTime,
                                        "days" to "mon,tue,wed,thu,fri"
                                    ))
                                )
                            ),
                            callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                override fun onSuccess(message: String) {}
                                override fun onError(error: String) {}
                            }
                        )
                    }
                    is ModeConfig.Endless -> {
                        viewModel.createWorkflowFromMode(
                            mode = mode.copy(
                                defaultTriggers = listOf(
                                    TriggerTemplate("MANUAL", mapOf("type" to "endless"))
                                )
                            ),
                            callback = object : WorkflowViewModel.WorkflowOperationCallback {
                                override fun onSuccess(message: String) {}
                                override fun onError(error: String) {}
                            }
                        )
                    }
                }

                showDialog = false
                selectedMode = null
            }
        )
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
sealed class ModeConfig {
    object Manual : ModeConfig()
    object Endless : ModeConfig()
    data class Scheduled(val startTime: String, val endTime: String) : ModeConfig()
}

@Composable
fun ModeConfigurationDialog(
    mode: ModeTemplate,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (ModeConfig) -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface // ✅ Fixed
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    // Header
                    Text(
                        text = "Configure ${mode.name}",
                        style = MaterialTheme.typography.headlineSmall, // ✅ Fixed
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Type Selection
                    var selectedMode by remember { mutableStateOf("manual") }

                    // ✅ MOVE TIME VARIABLES HERE (outside if block)
                    var startTime by remember { mutableStateOf("09:00") }
                    var endTime by remember { mutableStateOf("17:00") }

                    Text("Mode Type:", fontWeight = FontWeight.Medium)
                    /*
                    // Manual Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = "manual" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == "manual",
                            onClick = { selectedMode = "manual" }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Manual Control", fontWeight = FontWeight.Medium)
                            Text("Turn on/off manually", style = MaterialTheme.typography.labelSmall) // ✅ Fixed
                        }
                    }
                    */
                    // Scheduled Mode
                    /*Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = "scheduled" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == "scheduled",
                            onClick = { selectedMode = "scheduled" }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Scheduled Mode", fontWeight = FontWeight.Medium)
                            Text("Set start and end times", style = MaterialTheme.typography.labelSmall) // ✅ Fixed
                        }
                    }

                     */

                    // Endless Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = "endless" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == "endless",
                            onClick = { selectedMode = "endless" }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Endless Mode", fontWeight = FontWeight.Medium)
                            Text("Runs until manually stopped", style = MaterialTheme.typography.labelSmall) // ✅ Fixed
                        }
                    }

                    // Time Configuration (only for scheduled mode)
                    if (selectedMode == "scheduled") {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Start Time
                        Text("Start Time:", fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            placeholder = { Text("HH:MM") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // End Time
                        Text("End Time:", fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            placeholder = { Text("HH:MM") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val config = when (selectedMode) {
                                    "manual" -> ModeConfig.Manual
                                    "scheduled" -> ModeConfig.Scheduled(startTime, endTime)
                                    "endless" -> ModeConfig.Endless
                                    else -> ModeConfig.Manual
                                }
                                onSave(config)
                                onDismiss()
                            }
                        ) {
                            Text("Save")
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