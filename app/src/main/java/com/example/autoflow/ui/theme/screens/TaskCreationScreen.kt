package com.example.autoflow.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.viewmodel.WorkflowViewModel
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.util.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Production-ready Task Creation Screen with comprehensive error handling
 * Features:
 * - Null safety throughout
 * - Try-catch blocks for critical operations
 * - Input validation
 * - Proper state management
 * - Memory leak prevention
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreationScreen(
    modifier: Modifier = Modifier,
    workflowId: Long? = null,
    onBack: () -> Unit = {},
    onSaveTask: (taskName: String) -> Unit = {},
    viewModel: WorkflowViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load existing workflow if editing
    val workflows by viewModel.getWorkflows().observeAsState(emptyList())
    val existingWorkflow = remember(workflowId, workflows) {
        workflowId?.let { id -> workflows?.find { it.id == id } }
    }

    // Task name state
    var taskName by remember { mutableStateOf("") }
    var taskNameError by remember { mutableStateOf<String?>(null) }

    // Trigger states
    var locationTriggerExpanded by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }
    var locationDetailsInput by remember { mutableStateOf("") }
    var radiusValue by remember { mutableFloatStateOf(100f) }
    var triggerOnOption by remember { mutableStateOf("Entry") }

    var wifiTriggerExpanded by remember { mutableStateOf(false) }
    var wifiState by remember { mutableStateOf("On") }

    var timeTriggerExpanded by remember { mutableStateOf(false) }
    var timeValue by remember { mutableStateOf("") }

    var bluetoothDeviceTriggerExpanded by remember { mutableStateOf(false) }
    var bluetoothDeviceAddress by remember { mutableStateOf("") }

    // Action states
    var sendNotificationActionExpanded by remember { mutableStateOf(false) }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationPriority by remember { mutableStateOf("Normal") }

    var toggleSettingsActionExpanded by remember { mutableStateOf(false) }
    var toggleSetting by remember { mutableStateOf("WiFi") }

    var runScriptActionExpanded by remember { mutableStateOf(false) }
    var scriptText by remember { mutableStateOf("") }

    // Error/success messages
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessSnackbar by remember { mutableStateOf(false) }

    //  Sound Mode Action states
    var setSoundModeActionExpanded by remember { mutableStateOf(false) }
    var soundMode by remember { mutableStateOf("Normal") }

    // Block Apps Action states
    var blockAppsActionExpanded by remember { mutableStateOf(false) }
    var selectedAppsToBlock by remember { mutableStateOf<List<String>>(emptyList()) }

    // Pre-populate fields if editing
    LaunchedEffect(existingWorkflow) {
        existingWorkflow?.let { workflow ->
            try {
                taskName = workflow.workflowName

                workflow.toTrigger()?.let { trigger ->
                    when (trigger.type) {
                        Constants.TRIGGER_TIME -> {
                            timeTriggerExpanded = true
                            timeValue = trigger.value ?: ""
                        }
                        Constants.TRIGGER_WIFI -> {
                            wifiTriggerExpanded = true
                            wifiState = trigger.value ?: "On"
                        }
                        Constants.TRIGGER_BLE -> {
                            bluetoothDeviceTriggerExpanded = true
                            bluetoothDeviceAddress = trigger.value ?: ""
                        }
                        Constants.TRIGGER_LOCATION -> {
                            locationTriggerExpanded = true
                            try {
                                val locationJson = JSONObject(trigger.value ?: "{}")
                                locationName = locationJson.optString("locationName", "")
                                locationDetailsInput = locationJson.optString("coordinates", "")
                                radiusValue = locationJson.optDouble("radius", 100.0).toFloat()
                            } catch (e: Exception) {
                                Log.e("TaskCreation", "Error parsing location JSON", e)
                            }
                        }
                    }
                }

                workflow.toAction()?.let { action ->
                    when (action.type) {
                        Constants.ACTION_SEND_NOTIFICATION -> {
                            sendNotificationActionExpanded = true
                            notificationTitle = action.title ?: ""
                            notificationMessage = action.message ?: ""
                            notificationPriority = action.priority ?: "Normal"
                        }
                        Constants.ACTION_TOGGLE_WIFI -> {
                            toggleSettingsActionExpanded = true
                            toggleSetting = action.value ?: "WiFi"
                        }
                        "RUN_SCRIPT" -> {
                            runScriptActionExpanded = true
                            scriptText = action.value ?: ""
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskCreation", "Error loading existing workflow", e)
                errorMessage = "Error loading workflow: ${e.message}"
                showErrorDialog = true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            TaskNameCard(
                taskName = taskName,
                onTaskNameChange = {
                    taskName = it
                    taskNameError = null
                },
                error = taskNameError
            )

            // Triggers Card
            TriggersCard(
                locationTriggerExpanded = locationTriggerExpanded,
                onLocationExpandedChange = { locationTriggerExpanded = it },
                locationName = locationName,
                onLocationNameChange = { locationName = it },
                locationDetailsInput = locationDetailsInput,
                onLocationDetailsChange = { locationDetailsInput = it },
                radiusValue = radiusValue,
                onRadiusChange = { radiusValue = it },
                triggerOnOption = triggerOnOption,
                onTriggerOptionChange = { triggerOnOption = it },
                wifiTriggerExpanded = wifiTriggerExpanded,
                onWifiExpandedChange = { wifiTriggerExpanded = it },
                wifiState = wifiState,
                onWifiStateChange = { wifiState = it },
                timeTriggerExpanded = timeTriggerExpanded,
                onTimeExpandedChange = { timeTriggerExpanded = it },
                onTimeValueChange = { timeValue = it },
                bluetoothDeviceTriggerExpanded = bluetoothDeviceTriggerExpanded,
                onBluetoothExpandedChange = { bluetoothDeviceTriggerExpanded = it },
                bluetoothDeviceAddress = bluetoothDeviceAddress,
                onBluetoothAddressChange = { bluetoothDeviceAddress = it }
            )

            // Actions Card
            ActionsCard(
                sendNotificationActionExpanded = sendNotificationActionExpanded,
                onNotificationExpandedChange = { sendNotificationActionExpanded = it },
                notificationTitle = notificationTitle,
                onNotificationTitleChange = { notificationTitle = it },
                notificationMessage = notificationMessage,
                onNotificationMessageChange = { notificationMessage = it },
                notificationPriority = notificationPriority,
                onNotificationPriorityChange = { notificationPriority = it },
                toggleSettingsActionExpanded = toggleSettingsActionExpanded,
                onToggleExpandedChange = { toggleSettingsActionExpanded = it },
                toggleSetting = toggleSetting,
                onToggleSettingChange = { toggleSetting = it },
                setSoundModeActionExpanded = setSoundModeActionExpanded,
                onSoundModeExpandedChange = { setSoundModeActionExpanded = it },
                soundMode = soundMode,
                onSoundModeChange = { soundMode = it },
                blockAppsActionExpanded = blockAppsActionExpanded,
                onBlockAppsExpandedChange = { blockAppsActionExpanded = it },
                selectedAppsToBlock = selectedAppsToBlock,
                onSelectedAppsChange = { selectedAppsToBlock = it }
            )

            // Save/Back Buttons Card
            SaveButtonsCard(
                workflowId = workflowId,
                onSave = {
                    scope.launch {
                        handleSaveTask(
                            context = context,
                            viewModel = viewModel,
                            workflowId = workflowId,
                            taskName = taskName,
                            locationTriggerExpanded = locationTriggerExpanded,
                            locationName = locationName,
                            locationDetailsInput = locationDetailsInput,
                            radiusValue = radiusValue,
                            triggerOnOption = triggerOnOption,
                            wifiTriggerExpanded = wifiTriggerExpanded,
                            wifiState = wifiState,
                            timeTriggerExpanded = timeTriggerExpanded,
                            timeValue = timeValue,
                            bluetoothDeviceTriggerExpanded = bluetoothDeviceTriggerExpanded,
                            bluetoothDeviceAddress = bluetoothDeviceAddress,
                            sendNotificationActionExpanded = sendNotificationActionExpanded,
                            notificationTitle = notificationTitle,
                            notificationMessage = notificationMessage,
                            notificationPriority = notificationPriority,
                            toggleSettingsActionExpanded = toggleSettingsActionExpanded,
                            toggleSetting = toggleSetting,
                            runScriptActionExpanded = runScriptActionExpanded,
                            scriptText = scriptText,
                            onSuccess = {
                                showSuccessSnackbar = true
                                onSaveTask(taskName)
                            },
                            onError = { error ->
                                errorMessage = error
                                taskNameError = if (error.contains("name")) error else null
                                showErrorDialog = true
                            }
                        )
                    }
                },
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error Dialog
        if (showErrorDialog) {
            ErrorDialog(
                errorMessage = errorMessage,
                onDismiss = { showErrorDialog = false }
            )
        }

        // Success Snackbar
        if (showSuccessSnackbar) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showSuccessSnackbar = false
            }
        }
    }
}
// ========== COMPOSABLE COMPONENTS ==========

@Composable
private fun TaskNameCard(
    taskName: String,
    onTaskNameChange: (String) -> Unit,
    error: String?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Create New Task",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = taskName,
                onValueChange = onTaskNameChange,
                label = { Text("Task Name") },
                placeholder = { Text("Enter task name") },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true
            )
        }
    }
}

