package com.example.autoflow.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.data.WorkflowEntity
import com.example.autoflow.data.toAction
import com.example.autoflow.data.toTrigger
import com.example.autoflow.geofence.GeofenceManager
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.util.AlarmScheduler
import com.example.autoflow.util.Constants
import com.example.autoflow.viewmodel.WorkflowViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

// Note: You will also need imports for your 'fetchCurrentLocation' function,
// which is called but not defined in the provided snippet. For example:
// import com.google.android.gms.location.FusedLocationProviderClient

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
    val workflows: List<WorkflowEntity> by viewModel.workflows.observeAsState(emptyList())
    val existingWorkflow = remember(workflowId, workflows) {
        workflowId?.let { id -> workflows.find { it.id == id } }
    }

    //  Task name state with explicit types
    var taskName: String by remember { mutableStateOf("") }
    var taskNameError: String? by remember { mutableStateOf(null) }

    // : Trigger states with explicit types
    var locationTriggerExpanded: Boolean by remember { mutableStateOf(false) }
    var locationName: String by remember { mutableStateOf("") }
    var locationDetailsInput: String by remember { mutableStateOf("") }
    var radiusValue: Float by remember { mutableFloatStateOf(100f) }
    var triggerOnOption: String by remember { mutableStateOf("Entry") }

    var wifiTriggerExpanded: Boolean by remember { mutableStateOf(false) }
    var wifiState: String by remember { mutableStateOf("On") }

    var timeTriggerExpanded: Boolean by remember { mutableStateOf(false) }
    var timeValue: String by remember { mutableStateOf("") }

    var bluetoothDeviceTriggerExpanded: Boolean by remember { mutableStateOf(false) }
    var bluetoothDeviceAddress: String by remember { mutableStateOf("") }

    //   Action states with explicit types
    var sendNotificationActionExpanded: Boolean by remember { mutableStateOf(false) }
    var notificationTitle: String by remember { mutableStateOf("") }
    var notificationMessage: String by remember { mutableStateOf("") }
    var notificationPriority: String by remember { mutableStateOf("Normal") }

    var toggleSettingsActionExpanded: Boolean by remember { mutableStateOf(false) }
    var toggleSetting: String by remember { mutableStateOf("WiFi") }

    var runScriptActionExpanded: Boolean by remember { mutableStateOf(false) }
    var scriptText: String by remember { mutableStateOf("") }

    //  Error/success messages with explicit types
    var showErrorDialog: Boolean by remember { mutableStateOf(false) }
    var errorMessage: String by remember { mutableStateOf("") }
    var showSuccessSnackbar: Boolean by remember { mutableStateOf(false) }

    //  Sound Mode Action states with explicit types
    var setSoundModeActionExpanded: Boolean by remember { mutableStateOf(false) }
    var soundMode: String by remember { mutableStateOf("Normal") }

    //  Block Apps Action states with explicit types
    var blockAppsActionExpanded: Boolean by remember { mutableStateOf(false) }
    var selectedAppsToBlock: List<String> by remember { mutableStateOf(emptyList()) }


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
                            setSoundModeActionExpanded = setSoundModeActionExpanded,
                            soundMode = soundMode,
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

        // NO ANIMATION - Direct show/hide
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

        // NO ANIMATION - Direct show/hide
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


