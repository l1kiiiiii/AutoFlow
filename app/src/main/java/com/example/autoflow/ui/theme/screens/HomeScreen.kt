package com.example.autoflow.ui.theme.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.viewmodel.WorkflowViewModel
import com.example.autoflow.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Delete confirmation dialog state
    var workflowToDelete by remember { mutableStateOf<WorkflowEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(paddingValues)
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
                        WorkflowCardWithActions(
                            workflow = workflow,
                            onToggle = {
                                viewModel.updateWorkflowEnabled(workflow.id, !workflow.isEnabled)
                            },
                            onEdit = { onNavigateToEditTask(workflow.id) },
                            onDelete = {
                                workflowToDelete = workflow
                                showDeleteDialog = true
                            }
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
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No workflows yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button to create your first automation",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Space for bottom nav
            }

            // Floating Action Button
            FloatingActionButton(
                onClick = onNavigateToCreateTask,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 90.dp),
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

    // Delete Confirmation Dialog
    if (showDeleteDialog && workflowToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                workflowToDelete = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Workflow?") },
            text = {
                Column {
                    Text("Are you sure you want to delete '${workflowToDelete?.workflowName}'?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        workflowToDelete?.let { workflow ->
                            scope.launch {
                                try {
                                    viewModel.deleteWorkflow(workflow.id)
                                    snackbarHostState.showSnackbar(
                                        message = "Workflow '${workflow.workflowName}' deleted",
                                        duration = SnackbarDuration.Short
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to delete workflow",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                        showDeleteDialog = false
                        workflowToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
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
}

@Composable
fun WorkflowCardWithActions(
    workflow: WorkflowEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val triggerInfo = getWorkflowIcon(workflow)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
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

                // Actions Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            // Toggle Switch Section
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (workflow.isEnabled) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (workflow.isEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

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
}

// Alternative: Swipeable Workflow Card (Advanced)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableWorkflowCard(
    workflow: WorkflowEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipe = -200f // Swipe threshold for actions
    val triggerInfo = getWorkflowIcon(workflow)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        // Background actions (revealed on swipe)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit button
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Edit",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Delete button
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.error,
                        RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Delete",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Foreground card (swipeable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Snap to action if swiped far enough
                            when {
                                offsetX < -150 -> onDelete()
                                offsetX < -75 -> onEdit()
                                else -> offsetX = 0f
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val newOffset = (offsetX + dragAmount).coerceIn(maxSwipe, 0f)
                        offsetX = newOffset
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            // Same card content as WorkflowCardWithActions...
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workflow.workflowName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
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

                Switch(
                    checked = workflow.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
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
                Constants.ACTION_SEND_NOTIFICATION -> "send notification"
                Constants.ACTION_TOGGLE_WIFI -> "turn off WiFi"
                "SET_DND" -> "enable DND"
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