@Composable
private fun TriggersCard(
    locationTriggerExpanded: Boolean,
    onLocationExpandedChange: (Boolean) -> Unit,
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    locationDetailsInput: String,
    onLocationDetailsChange: (String) -> Unit,
    radiusValue: Float,
    onRadiusChange: (Float) -> Unit,
    triggerOnOption: String,
    onTriggerOptionChange: (String) -> Unit,
    wifiTriggerExpanded: Boolean,
    onWifiExpandedChange: (Boolean) -> Unit,
    wifiState: String,
    onWifiStateChange: (String) -> Unit,
    timeTriggerExpanded: Boolean,
    onTimeExpandedChange: (Boolean) -> Unit,
    onTimeValueChange: (String) -> Unit,
    bluetoothDeviceTriggerExpanded: Boolean,
    onBluetoothExpandedChange: (Boolean) -> Unit,
    bluetoothDeviceAddress: String,
    onBluetoothAddressChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Configure Triggers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Location Trigger
            ExpandableTriggerSection(
                title = "Location",
                icon = Icons.Default.LocationOn,
                expanded = locationTriggerExpanded,
                onExpandedChange = onLocationExpandedChange
            ) {
                LocationTriggerContent(
                    locationName = locationName,
                    onLocationNameChange = onLocationNameChange,
                    locationDetailsInput = locationDetailsInput,
                    onLocationDetailsChange = onLocationDetailsChange,
                    radiusValue = radiusValue,
                    onRadiusChange = onRadiusChange,
                    triggerOnOption = triggerOnOption,
                    onTriggerOptionChange = onTriggerOptionChange
                )
            }

            // Time Trigger
            ExpandableTriggerSection(
                title = "Time",
                icon = Icons.Default.Schedule,
                expanded = timeTriggerExpanded,
                onExpandedChange = onTimeExpandedChange
            ) {
                TimeTriggerContent(onTimeValueChange = onTimeValueChange)
            }

            // WiFi Trigger
            ExpandableTriggerSection(
                title = "Wi-Fi Network",
                icon = Icons.Default.Wifi,
                expanded = wifiTriggerExpanded,
                onExpandedChange = onWifiExpandedChange
            ) {
                WiFiTriggerContent(
                    wifiState = wifiState,
                    onWifiStateChange = onWifiStateChange
                )
            }

            // Bluetooth Trigger
            ExpandableTriggerSection(
                title = "Bluetooth Device",
                icon = Icons.Default.Bluetooth,
                expanded = bluetoothDeviceTriggerExpanded,
                onExpandedChange = onBluetoothExpandedChange
            ) {
                BluetoothTriggerContent(
                    bluetoothDeviceAddress = bluetoothDeviceAddress,
                    onBluetoothAddressChange = onBluetoothAddressChange
                )
            }
        }
    }
}