// Add to your LocationTriggerContent composable

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Location client
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Location states
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var currentAddress by remember { mutableStateOf<String?>(null) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }

    // Radius text field state
    var radiusText by remember { mutableStateOf(radiusValue.roundToInt().toString()) }
    var radiusError by remember { mutableStateOf<String?>(null) }

    // Update text when slider changes
    LaunchedEffect(radiusValue) {
        radiusText = radiusValue.roundToInt().toString()
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            fetchCurrentLocation(
                context = context,
                fusedLocationClient = fusedLocationClient,
                onLocationReceived = { lat, lng, address ->
                    onLocationDetailsChange("$lat,$lng")
                    currentAddress = address
                    if (locationName.isEmpty()) {
                        onLocationNameChange(address ?: "Current Location")
                    }
                    isLoadingLocation = false
                },
                onError = { error ->
                    locationError = error
                    isLoadingLocation = false
                }
            )
        } else {
            locationError = "Location permission denied"
            isLoadingLocation = false
            showLocationPermissionDialog = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Location Name Field
        OutlinedTextField(
            value = locationName,
            onValueChange = onLocationNameChange,
            label = { Text("Location Name") },
            placeholder = { Text("Home, Office, etc.") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Home, null) },
            supportingText = {
                if (currentAddress != null) {
                    Text(
                        "Detected: $currentAddress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        // Coordinates Field with Auto-Detect Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = locationDetailsInput,
                onValueChange = onLocationDetailsChange,
                label = { Text("Coordinates") },
                placeholder = { Text("37.7749,-122.4194") },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                isError = locationError != null,
                supportingText = locationError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                enabled = !isLoadingLocation
            )

            // Auto-Detect Location Button
            Button(
                onClick = {
                    locationError = null
                    isLoadingLocation = true

                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        fetchCurrentLocation(
                            context = context,
                            fusedLocationClient = fusedLocationClient,
                            onLocationReceived = { lat, lng, address ->
                                onLocationDetailsChange("$lat,$lng")
                                currentAddress = address
                                if (locationName.isEmpty()) {
                                    onLocationNameChange(address ?: "Current Location")
                                }
                                isLoadingLocation = false
                            },
                            onError = { error ->
                                locationError = error
                                isLoadingLocation = false
                            }
                        )
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = !isLoadingLocation,
                modifier = Modifier.height(56.dp)
            ) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.MyLocation, "Detect Location")
                }
            }
        }

        // Location Preview Card
        if (locationDetailsInput.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            "Location Set",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            locationDetailsInput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ========== UPDATED: Radius Input with Indoor Presets ==========

        Text(
            "Trigger Radius",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Text Input for precise value
            OutlinedTextField(
                value = radiusText,
                onValueChange = { newValue ->
                    radiusText = newValue
                    // Validate and update radius
                    val parsedValue = newValue.toIntOrNull()
                    when {
                        newValue.isEmpty() -> {
                            radiusError = null
                        }
                        parsedValue == null -> {
                            radiusError = "Enter a valid number"
                        }
                        parsedValue < 10 -> {
                            radiusError = "Minimum 10 meters"
                        }
                        parsedValue > 5000 -> {
                            radiusError = "Maximum 5000 meters"
                        }
                        else -> {
                            radiusError = null
                            onRadiusChange(parsedValue.toFloat())
                        }
                    }
                },
                label = { Text("Radius (meters)") },
                placeholder = { Text("100") },
                modifier = Modifier.width(140.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Validate on done
                        val parsedValue = radiusText.toIntOrNull()
                        if (parsedValue != null && parsedValue in 10..5000) {
                            onRadiusChange(parsedValue.toFloat())
                        }
                    }
                ),
                isError = radiusError != null,
                supportingText = radiusError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                trailingIcon = {
                    Text(
                        "m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true
            )

            // ✅ UPDATED: Quick preset buttons with indoor options
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Quick Presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ✅ NEW: Indoor/Small Space Presets Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        10 to "10m\nRoom",
                        20 to "20m\nOffice",
                        30 to "30m\nFloor"
                    ).forEach { (preset, label) ->
                        FilterChip(
                            selected = radiusValue.roundToInt() == preset,
                            onClick = {
                                onRadiusChange(preset.toFloat())
                                radiusText = preset.toString()
                                radiusError = null
                            },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 12.sp,
                                    fontSize = 10.sp
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Standard Presets Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(50, 100, 200).forEach { preset ->
                        FilterChip(
                            selected = radiusValue.roundToInt() == preset,
                            onClick = {
                                onRadiusChange(preset.toFloat())
                                radiusText = preset.toString()
                                radiusError = null
                            },
                            label = {
                                Text(
                                    "${preset}m",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Large Area Presets Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(500, 1000, 2000).forEach { preset ->
                        FilterChip(
                            selected = radiusValue.roundToInt() == preset,
                            onClick = {
                                onRadiusChange(preset.toFloat())
                                radiusText = preset.toString()
                                radiusError = null
                            },
                            label = {
                                Text(
                                    when {
                                        preset >= 1000 -> "${preset / 1000}km"
                                        else -> "${preset}m"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Fine-tuned Slider for visual adjustment
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Fine-tune: ${radiusValue.roundToInt()} meters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = radiusValue,
                onValueChange = { newValue ->
                    onRadiusChange(newValue)
                    radiusText = newValue.roundToInt().toString()
                    radiusError = null
                },
                valueRange = 10f..5000f,
                steps = 498, // More granular steps (10m increments)
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Range indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "10m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "1km",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "2.5km",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "5km",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Visual radius representation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        getRadiusDescription(radiusValue.roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "Recommended for ${getRadiusUseCase(radiusValue.roundToInt())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Trigger Options
        Text(
            "Trigger When:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("Entry", Icons.Default.Login, "Trigger when entering the location area"),
                Triple("Exit", Icons.Default.Logout, "Trigger when leaving the location area"),
                Triple("Both", Icons.Default.SwapHoriz, "Trigger on both entry and exit")
            ).forEach { (option, icon, description) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTriggerOptionChange(option) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (triggerOnOption == option)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = if (triggerOnOption == option)
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    else
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (triggerOnOption == option)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                option,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (triggerOnOption == option) FontWeight.Bold else FontWeight.Medium,
                                color = if (triggerOnOption == option)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (triggerOnOption == option) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

//: Helper functions with indoor support
private fun getRadiusDescription(radius: Int): String {
    return when {
        radius <= 10 -> "Tiny area - ${radius}m"
        radius <= 30 -> "Very small area - ${radius}m"
        radius <= 50 -> "Small area - ${radius}m"
        radius <= 150 -> "Medium area - ${radius}m"
        radius <= 500 -> "Large area - ${radius}m"
        radius <= 1000 -> "Very large area - ${radius}m"
        else -> "Massive area - ${radius / 1000}km"
    }
}

private fun getRadiusUseCase(radius: Int): String {
    return when {
        radius <= 10 -> "specific rooms (bedroom, bathroom)"
        radius <= 30 -> "offices, conference rooms, or single floors"
        radius <= 50 -> "small buildings or parking spots"
        radius <= 150 -> "homes, offices, or parking lots"
        radius <= 500 -> "neighborhoods or large buildings"
        radius <= 1000 -> "districts or large complexes"
        else -> "cities or wide areas"
    }
}

// Permission Dialog (same as before)...
// ========== LOCATION FETCHING FUNCTION ==========

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (latitude: Double, longitude: Double, address: String?) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Use getCurrentLocation for fresh, accurate location
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(listener: OnTokenCanceledListener) =
                    CancellationTokenSource().token

                override fun isCancellationRequested() = false
            }
        ).addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                // Get address from coordinates (Reverse Geocoding)
                val address = getAddressFromCoordinates(context, latitude, longitude)

                onLocationReceived(latitude, longitude, address)
            } else {
                // Fallback to last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        val address = getAddressFromCoordinates(
                            context,
                            lastLocation.latitude,
                            lastLocation.longitude
                        )
                        onLocationReceived(lastLocation.latitude, lastLocation.longitude, address)
                    } else {
                        onError("Unable to get location. Please enable GPS")
                    }
                }.addOnFailureListener { e ->
                    onError("Location error: ${e.message}")
                }
            }
        }.addOnFailureListener { e ->
            onError("Failed to get location: ${e.message}")
        }
    } catch (e: SecurityException) {
        onError("Location permission not granted")
    } catch (e: Exception) {
        onError("Error: ${e.message}")
    }
}

// ========== REVERSE GEOCODING ==========

private fun getAddressFromCoordinates(
    context: Context,
    latitude: Double,
    longitude: Double
): String? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            // Build readable address
            buildString {
                address.featureName?.let { append("$it, ") }
                address.locality?.let { append("$it, ") }
                address.adminArea?.let { append(it) }
            }.takeIf { it.isNotBlank() } ?: "Unknown Location"
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("Geocoding", "Error getting address", e)
        null
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

    // ✅ AUTOMATICALLY DETECTS USER'S SYSTEM PREFERENCE (12hr or 24hr)
    val context = LocalContext.current
    val is24HourFormat = android.text.format.DateFormat.is24HourFormat(context)

    val timePickerState = rememberTimePickerState(
        initialHour = LocalTime.now().hour,
        initialMinute = LocalTime.now().minute,
        is24Hour = is24HourFormat  // ← Auto-detects from system settings!
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Date picker button
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DateRange, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(selectedDate?.toString() ?: "Select Date")
        }

        // Time picker button
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Schedule, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                selectedTime?.let {
                    // ✅ Display in user's preferred format
                    if (is24HourFormat) {
                        String.format("%02d:%02d", it.hour, it.minute)
                    } else {
                        val hour12 = if (it.hour == 0) 12 else if (it.hour > 12) it.hour - 12 else it.hour
                        val amPm = if (it.hour < 12) "AM" else "PM"
                        String.format("%d:%02d %s", hour12, it.minute, amPm)
                    }
                } ?: "Select Time"
            )
        }

        // Show scheduled time
        if (selectedDate != null && selectedTime != null) {
            val dateTime = LocalDateTime.of(selectedDate, selectedTime)

            // ✅ CORRECT: Proper timezone conversion
            val timestamp = dateTime
                .atZone(java.time.ZoneId.systemDefault())
                .toEpochSecond()

            // Save the timestamp
            onTimeValueChange(timestamp.toString())

            // Log for debugging
            Log.d("TimeTriggerContent", "Is24Hour: $is24HourFormat")
            Log.d("TimeTriggerContent", "Selected DateTime: $dateTime")
            Log.d("TimeTriggerContent", "Timestamp (seconds): $timestamp")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Scheduled for:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        // ✅ Display in user's preferred format
                        if (is24HourFormat) {
                            dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        } else {
                            dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a"))
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Format: ${if (is24HourFormat) "24-hour" else "12-hour"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // ✅ TimePickerState.hour is ALWAYS in 24-hour format (0-23)
                    // No conversion needed - it handles 12hr/24hr automatically!
                    selectedTime = LocalTime.of(
                        timePickerState.hour,    // Always 0-23
                        timePickerState.minute
                    )
                    showTimePicker = false

                    Log.d("TimePicker", "Selected: ${timePickerState.hour}:${timePickerState.minute} (24hr format)")
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
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
    var showPermissionRationale by remember { mutableStateOf(false) }

    // ✅ Permission launcher for Bluetooth Connect (Android 12+)
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                pairedDevices = getPairedBluetoothDevices(context)
                showDevicePicker = true
            } catch (e: Exception) {
                Log.e("Bluetooth", "Error fetching devices", e)
            }
        } else {
            showPermissionRationale = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Select device button
        OutlinedButton(
            onClick = {
                // Check if permission is needed (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        try {
                            pairedDevices = getPairedBluetoothDevices(context)
                            showDevicePicker = true
                        } catch (e: Exception) {
                            Log.e("Bluetooth", "Error fetching devices", e)
                        }
                    } else {
                        // Request permission
                        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                } else {
                    // Android 11 and below - no BLUETOOTH_CONNECT needed
                    try {
                        pairedDevices = getPairedBluetoothDevices(context)
                        showDevicePicker = true
                    } catch (e: Exception) {
                        Log.e("Bluetooth", "Error fetching devices", e)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Bluetooth, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Paired Device")
        }

        // Manual address input
        OutlinedTextField(
            value = bluetoothDeviceAddress,
            onValueChange = onBluetoothAddressChange,
            label = { Text("Device MAC Address") },
            placeholder = { Text("XX:XX:XX:XX:XX:XX") },
            modifier = Modifier.fillMaxWidth()
        )

        // Show selected device
        if (bluetoothDeviceAddress.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            "Device Selected",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            bluetoothDeviceAddress,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // Device picker dialog
    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            confirmButton = {
                TextButton(onClick = { showDevicePicker = false }) {
                    Text("Cancel")
                }
            },
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
                                    Text(
                                        device.address,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Permission rationale dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Bluetooth Permission Required") },
            text = {
                Text("AutoFlow needs Bluetooth permission to scan for paired devices. This allows you to select a device as a trigger.")
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
    setSoundModeActionExpanded: Boolean,
    soundMode: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        Log.d("TaskCreation", "🔵 handleSaveTask started")

        // 1. VALIDATE TASK NAME
        if (taskName.isBlank()) {
            Log.e("TaskCreation", "❌ Task name is blank")
            onError("Task name cannot be empty")
            return
        }
        Log.d("TaskCreation", "✅ Task name valid: $taskName")

        // 2. CHECK WHICH TRIGGERS ARE CONFIGURED
        val hasLocationTrigger = locationTriggerExpanded && locationDetailsInput.isNotBlank()
        val hasWifiTrigger = wifiTriggerExpanded
        val hasTimeTrigger = timeTriggerExpanded && timeValue.isNotBlank()
        val hasBluetoothTrigger = bluetoothDeviceTriggerExpanded && bluetoothDeviceAddress.isNotBlank()

        Log.d("TaskCreation", "=== TRIGGER CHECK ===")
        Log.d("TaskCreation", "Location: $hasLocationTrigger")
        Log.d("TaskCreation", "WiFi: $hasWifiTrigger")
        Log.d("TaskCreation", "Time: $hasTimeTrigger")
        Log.d("TaskCreation", "Bluetooth: $hasBluetoothTrigger")

        // 3. VALIDATE AT LEAST ONE TRIGGER
        if (!hasLocationTrigger && !hasWifiTrigger && !hasTimeTrigger && !hasBluetoothTrigger) {
            Log.e("TaskCreation", "❌ No trigger configured")
            onError("Please configure at least ONE trigger")
            return
        }
        Log.d("TaskCreation", "✅ At least one trigger is configured")

        // 4. CREATE TRIGGER (Priority: Time > WiFi > Bluetooth > Location)
        val trigger = when {
            hasTimeTrigger -> {
                Log.d("TaskCreation", "→ Creating TIME trigger: $timeValue")
                Trigger(0, 0, Constants.TRIGGER_TIME, timeValue)
            }
            hasWifiTrigger -> {
                Log.d("TaskCreation", "→ Creating WIFI trigger: $wifiState")
                Trigger(0, 0, Constants.TRIGGER_WIFI, wifiState)
            }
            hasBluetoothTrigger -> {
                Log.d("TaskCreation", "→ Creating BLUETOOTH trigger: $bluetoothDeviceAddress")
                Trigger(0, 0, Constants.TRIGGER_BLE, bluetoothDeviceAddress)
            }
            hasLocationTrigger -> {
                Log.d("TaskCreation", "→ Creating LOCATION trigger")
                val parts = locationDetailsInput.split(",").map { it.trim() }
                if (parts.size != 2) {
                    onError("Invalid coordinates. Use format: latitude,longitude")
                    return
                }
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                if (lat == null || lng == null) {
                    onError("Invalid coordinate values")
                    return
                }
                val json = JSONObject().apply {
                    put("locationName", locationName.ifEmpty { "Unnamed Location" })
                    put("coordinates", locationDetailsInput)
                    put("latitude", lat)
                    put("longitude", lng)
                    put("radius", radiusValue.roundToInt())
                    put("triggerOnEntry", triggerOnOption == "Entry" || triggerOnOption == "Both")
                    put("triggerOnExit", triggerOnOption == "Exit" || triggerOnOption == "Both")

                    //  For GeofenceReceiver compatibility
                    put("triggerOn", when (triggerOnOption) {
                        "Entry" -> "enter"
                        "Exit" -> "exit"
                        "Both" -> "both"
                        else -> "enter"
                    })
                }
                Trigger(0, 0, Constants.TRIGGER_LOCATION, json.toString())
            }
            else -> {
                onError("Failed to create trigger")
                return
            }
        }
        Log.d("TaskCreation", "✅ Trigger created: ${trigger.type}")

        // 5. CHECK WHICH ACTIONS ARE CONFIGURED
        val hasNotificationAction = sendNotificationActionExpanded && notificationTitle.isNotBlank()
        val hasToggleAction = toggleSettingsActionExpanded
        val hasScriptAction = runScriptActionExpanded && scriptText.isNotBlank()
        val hasSoundModeAction = setSoundModeActionExpanded  // ✅ FIXED

        Log.d("TaskCreation", "=== ACTION CHECK ===")
        Log.d("TaskCreation", "Notification: $hasNotificationAction")
        Log.d("TaskCreation", "Toggle: $hasToggleAction")
        Log.d("TaskCreation", "Script: $hasScriptAction")
        Log.d("TaskCreation", "Sound Mode: $hasSoundModeAction")

        // 6. VALIDATE AT LEAST ONE ACTION
        if (!hasNotificationAction && !hasToggleAction && !hasScriptAction && !hasSoundModeAction) {
            Log.e("TaskCreation", "❌ No action configured")
            onError("Please configure at least ONE action")
            return
        }
        Log.d("TaskCreation", "✅ At least one action is configured")

        // 7. CREATE ACTION (Priority: Notification > Toggle > SoundMode > Script)
        val action = when {
            hasNotificationAction -> {
                Log.d("TaskCreation", "→ Creating NOTIFICATION action")
                Action(
                    Constants.ACTION_SEND_NOTIFICATION,
                    notificationTitle,
                    notificationMessage,
                    notificationPriority
                )
            }
            hasToggleAction -> {
                Log.d("TaskCreation", "→ Creating TOGGLE action: $toggleSetting")
                val actionType = when {
                    toggleSetting.startsWith("WIFI") -> Constants.ACTION_TOGGLE_WIFI
                    toggleSetting.startsWith("BLUETOOTH") -> Constants.ACTION_TOGGLE_BLUETOOTH
                    else -> Constants.ACTION_TOGGLE_WIFI // default fallback
                }

                Action(actionType, null, null, null).apply {
                    value = toggleSetting
                }
            }
            hasSoundModeAction -> {
                Log.d("TaskCreation", "→ Creating SOUND MODE action: $soundMode")
                Action(Constants.ACTION_SET_SOUND_MODE, null, null, null).apply {
                    value = soundMode
                }
            }
            hasScriptAction -> {
                Log.d("TaskCreation", "→ Creating SCRIPT action")
                Action(Constants.ACTION_RUN_SCRIPT, null, null, null).apply {
                    value = scriptText
                }
            }
            else -> {
                onError("Failed to create action")
                return
            }
        }
        Log.d("TaskCreation", "✅ Action created: ${action.type}")

        // 8. SAVE TO DATABASE
        Log.d("TaskCreation", "💾 Saving workflow...")

        if (workflowId != null) {
            // UPDATE existing
            Log.d("TaskCreation", "→ Updating workflow ID: $workflowId")
            viewModel.updateWorkflow(workflowId, taskName, trigger, action, null)
            Log.d("TaskCreation", "✅ Update sent")
        } else {
            // CREATE new
            Log.d("TaskCreation", "→ Creating new workflow")
            viewModel.addWorkflow(taskName, trigger, action)
            Log.d("TaskCreation", "✅ Create sent")
        }

        // 9. SCHEDULE ALARM IF TIME TRIGGER
        if (trigger.type == Constants.TRIGGER_TIME) {
            try {
                val triggerTimeSeconds = timeValue.toLongOrNull() ?: 0L
                val triggerTimeMillis = triggerTimeSeconds * 1000L
                val currentTimeMillis = System.currentTimeMillis()

                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                Log.d("TaskCreation", "⏰ Time Check:")
                Log.d("TaskCreation", "   Current:  ${dateTimeFormat.format(currentTimeMillis)}")
                Log.d("TaskCreation", "   Selected: ${dateTimeFormat.format(triggerTimeMillis)}")

                if (triggerTimeMillis > currentTimeMillis) {
                    // ✅ FIXED: Route based on action type
                    when (action.type) {
                        Constants.ACTION_SEND_NOTIFICATION -> {
                            AlarmScheduler.scheduleNotification(
                                context,
                                0L,
                                triggerTimeMillis,
                                action.title ?: "AutoFlow",
                                action.message ?: "Triggered"
                            )
                        }
                        Constants.ACTION_SET_SOUND_MODE -> {
                            AlarmScheduler.scheduleSoundMode(
                                context,
                                0L,
                                triggerTimeMillis,
                                action.value ?: "Silent"
                            )
                        }
                        Constants.ACTION_TOGGLE_WIFI -> {
                            val wifiState = action.value == "WIFI_ON"  // ✅ Parse state
                            AlarmScheduler.scheduleWiFiToggle(
                                context,
                                0L,
                                triggerTimeMillis,
                                wifiState
                            )
                        }
                        Constants.ACTION_TOGGLE_BLUETOOTH -> {  // ✅ ADD THIS
                            val bluetoothState = action.value == "BLUETOOTH_ON"  // ✅ Parse state
                            AlarmScheduler.scheduleBluetoothToggle(
                                context,
                                0L,
                                triggerTimeMillis,
                                bluetoothState
                            )
                        }
                        Constants.ACTION_RUN_SCRIPT -> {
                            AlarmScheduler.scheduleScript(
                                context,
                                0L,
                                triggerTimeMillis,
                                action.value ?: ""
                            )
                        }
                    }
                    Log.d("TaskCreation", "✅ Alarm scheduled for ${action.type}")
                } else {
                    Log.w("TaskCreation", "⚠️ Time is in the past")
                }
            } catch (e: Exception) {
                Log.e("TaskCreation", "❌ Alarm scheduling failed", e)
            }
        }
        // 10. ADD GEOFENCE IF LOCATION TRIGGER
        if (trigger.type == Constants.TRIGGER_LOCATION) {
            try {
                val locationJson = JSONObject(trigger.value)
                val lat = locationJson.getDouble("latitude")
                val lng = locationJson.getDouble("longitude")
                val radius = locationJson.optInt("radius", 100).toFloat()

                //  Use the correct field names that match your JSON
                val triggerOnEntry = locationJson.optBoolean("triggerOnEntry", false)
                val triggerOnExit = locationJson.optBoolean("triggerOnExit", false)

                //  Store triggerOn for GeofenceReceiver compatibility
                val triggerOn = when {
                    triggerOnEntry && triggerOnExit -> "both"
                    triggerOnEntry -> "enter"
                    triggerOnExit -> "exit"
                    else -> "enter" // default
                }

                val geofenceAdded = GeofenceManager.addGeofence(
                    context = context,
                    workflowId = workflowId ?: 0L,
                    latitude = lat,
                    longitude = lng,
                    radius = radius,
                    triggerOnEntry = triggerOnEntry,
                    triggerOnExit = triggerOnExit
                )

                if (geofenceAdded) {
                    Log.d("TaskCreation", "✅ Geofence registered for workflow")
                } else {
                    Log.e("TaskCreation", "❌ Failed to register geofence")
                }
            } catch (e: Exception) {
                Log.e("TaskCreation", "❌ Geofence setup failed", e)
            }
        }




        // 11. SUCCESS
        onSuccess()

    } catch (e: NumberFormatException) {
        Log.e("TaskCreation", "❌ Number format error", e)
        onError("Invalid number: ${e.message}")
    } catch (e: JSONException) {
        Log.e("TaskCreation", "❌ JSON error", e)
        onError("Location data error: ${e.message}")
    } catch (e: Exception) {
        Log.e("TaskCreation", "❌ Unexpected error", e)
        onError("Error: ${e.message}")
    }
}
// ========== UTILITY FUNCTIONS ==========

data class BluetoothDeviceInfo(val name: String, val address: String)

@Composable
private fun ToggleSettingsActionContent(
    toggleSetting: String,
    onToggleSettingChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Select Setting to Toggle",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // WiFi Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (toggleSetting.startsWith("WIFI"))
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("WiFi", fontWeight = FontWeight.Bold)
                        Text("Toggle WiFi connection", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.Wifi, contentDescription = null)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // WiFi ON/OFF buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = toggleSetting == "WIFI_ON",
                        onClick = { onToggleSettingChange("WIFI_ON") },
                        label = { Text("Turn ON") },
                        leadingIcon = if (toggleSetting == "WIFI_ON") {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = toggleSetting == "WIFI_OFF",
                        onClick = { onToggleSettingChange("WIFI_OFF") },
                        label = { Text("Turn OFF") },
                        leadingIcon = if (toggleSetting == "WIFI_OFF") {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }

        // Bluetooth Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (toggleSetting.startsWith("BLUETOOTH"))
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bluetooth", fontWeight = FontWeight.Bold)
                        Text("Toggle Bluetooth connection", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bluetooth ON/OFF buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = toggleSetting == "BLUETOOTH_ON",
                        onClick = { onToggleSettingChange("BLUETOOTH_ON") },
                        label = { Text("Turn ON") },
                        leadingIcon = if (toggleSetting == "BLUETOOTH_ON") {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = toggleSetting == "BLUETOOTH_OFF",
                        onClick = { onToggleSettingChange("BLUETOOTH_OFF") },
                        label = { Text("Turn OFF") },
                        leadingIcon = if (toggleSetting == "BLUETOOTH_OFF") {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }

        // Show selection summary
        if (toggleSetting.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        when (toggleSetting) {
                            "WIFI_ON" -> "Will turn WiFi ON"
                            "WIFI_OFF" -> "Will turn WiFi OFF"
                            "BLUETOOTH_ON" -> "Will turn Bluetooth ON"
                            "BLUETOOTH_OFF" -> "Will turn Bluetooth OFF"
                            else -> "Select an option"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}



/**
 * Get paired Bluetooth devices with proper permission handling
 */
@SuppressLint("MissingPermission")
fun getPairedBluetoothDevices(context: Context): List<BluetoothDeviceInfo> {
    return try {
        // Check for BLUETOOTH_CONNECT permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.e("Bluetooth", "❌ BLUETOOTH_CONNECT permission not granted")
                return emptyList()
            }
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        when {
            adapter == null -> {
                Log.e("Bluetooth", "❌ Bluetooth adapter not available")
                emptyList()
            }
            !adapter.isEnabled -> {
                Log.w("Bluetooth", "⚠️ Bluetooth is disabled")
                emptyList()
            }
            else -> {
                adapter.bondedDevices?.map {
                    BluetoothDeviceInfo(
                        name = it.name ?: "Unknown",
                        address = it.address
                    )
                } ?: emptyList()
            }
        }
    } catch (e: SecurityException) {
        Log.e("Bluetooth", "❌ Security exception: ${e.message}", e)
        emptyList()
    } catch (e: Exception) {
        Log.e("Bluetooth", "❌ Error getting devices: ${e.message}", e)
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

@Preview(showBackground = true, name = "Task Creation Screen Preview")
@Composable
fun TaskCreationScreenPreview() {
    AutoFlowTheme {
        TaskCreationScreen(
            onBack = {},
            onSaveTask = {}
        )
    }
}
