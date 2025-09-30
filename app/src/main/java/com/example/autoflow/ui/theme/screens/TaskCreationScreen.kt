package com.example.autoflow.ui.theme.screens

import android.R.attr.action
import com.example.autoflow.util.AlarmScheduler
import androidx.compose.ui.platform.LocalContext
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.util.Constants
import com.example.autoflow.util.LocationState
import com.example.autoflow.util.TimeUtils
import com.example.autoflow.util.refreshLocation
import com.example.autoflow.util.rememberLocationState
import com.example.autoflow.viewmodel.WorkflowViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.jar.Manifest
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSaveTask: (taskName: String) -> Unit,
    viewModel: WorkflowViewModel = viewModel()
) {
    var taskName by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val context = LocalContext.current // Added this line

    // States for Triggers
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

    // State for Send Notification Action
    var sendNotificationActionExpanded by remember { mutableStateOf(false) }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationPriority by remember { mutableStateOf("Normal") }

    // State for Toggle Settings Action
    var toggleSettingsActionExpanded by remember { mutableStateOf(false) }
    var toggleSetting by remember { mutableStateOf("WiFi") }

    // State for Run Script Action
    var runScriptActionExpanded by remember { mutableStateOf(false) }
    var scriptText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Task Name
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create New Task", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Card 2: Configure Triggers
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Configure Triggers", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    val triggerOptions = listOf(
                        "Location", "WiFi", "Time", "Bluetooth Device"
                    )

                    triggerOptions.forEach { triggerName ->
                        var expandedState by remember { mutableStateOf(false) }
                        when (triggerName) {
                            "Location" -> expandedState = locationTriggerExpanded
                            "WiFi" -> expandedState = wifiTriggerExpanded
                            "Time" -> expandedState = timeTriggerExpanded
                            "Bluetooth Device" -> expandedState = bluetoothDeviceTriggerExpanded
                        }

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (triggerName) {
                                            "Location" -> locationTriggerExpanded = !locationTriggerExpanded
                                            "WiFi" -> wifiTriggerExpanded = !wifiTriggerExpanded
                                            "Time" -> timeTriggerExpanded = !timeTriggerExpanded
                                            "Bluetooth Device" -> bluetoothDeviceTriggerExpanded = !bluetoothDeviceTriggerExpanded
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(triggerName, style = MaterialTheme.typography.bodyLarge)
                                Icon(
                                    imageVector = if (expandedState) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = if (expandedState) "Collapse" else "Expand"
                                )
                            }

                            when (triggerName) {
                                "Location" -> {
                                    LocationTriggerSection(
                                        expanded = locationTriggerExpanded,
                                        locationName = locationName,
                                        onLocationNameChange = { locationName = it },
                                        locationDetailsInput = locationDetailsInput,
                                        onLocationDetailsChange = { locationDetailsInput = it },
                                        radiusValue = radiusValue,
                                        onRadiusChange = { radiusValue = it },
                                        triggerOnOption = triggerOnOption,
                                        onTriggerOptionChange = { triggerOnOption = it }
                                    )
                                }

                                "WiFi" -> {
                                    if (wifiTriggerExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Target State:")
                                            val wifiStates = listOf("On", "Off")
                                            wifiStates.forEach { state ->
                                                Row(
                                                    Modifier.clickable { wifiState = state },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = (wifiState == state),
                                                        onClick = { wifiState = state }
                                                    )
                                                    Text(state)
                                                }
                                            }
                                        }
                                    }
                                }

                                "Time" -> {
                                    TimeTriggerSection(
                                        expanded = timeTriggerExpanded,
                                        onTimeValueChange = { timeValue = it }
                                    )
                                }

                                "Bluetooth Device" -> {
                                    if (bluetoothDeviceTriggerExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val context = LocalContext.current
                                        var pairedDevices by remember { mutableStateOf<List<BluetoothDeviceInfo>>(emptyList()) }
                                        var showDevicePicker by remember { mutableStateOf(false) }

                                        // Fetch paired devices button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    pairedDevices = getPairedBluetoothDevices(context)
                                                    showDevicePicker = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bluetooth,
                                                    contentDescription = "Get Paired Devices"
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Get Paired Devices")
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Manual entry
                                        TextField(
                                            value = bluetoothDeviceAddress,
                                            onValueChange = { bluetoothDeviceAddress = it },
                                            label = { Text("Device Address (MAC)") },
                                            placeholder = { Text("XX:XX:XX:XX:XX:XX") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Device picker dialog
                                        if (showDevicePicker && pairedDevices.isNotEmpty()) {
                                            AlertDialog(
                                                onDismissRequest = { showDevicePicker = false },
                                                title = { Text("Select Bluetooth Device") },
                                                text = {
                                                    LazyColumn {
                                                        items(pairedDevices) { device ->
                                                            Card(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 4.dp)
                                                                    .clickable {
                                                                        bluetoothDeviceAddress = device.address
                                                                        showDevicePicker = false
                                                                    }
                                                            ) {
                                                                Column(modifier = Modifier.padding(12.dp)) {
                                                                    Text(
                                                                        text = device.name,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                    Text(
                                                                        text = device.address,
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        color = MaterialTheme.colorScheme.secondary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                confirmButton = {
                                                    TextButton(onClick = { showDevicePicker = false }) {
                                                        Text("Cancel")
                                                    }
                                                }
                                            )
                                        }

                                        // Show message if no devices found
                                        if (showDevicePicker && pairedDevices.isEmpty()) {
                                            AlertDialog(
                                                onDismissRequest = { showDevicePicker = false },
                                                title = { Text("No Paired Devices") },
                                                text = { Text("No Bluetooth devices are currently paired with this phone.") },
                                                confirmButton = {
                                                    TextButton(onClick = { showDevicePicker = false }) {
                                                        Text("OK")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Card 3: Define Actions
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Define Actions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Send Notification Action
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sendNotificationActionExpanded = !sendNotificationActionExpanded
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Send Notification", style = MaterialTheme.typography.bodyLarge)
                            Icon(
                                imageVector = if (sendNotificationActionExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = if (sendNotificationActionExpanded) "Collapse" else "Expand"
                            )
                        }

                        if (sendNotificationActionExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = notificationTitle,
                                onValueChange = { notificationTitle = it },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = notificationMessage,
                                onValueChange = { notificationMessage = it },
                                label = { Text("Message") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            val priorityOptions = listOf("Low", "Normal", "High")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val currentIndex = priorityOptions.indexOf(notificationPriority)
                                        val nextIndex = (currentIndex + 1) % priorityOptions.size
                                        notificationPriority = priorityOptions[nextIndex]
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Priority: $notificationPriority",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Change Priority"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Toggle Settings Action
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    toggleSettingsActionExpanded = !toggleSettingsActionExpanded
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Toggle Settings", style = MaterialTheme.typography.bodyLarge)
                            Icon(
                                imageVector = if (toggleSettingsActionExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = if (toggleSettingsActionExpanded) "Collapse" else "Expand"
                            )
                        }

                        if (toggleSettingsActionExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Setting:")
                                val settings = listOf("WiFi", "Bluetooth")
                                settings.forEach { setting ->
                                    Row(
                                        Modifier.clickable { toggleSetting = setting },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (toggleSetting == setting),
                                            onClick = { toggleSetting = setting }
                                        )
                                        Text(setting)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Run Script Action (Enhanced) - Fixed version
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { runScriptActionExpanded = !runScriptActionExpanded }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Run Script", style = MaterialTheme.typography.bodyLarge)
                            Icon(
                                imageVector = if (runScriptActionExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = if (runScriptActionExpanded) "Collapse" else "Expand"
                            )
                        }

                        if (runScriptActionExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Script templates
                            var selectedTemplate by remember { mutableStateOf("Custom") }
                            val templates = mapOf(
                                "Custom" to "",
                                "Log Message" to "log('Hello from AutoFlow!');\nlog('Current time: ' + new Date());",
                                "Send Notification" to "notify('AutoFlow Alert', 'Script executed successfully!');\nlog('Notification sent');",
                                "HTTP Request" to "var response = httpGet('https://api.example.com/data');\nlog('Response: ' + response);\nnotify('API Call', 'Request completed');",
                                "File Operation" to "// File operations (requires permissions)\nlog('Performing file operations...');\n// Add your file handling code here"
                            )

                            // Template selector
                            Text("Script Templates:", style = MaterialTheme.typography.titleSmall)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                items(templates.keys.toList()) { template ->
                                    FilterChip(
                                        selected = selectedTemplate == template,
                                        onClick = {
                                            selectedTemplate = template
                                            scriptText = templates[template] ?: ""
                                        },
                                        label = { Text(template) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Script editor
                            TextField(
                                value = scriptText,
                                onValueChange = { scriptText = it },
                                label = { Text("JavaScript Code") },
                                placeholder = { Text("Enter your JavaScript code here...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                maxLines = 10,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Script validation and test - FIXED SECTION
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Test script execution - now properly defined
                                        testScriptExecution(scriptText, context)
                                    }
                                ) {
                                    Text("Test Script")
                                }

                                Button(
                                    onClick = {
                                        // Validate script syntax - now properly defined
                                        validateScript(scriptText)
                                    }
                                ) {
                                    Text("Validate")
                                }
                            }

                            // Help text
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Available Functions:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("• log(message) - Write to console", style = MaterialTheme.typography.bodySmall)
                                    Text("• notify(title, message) - Send notification", style = MaterialTheme.typography.bodySmall)
                                    Text("• httpGet(url) - Make HTTP request", style = MaterialTheme.typography.bodySmall)
                                    Text("• androidContext - Access Android context", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Card 4: Save/Back Buttons
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        Log.d("TaskCreation", "Save button clicked")

                        if (taskName.isNotBlank()) {
                            Log.d("TaskCreation", "Task name is valid: $taskName")

                            // Create trigger based on expanded sections
                            val trigger = when {
                                locationTriggerExpanded && locationDetailsInput.isNotBlank() -> {
                                    Log.d("TaskCreation", "Creating location trigger")
                                    Trigger(
                                        0, // id
                                        0, // workflowId
                                        Constants.TRIGGER_LOCATION, // type
                                        "{\"locationName\":\"$locationName\",\"coordinates\":\"$locationDetailsInput\",\"radius\":${radiusValue.roundToInt()},\"triggerOnEntry\":${triggerOnOption == "Entry" || triggerOnOption == "Both"},\"triggerOnExit\":${triggerOnOption == "Exit" || triggerOnOption == "Both"}}" // value
                                    )
                                }
                                wifiTriggerExpanded -> {
                                    Log.d("TaskCreation", "Creating WiFi trigger")
                                    Trigger(0, 0, Constants.TRIGGER_WIFI, wifiState)
                                }
                                timeTriggerExpanded && timeValue.isNotBlank() -> {
                                    Log.d("TaskCreation", "Creating time trigger")
                                    Trigger(0, 0, Constants.TRIGGER_TIME, timeValue)
                                }
                                bluetoothDeviceTriggerExpanded && bluetoothDeviceAddress.isNotBlank() -> {
                                    Log.d("TaskCreation", "Creating Bluetooth trigger")
                                    Trigger(0, 0, Constants.TRIGGER_BLE, bluetoothDeviceAddress)
                                }
                                else -> {
                                    Log.w("TaskCreation", "No trigger selected or configured")
                                    null
                                }
                            }

                            // Create action based on expanded sections
                            val action = when {
                                sendNotificationActionExpanded && notificationTitle.isNotBlank() -> {
                                    Log.d("TaskCreation", "Creating notification action")
                                    Action(
                                        Constants.ACTION_SEND_NOTIFICATION, // type
                                        notificationTitle, // title
                                        notificationMessage, // message
                                        notificationPriority // priority
                                    )
                                }
                                toggleSettingsActionExpanded -> {
                                    Log.d("TaskCreation", "Creating toggle settings action")
                                    val toggleAction = Action(Constants.ACTION_TOGGLE_WIFI, null, null, "Normal")
                                    toggleAction.setValue(toggleSetting)
                                    toggleAction
                                }
                                runScriptActionExpanded && scriptText.isNotBlank() -> {
                                    Log.d("TaskCreation", "Creating script action")
                                    val scriptAction = Action("RUN_SCRIPT", null, null, null)
                                    scriptAction.setValue(scriptText)
                                    scriptAction
                                }
                                else -> {
                                    Log.w("TaskCreation", "No action selected or configured")
                                    null
                                }
                            }

                            // Validate and save
                            if (trigger != null && action != null) {
                                Log.d("TaskCreation", "Both trigger and action are valid, saving to database...")

                                viewModel.addWorkflow(trigger, action, object : WorkflowViewModel.WorkflowOperationCallback {
                                    override fun onSuccess(message: String) {
                                        Log.d("TaskCreation", "Task saved successfully: $message")

                                        // IMPORTANT: Schedule alarm if it's a time trigger
                                        if (trigger.type == Constants.TRIGGER_TIME && timeValue.isNotBlank()) {
                                            try {
                                                val triggerTimeMillis = timeValue.toLong()
                                                val notificationTitle = if (action.type == Constants.ACTION_SEND_NOTIFICATION) {
                                                    action.title ?: "AutoFlow Alert"
                                                } else {
                                                    "AutoFlow Alert"
                                                }
                                                val notificationMessage = if (action.type == Constants.ACTION_SEND_NOTIFICATION) {
                                                    action.message ?: "Time trigger activated"
                                                } else {
                                                    "Time trigger activated"
                                                }

                                                // Extract workflow ID from success message
                                                val workflowId = message.substringAfterLast(":").trim().toLongOrNull() ?: 0

                                                AlarmScheduler.scheduleNotification(
                                                    context,
                                                    workflowId,
                                                    triggerTimeMillis,
                                                    notificationTitle,
                                                    notificationMessage
                                                )

                                                Log.d("TaskCreation", "Alarm scheduled for time: $triggerTimeMillis")
                                            } catch (e: Exception) {
                                                Log.e("TaskCreation", "Failed to schedule alarm: ${e.message}")
                                            }
                                        }

                                        onSaveTask(taskName)
                                    }

                                    override fun onError(error: String) {
                                        Log.e("TaskCreation", "Failed to save task: $error")
                                    }
                                })
                            }

                            else {
                                Log.w("TaskCreation", "Cannot save - Trigger: $trigger, Action: $action")
                                // TODO: Show validation error to user
                                when {
                                    trigger == null -> Log.w("TaskCreation", "No trigger configured")
                                    action == null -> Log.w("TaskCreation", "No action configured")
                                }
                            }
                        } else {
                            Log.w("TaskCreation", "Task name is blank")
                            // TODO: Show "Please enter task name" message
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Task")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        Log.d("TaskCreation", "Back button clicked")
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }

    }
}

//  MISSING FUNCTIONS:

/**
 * Test script execution function
 */
private fun testScriptExecution(scriptCode: String, context: android.content.Context) {
    Log.i("ScriptTest", "Testing script: $scriptCode")

    // Basic validation
    if (scriptCode.trim().isEmpty()) {
        Log.w("ScriptTest", "Empty script provided")
        return
    }

    // Simple syntax checks
    val basicChecks = listOf(
        "log(" to "log function usage detected",
        "notify(" to "notify function usage detected",
        "httpGet(" to "httpGet function usage detected"
    )

    basicChecks.forEach { (pattern, message) ->
        if (scriptCode.contains(pattern)) {
            Log.i("ScriptTest", message)
        }
    }

    Log.i("ScriptTest", "Script test completed")

    // TODO: Implement actual script execution with ScriptExecutor when ready
    // val scriptExecutor = ScriptExecutor(context)
    // val result = scriptExecutor.executeScript(scriptCode)
}

/**
 * Validate script syntax function
 */
private fun validateScript(scriptCode: String) {
    Log.i("ScriptValidation", "Validating script syntax")

    if (scriptCode.trim().isEmpty()) {
        Log.w("ScriptValidation", "Empty script - validation failed")
        return
    }

    // Basic JavaScript syntax validation
    val errors = mutableListOf<String>()

    // Check for balanced parentheses
    var parenthesesCount = 0
    var braceCount = 0

    for (char in scriptCode) {
        when (char) {
            '(' -> parenthesesCount++
            ')' -> parenthesesCount--
            '{' -> braceCount++
            '}' -> braceCount--
        }
    }

    if (parenthesesCount != 0) {
        errors.add("Unbalanced parentheses")
    }

    if (braceCount != 0) {
        errors.add("Unbalanced braces")
    }

    // Check for common syntax issues
    if (scriptCode.contains("function") && !scriptCode.contains("{")) {
        errors.add("Function declaration missing opening brace")
    }

    if (errors.isEmpty()) {
        Log.i("ScriptValidation", "Script validation passed")
    } else {
        Log.w("ScriptValidation", "Script validation errors: ${errors.joinToString(", ")}")
    }
}

// Enhanced Location Trigger Section with real-time location
@Composable
fun LocationTriggerSection(
    expanded: Boolean,
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    locationDetailsInput: String,
    onLocationDetailsChange: (String) -> Unit,
    radiusValue: Float,
    onRadiusChange: (Float) -> Unit,
    triggerOnOption: String,
    onTriggerOptionChange: (String) -> Unit
) {
    if (expanded) {
        Spacer(modifier = Modifier.height(8.dp))

        // Location name input
        TextField(
            value = locationName,
            onValueChange = onLocationNameChange,
            label = { Text("Location Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Current location vs manual entry toggle
        var useCurrentLocation by remember { mutableStateOf(false) }
        val locationState = rememberLocationState()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use Current Location")
            Switch(
                checked = useCurrentLocation,
                onCheckedChange = {
                    useCurrentLocation = it
                    if (it && locationState.location != null) {
                        // Auto-fill coordinates when toggled on
                        val location = locationState.location!!
                        onLocationDetailsChange("${location.latitude},${location.longitude}")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (useCurrentLocation) {
            // Current location section
            CurrentLocationSection(
                locationState = locationState,
                onLocationReceived = { location ->
                    onLocationDetailsChange("${location.latitude},${location.longitude}")
                }
            )
        } else {
            // Manual coordinate entry
            TextField(
                value = locationDetailsInput,
                onValueChange = onLocationDetailsChange,
                label = { Text("Coordinates (e.g., lat,lng)") },
                placeholder = { Text("37.4220,-122.0840") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location"
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Radius slider
        Text("Radius: ${radiusValue.roundToInt()}m")
        Slider(
            value = radiusValue,
            onValueChange = onRadiusChange,
            valueRange = 50f..500f,
            steps = ((500f - 50f) / 10f - 1).toInt(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger options
        Text("Trigger On:")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.Start
        ) {
            val radioOptions = listOf("Entry", "Exit", "Both")
            radioOptions.forEach { option ->
                Row(
                    Modifier
                        .clickable { onTriggerOptionChange(option) }
                        .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (triggerOnOption == option),
                        onClick = { onTriggerOptionChange(option) }
                    )
                    Text(option)
                }
            }
        }
    }
}

@Composable
fun CurrentLocationSection(
    locationState: LocationState,
    onLocationReceived: (android.location.Location) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            when {
                !locationState.hasPermission -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Permission",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Location permission required",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                locationState.isLoading || isRefreshing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Getting your location...")
                    }
                }

                locationState.error != null -> {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = locationState.error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                isRefreshing = true
                                // Use the helper function for refresh
                                refreshLocation(context, scope) { result ->
                                    isRefreshing = false
                                    // Note: In a real implementation, you'd need to properly
                                    // propagate this result back to the parent state
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }

                locationState.location != null -> {
                    val location = locationState.location
                    onLocationReceived(location)

                    Column {
                        Text(
                            text = "Current Location:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latitude: ${"%.6f".format(location.latitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Longitude: ${"%.6f".format(location.longitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (location.hasAccuracy()) {
                            Text(
                                text = "Accuracy: ±${location.accuracy.roundToInt()}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Show location age
                        val locationAge = (System.currentTimeMillis() - location.time) / 1000
                        Text(
                            text = "Updated: ${locationAge}s ago",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                isRefreshing = true
                                refreshLocation(context, scope) { result ->
                                    isRefreshing = false
                                    // Handle result update
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Location")
                        }
                    }
                }

                else -> {
                    Text("Initializing location services...")
                }
            }
        }
    }
}



// Enhanced Time Trigger Section with Date/Time Pickers
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTriggerSection(
    expanded: Boolean,
    onTimeValueChange: (String) -> Unit
) {
    if (expanded) {
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }
        var showQuickOptions by remember { mutableStateOf(true) }

        val datePickerState = rememberDatePickerState()
        val timePickerState = rememberTimePickerState()

        Spacer(modifier = Modifier.height(8.dp))

        // Quick time options toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quick Options")
            Switch(
                checked = showQuickOptions,
                onCheckedChange = { showQuickOptions = it }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showQuickOptions) {
            // Quick time options
            QuickTimeOptionsSection(
                onTimeSelected = { minutes ->
                    val futureTime = LocalDateTime.now().plusMinutes(minutes.toLong())
                    val timestamp = TimeUtils.dateTimeToUnixTimestamp(futureTime)
                    onTimeValueChange(timestamp.toString())
                    selectedDate = futureTime.toLocalDate()
                    selectedTime = futureTime.toLocalTime()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OR",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxSize(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Custom date and time selection
        Text(
            text = if (showQuickOptions) "Set Custom Date & Time:" else "Set Date & Time:",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Date selection
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select Date"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedDate?.let { TimeUtils.formatDate(it) } ?: "Select Date"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time selection
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Select Time"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedTime?.let { TimeUtils.formatTime(it) } ?: "Select Time"
            )
        }

        // Display selected datetime and validation
        if (selectedDate != null && selectedTime != null) {
            val selectedDateTime = LocalDateTime.of(selectedDate, selectedTime)
            val isFuture = TimeUtils.isFutureTime(selectedDateTime)

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFuture)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Scheduled for:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = TimeUtils.formatDateTime(selectedDateTime),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isFuture)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )

                    if (!isFuture) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚠️ Selected time is in the past",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Convert to Unix timestamp when both date and time are selected
            if (isFuture) {
                val timestamp = TimeUtils.dateAndTimeToUnixTimestamp(selectedDate!!, selectedTime!!)
                onTimeValueChange(timestamp.toString())
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            }
                            showDatePicker = false
                        }
                    ) {
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

        // Time Picker Dialog
        if (showTimePicker) {
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                onConfirm = {
                    selectedTime = LocalTime.of(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@Composable
fun QuickTimeOptionsSection(
    onTimeSelected: (Int) -> Unit
) {
    Text(
        text = "Quick Time Options:",
        style = MaterialTheme.typography.titleSmall
    )

    Spacer(modifier = Modifier.height(8.dp))

    val quickOptions = listOf(
        "5 min" to 5,
        "15 min" to 15,
        "30 min" to 30,
        "1 hour" to 60,
        "2 hours" to 120,
        "1 day" to 1440
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(120.dp)
    ) {
        items(quickOptions) { (label, minutes) ->
            OutlinedButton(
                onClick = { onTimeSelected(minutes) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = { content() }
    )
}
// Data class for Bluetooth device info
data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)

// Function to get paired Bluetooth devices
@SuppressLint("MissingPermission")
fun getPairedBluetoothDevices(context: Context): List<BluetoothDeviceInfo> {
    val deviceList = mutableListOf<BluetoothDeviceInfo>()

    try {
        // Check for Bluetooth permissions
        if (!hasBluetoothPermissions(context)) {
            Log.w("Bluetooth", "Missing Bluetooth permissions")
            return emptyList()
        }

        // Get Bluetooth adapter
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Log.w("Bluetooth", "Bluetooth adapter not available")
            return emptyList()
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.w("Bluetooth", "Bluetooth is not enabled")
            return emptyList()
        }

        // Get bonded (paired) devices
        val pairedDevices = bluetoothAdapter.bondedDevices

        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address // MAC address

                deviceList.add(BluetoothDeviceInfo(deviceName, deviceAddress))
                Log.d("Bluetooth", "Found paired device: $deviceName at $deviceAddress")
            }
        } else {
            Log.d("Bluetooth", "No paired devices found")
        }

    } catch (e: SecurityException) {
        Log.e("Bluetooth", "Permission error: ${e.message}")
    } catch (e: Exception) {
        Log.e("Bluetooth", "Error getting paired devices: ${e.message}")
    }

    return deviceList
}

// Check if app has Bluetooth permissions
fun hasBluetoothPermissions(context: Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        // Android 12+
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        // Android 11 and below
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.BLUETOOTH
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}



@Preview(showBackground = true, name = "Task Creation Screen Preview")
@Composable
fun TaskCreationScreenPreview() {
    AutoFlowTheme {
        TaskCreationScreen(
            onBack = { println("Preview: Back clicked") },
            onSaveTask = { taskName -> println("Preview: Save Task clicked with name: $taskName") }
        )
    }
}