// ========== UPDATED ACTIONS CARD ==========

@Composable
private fun ActionsCard(
    sendNotificationActionExpanded: Boolean,
    onNotificationExpandedChange: (Boolean) -> Unit,
    notificationTitle: String,
    onNotificationTitleChange: (String) -> Unit,
    notificationMessage: String,
    onNotificationMessageChange: (String) -> Unit,
    notificationPriority: String,
    onNotificationPriorityChange: (String) -> Unit,
    toggleSettingsActionExpanded: Boolean,
    onToggleExpandedChange: (Boolean) -> Unit,
    toggleSetting: String,
    onToggleSettingChange: (String) -> Unit,
    setSoundModeActionExpanded: Boolean,
    onSoundModeExpandedChange: (Boolean) -> Unit,
    soundMode: String,
    onSoundModeChange: (String) -> Unit,
    blockAppsActionExpanded: Boolean,
    onBlockAppsExpandedChange: (Boolean) -> Unit,
    selectedAppsToBlock: List<String>,
    onSelectedAppsChange: (List<String>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Define Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Send Notification Action
            ExpandableActionSection(
                title = "Send Notification",
                icon = Icons.Default.Notifications,
                expanded = sendNotificationActionExpanded,
                onExpandedChange = onNotificationExpandedChange
            ) {
                NotificationActionContent(
                    notificationTitle = notificationTitle,
                    onNotificationTitleChange = onNotificationTitleChange,
                    notificationMessage = notificationMessage,
                    onNotificationMessageChange = onNotificationMessageChange,
                    notificationPriority = notificationPriority,
                    onNotificationPriorityChange = onNotificationPriorityChange
                )
            }

            // Toggle Settings Action
            ExpandableActionSection(
                title = "Toggle Settings",
                icon = Icons.Default.Settings,
                expanded = toggleSettingsActionExpanded,
                onExpandedChange = onToggleExpandedChange
            ) {
                ToggleSettingsContent(
                    toggleSetting = toggleSetting,
                    onToggleSettingChange = onToggleSettingChange
                )
            }

            // NEW: Set Sound Mode Action
            ExpandableActionSection(
                title = "Set Sound Mode",
                icon = Icons.Default.VolumeUp,
                expanded = setSoundModeActionExpanded,
                onExpandedChange = onSoundModeExpandedChange
            ) {
                SetSoundModeContent(
                    soundMode = soundMode,
                    onSoundModeChange = onSoundModeChange
                )
            }

            // NEW: Block Apps Action
            ExpandableActionSection(
                title = "Block Apps",
                icon = Icons.Default.Block,
                expanded = blockAppsActionExpanded,
                onExpandedChange = onBlockAppsExpandedChange
            ) {
                BlockAppsContent(
                    selectedApps = selectedAppsToBlock,
                    onSelectedAppsChange = onSelectedAppsChange
                )
            }
        }
    }
}
// ========== NEW ACTION CONTENT COMPONENTS ==========

@Composable
private fun SetSoundModeContent(
    soundMode: String,
    onSoundModeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Select Sound Profile:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        // Sound mode options
        val soundModes = listOf(
            SoundModeOption("Normal", Icons.Default.VolumeUp, "Normal ringing and notifications"),
            SoundModeOption("Silent", Icons.Default.VolumeOff, "No sound or vibration"),
            SoundModeOption("Vibrate", Icons.Default.Vibration, "Vibrate only, no sound"),
            SoundModeOption("DND", Icons.Default.DoNotDisturb, "Do Not Disturb mode")
        )

        soundModes.forEach { mode ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSoundModeChange(mode.name) },
                colors = CardDefaults.cardColors(
                    containerColor = if (soundMode == mode.name)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = if (soundMode == mode.name)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else
                    null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = mode.name,
                        modifier = Modifier.size(32.dp),
                        tint = if (soundMode == mode.name)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (soundMode == mode.name) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (soundMode == mode.name) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Info card for DND mode
        if (soundMode == "DND") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "DND mode requires notification policy access permission",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockAppsContent(
    selectedApps: List<String>,
    onSelectedAppsChange: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Select apps to block when triggered:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        // Selected apps display
        if (selectedApps.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Selected Apps (${selectedApps.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    selectedApps.forEach { appPackage ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = getAppName(context, appPackage),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            IconButton(
                                onClick = {
                                    onSelectedAppsChange(selectedApps - appPackage)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (appPackage != selectedApps.last()) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Add apps button
        Button(
            onClick = {
                isLoading = true
                try {
                    installedApps = getInstalledApps(context)
                    showAppPicker = true
                } catch (e: Exception) {
                    Log.e("BlockApps", "Error loading apps", e)
                } finally {
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Loading Apps...")
            } else {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedApps.isEmpty()) "Select Apps to Block" else "Add More Apps")
            }
        }

        // Quick presets
        Text(
            "Quick Presets:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(appBlockPresets.keys.toList()) { presetName ->
                FilterChip(
                    selected = false,
                    onClick = {
                        val presetApps = appBlockPresets[presetName] ?: emptyList()
                        onSelectedAppsChange((selectedApps + presetApps).distinct())
                    },
                    label = { Text(presetName) },
                    leadingIcon = {
                        Icon(
                            imageVector = getPresetIcon(presetName),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Blocked apps will be restricted when this automation triggers. Requires accessibility permission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    // App picker dialog
    if (showAppPicker) {
        AppPickerDialog(
            apps = installedApps,
            selectedApps = selectedApps,
            onDismiss = { showAppPicker = false },
            onConfirm = { newSelection ->
                onSelectedAppsChange(newSelection)
                showAppPicker = false
            }
        )
    }
}
@Composable
private fun AppPickerDialog(
    apps: List<AppInfo>,
    selectedApps: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var tempSelectedApps by remember { mutableStateOf(selectedApps) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Apps to Block") },
        text = {
            Column {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${tempSelectedApps.size} apps selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Apps list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(filteredApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempSelectedApps = if (app.packageName in tempSelectedApps) {
                                        tempSelectedApps - app.packageName
                                    } else {
                                        tempSelectedApps + app.packageName
                                    }
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Checkbox(
                                checked = app.packageName in tempSelectedApps,
                                onCheckedChange = null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempSelectedApps) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
// ========== UTILITY FUNCTIONS ==========

private fun getInstalledApps(context: Context): List<AppInfo> {
    return try {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        packages
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // Filter non-system apps
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(appInfo)
                )
            }
            .sortedBy { it.appName }
    } catch (e: Exception) {
        Log.e("AppPicker", "Error getting installed apps", e)
        emptyList()
    }
}

private fun getAppName(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName
    }
}

private val appBlockPresets = mapOf(
    "Social Media" to listOf(
        "com.facebook.katana",
        "com.instagram.android",
        "com.twitter.android",
        "com.snapchat.android",
        "com.zhiliaoapp.musically" // TikTok
    ),
    "Gaming" to listOf(
        "com.pubg.imobile",
        "com.supercell.clashofclans",
        "com.king.candycrushsaga",
        "com.roblox.client"
    ),
    "Streaming" to listOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.spotify.music"
    ),
    "Shopping" to listOf(
        "com.amazon.mShop.android.shopping",
        "com.ebay.mobile",
        "com.shopify.mobile"
    )
)

private fun getPresetIcon(presetName: String): ImageVector {
    return when (presetName) {
        "Social Media" -> Icons.Default.Group
        "Gaming" -> Icons.Default.SportsEsports
        "Streaming" -> Icons.Default.PlayCircle
        "Shopping" -> Icons.Default.ShoppingCart
        else -> Icons.Default.Apps
    }
}
// ========== SUPPORTING COMPONENTS AND DATA CLASSES ==========

data class SoundModeOption(
    val name: String,
    val icon: ImageVector,
    val description: String
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?
)


@Composable
private fun SaveButtonsCard(
    workflowId: Long?,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (workflowId != null) Icons.Default.Edit else Icons.Default.Save,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (workflowId != null) "Update Task" else "Save Task")
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
        }
    }
}

// ========== EXPANDABLE SECTIONS ==========

@Composable
private fun ExpandableTriggerSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        if (expanded) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
            content()
        }
    }
}

@Composable
private fun ExpandableActionSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        if (expanded) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
            content()
        }
    }
}

// ========== TRIGGER CONTENT COMPONENTS ==========

@Composable
private fun LocationTriggerContent(
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    locationDetailsInput: String,
    onLocationDetailsChange: (String) -> Unit,
    radiusValue: Float,
    onRadiusChange: (Float) -> Unit,
    triggerOnOption: String,
    onTriggerOptionChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = locationName,
            onValueChange = onLocationNameChange,
            label = { Text("Location Name") },
            placeholder = { Text("Home, Office, etc.") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Home, null) }
        )

        OutlinedTextField(
            value = locationDetailsInput,
            onValueChange = onLocationDetailsChange,
            label = { Text("Coordinates (lat,lng)") },
            placeholder = { Text("37.7749,-122.4194") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.MyLocation, null) }
        )

        Text("Radius: ${radiusValue.roundToInt()}m", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = radiusValue,
            onValueChange = onRadiusChange,
            valueRange = 50f..500f,
            steps = 44,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Trigger On:", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Entry", "Exit", "Both").forEach { option ->
                FilterChip(
                    selected = triggerOnOption == option,
                    onClick = { onTriggerOptionChange(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeTriggerContent(
    onTimeValueChange: (String) -> Unit
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DateRange, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(selectedDate?.toString() ?: "Select Date")
        }

        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Schedule, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(selectedTime?.toString() ?: "Select Time")
        }

        if (selectedDate != null && selectedTime != null) {
            val dateTime = LocalDateTime.of(selectedDate, selectedTime)
            val timestamp = java.time.ZoneId.systemDefault().rules
                .getOffset(dateTime).totalSeconds + dateTime.toEpochSecond(java.time.ZoneOffset.UTC)
            onTimeValueChange(timestamp.toString())

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    "Scheduled: ${dateTime}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun WiFiTriggerContent(
    wifiState: String,
    onWifiStateChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Target State:", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("On", "Off").forEach { state ->
                FilterChip(
                    selected = wifiState == state,
                    onClick = { onWifiStateChange(state) },
                    label = { Text(state) }
                )
            }
        }
    }
}

@Composable
private fun BluetoothTriggerContent(
    bluetoothDeviceAddress: String,
    onBluetoothAddressChange: (String) -> Unit
) {
    val context = LocalContext.current
    var showDevicePicker by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDeviceInfo>>(emptyList()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = {
                try {
                    pairedDevices = getPairedBluetoothDevices(context)
                    showDevicePicker = true
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error fetching devices", e)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Bluetooth, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Paired Device")
        }

        OutlinedTextField(
            value = bluetoothDeviceAddress,
            onValueChange = onBluetoothAddressChange,
            label = { Text("Device MAC Address") },
            placeholder = { Text("XX:XX:XX:XX:XX:XX") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            confirmButton = { TextButton(onClick = { showDevicePicker = false }) { Text("Cancel") } },
            title = { Text("Paired Bluetooth Devices") },
            text = {
                if (pairedDevices.isEmpty()) {
                    Text("No paired devices found")
                } else {
                    LazyColumn {
                        items(pairedDevices) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onBluetoothAddressChange(device.address)
                                        showDevicePicker = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(device.name, fontWeight = FontWeight.Bold)
                                    Text(device.address, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

// ========== ACTION CONTENT COMPONENTS ==========

@Composable
private fun NotificationActionContent(
    notificationTitle: String,
    onNotificationTitleChange: (String) -> Unit,
    notificationMessage: String,
    onNotificationMessageChange: (String) -> Unit,
    notificationPriority: String,
    onNotificationPriorityChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = notificationTitle,
            onValueChange = onNotificationTitleChange,
            label = { Text("Title") },
            placeholder = { Text("Notification title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notificationMessage,
            onValueChange = onNotificationMessageChange,
            label = { Text("Message") },
            placeholder = { Text("Notification message") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Text("Priority:", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Low", "Normal", "High").forEach { priority ->
                FilterChip(
                    selected = notificationPriority == priority,
                    onClick = { onNotificationPriorityChange(priority) },
                    label = { Text(priority) }
                )
            }
        }
    }
}

@Composable
private fun ToggleSettingsContent(
    toggleSetting: String,
    onToggleSettingChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Setting to Toggle:", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("WiFi", "Bluetooth").forEach { setting ->
                FilterChip(
                    selected = toggleSetting == setting,
                    onClick = { onToggleSettingChange(setting) },
                    label = { Text(setting) }
                )
            }
        }
    }
}

@Composable
private fun RunScriptContent(
    scriptText: String,
    onScriptTextChange: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf("Custom") }

    val templates = mapOf(
        "Custom" to "",
        "Log Message" to "log('Hello from AutoFlow!');",
        "Send Notification" to "notify('Alert', 'Script executed!');",
        "HTTP Request" to "var response = httpGet('https://api.example.com');"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Templates:", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates.keys.toList()) { template ->
                FilterChip(
                    selected = selectedTemplate == template,
                    onClick = {
                        selectedTemplate = template
                        onScriptTextChange(templates[template] ?: "")
                    },
                    label = { Text(template) }
                )
            }
        }

        OutlinedTextField(
            value = scriptText,
            onValueChange = onScriptTextChange,
            label = { Text("JavaScript Code") },
            placeholder = { Text("Enter code here...") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { validateScript(scriptText) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Validate")
            }
            Button(
                onClick = { testScriptExecution(scriptText, context) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Test")
            }
        }
    }
}

// ========== ERROR DIALOG ==========

@Composable
private fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = { Text("Error") },
        text = { Text(errorMessage) },
        icon = { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) }
    )
}

// ========== SAVE HANDLER ==========

private suspend fun handleSaveTask(
    context: Context,
    viewModel: WorkflowViewModel,
    workflowId: Long?,
    taskName: String,
    locationTriggerExpanded: Boolean,
    locationName: String,
    locationDetailsInput: String,
    radiusValue: Float,
    triggerOnOption: String,
    wifiTriggerExpanded: Boolean,
    wifiState: String,
    timeTriggerExpanded: Boolean,
    timeValue: String,
    bluetoothDeviceTriggerExpanded: Boolean,
    bluetoothDeviceAddress: String,
    sendNotificationActionExpanded: Boolean,
    notificationTitle: String,
    notificationMessage: String,
    notificationPriority: String,
    toggleSettingsActionExpanded: Boolean,
    toggleSetting: String,
    runScriptActionExpanded: Boolean,
    scriptText: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Validate task name
        if (taskName.isBlank()) {
            onError("Task name cannot be empty")
            return
        }

        // Create trigger
        val trigger = when {
            locationTriggerExpanded && locationDetailsInput.isNotBlank() -> {
                val parts = locationDetailsInput.split(",").map { it.trim() }
                if (parts.size != 2) {
                    onError("Invalid coordinate format. Use: lat,lng")
                    return
                }

                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()

                if (lat == null || lng == null) {
                    onError("Invalid coordinate values")
                    return
                }

                val json = JSONObject().apply {
                    put("locationName", locationName.ifEmpty { "Unnamed" })
                    put("coordinates", locationDetailsInput)
                    put("latitude", lat)
                    put("longitude", lng)
                    put("radius", radiusValue.roundToInt())
                    put("triggerOnEntry", triggerOnOption in listOf("Entry", "Both"))
                    put("triggerOnExit", triggerOnOption in listOf("Exit", "Both"))
                }.toString()

                Trigger(0, 0, Constants.TRIGGER_LOCATION, json)
            }
            wifiTriggerExpanded -> Trigger(0, 0, Constants.TRIGGER_WIFI, wifiState)
            timeTriggerExpanded && timeValue.isNotBlank() -> Trigger(0, 0, Constants.TRIGGER_TIME, timeValue)
            bluetoothDeviceTriggerExpanded && bluetoothDeviceAddress.isNotBlank() ->
                Trigger(0, 0, Constants.TRIGGER_BLE, bluetoothDeviceAddress)
            else -> {
                onError("Please configure at least one trigger")
                return
            }
        }

        // Create action
        val action = when {
            sendNotificationActionExpanded && notificationTitle.isNotBlank() ->
                Action(Constants.ACTION_SEND_NOTIFICATION, notificationTitle, notificationMessage, notificationPriority)
            toggleSettingsActionExpanded ->
                Action(Constants.ACTION_TOGGLE_WIFI, null, null, "Normal").apply { setValue(toggleSetting) }
            runScriptActionExpanded && scriptText.isNotBlank() ->
                Action("RUN_SCRIPT", null, null, null).apply { setValue(scriptText) }
            else -> {
                onError("Please configure at least one action")
                return
            }
        }

        // Save workflow
        if (workflowId != null) {
            viewModel.updateWorkflow(
                workflowId,
                taskName,
                trigger,
                action,
                object : WorkflowViewModel.WorkflowOperationCallback {
                    override fun onSuccess(message: String) {
                        Log.d("TaskCreation", "Update successful: $message")
                        onSuccess()
                    }
                    override fun onError(error: String) {
                        Log.e("TaskCreation", "Update failed: $error")
                        onError(error)
                    }
                }
            )
        } else {
            viewModel.addWorkflow(
                trigger,
                action,
                object : WorkflowViewModel.WorkflowOperationCallback {
                    override fun onSuccess(message: String) {
                        Log.d("TaskCreation", "Creation successful: $message")

                        // Schedule alarm if time trigger
                        if (trigger.type == Constants.TRIGGER_TIME) {
                            try {
                                val triggerTime = timeValue.toLongOrNull()
                                if (triggerTime != null) {
                                    AlarmScheduler.scheduleNotification(
                                        context,
                                        0L,
                                        triggerTime,
                                        action.title ?: "AutoFlow Alert",
                                        action.message ?: "Trigger activated"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("TaskCreation", "Failed to schedule alarm", e)
                            }
                        }

                        onSuccess()
                    }
                    override fun onError(error: String) {
                        Log.e("TaskCreation", "Creation failed: $error")
                        onError(error)
                    }
                }
            )
        }
    } catch (e: Exception) {
        Log.e("TaskCreation", "Error saving task", e)
        onError("Unexpected error: ${e.message}")
    }
}

// ========== UTILITY FUNCTIONS ==========

data class BluetoothDeviceInfo(val name: String, val address: String)

@SuppressLint("MissingPermission")
fun getPairedBluetoothDevices(context: Context): List<BluetoothDeviceInfo> {
    return try {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            emptyList()
        } else {
            adapter.bondedDevices?.map {
                BluetoothDeviceInfo(it.name ?: "Unknown", it.address)
            } ?: emptyList()
        }
    } catch (e: Exception) {
        Log.e("Bluetooth", "Error getting devices", e)
        emptyList()
    }
}

private fun testScriptExecution(scriptCode: String, context: Context) {
    Log.i("ScriptTest", "Testing: $scriptCode")
    // Add actual script execution logic here
}

private fun validateScript(scriptCode: String) {
    if (scriptCode.trim().isEmpty()) {
        Log.w("ScriptValidation", "Empty script")
        return
    }

    var parenCount = 0
    var braceCount = 0
    scriptCode.forEach { char ->
        when (char) {
            '(' -> parenCount++
            ')' -> parenCount--
            '{' -> braceCount++
            '}' -> braceCount--
        }
    }

    if (parenCount != 0 || braceCount != 0) {
        Log.w("ScriptValidation", "Unbalanced brackets")
    } else {
        Log.i("ScriptValidation", "Validation passed")
    }
}

// ========== PREVIEW ==========

@Preview(showBackground = true)
@Composable
fun TaskCreationScreenPreview() {
    AutoFlowTheme {
        TaskCreationScreen()
    }
}
