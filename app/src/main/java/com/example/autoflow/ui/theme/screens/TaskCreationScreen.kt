package com.example.autoflow.ui.theme.screens

import android.R.attr.action
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.model.Action
import com.example.autoflow.model.Trigger
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.util.Constants
import com.example.autoflow.viewmodel.WorkflowViewModel
import kotlin.math.roundToInt

@Composable
fun TaskCreationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSaveTask: (taskName: String) -> Unit,
    viewModel: WorkflowViewModel = viewModel()
) {
    var taskName by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

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
                                            "Location" -> locationTriggerExpanded =
                                                !locationTriggerExpanded

                                            "WiFi" -> wifiTriggerExpanded = !wifiTriggerExpanded
                                            "Time" -> timeTriggerExpanded = !timeTriggerExpanded
                                            "Bluetooth Device" -> bluetoothDeviceTriggerExpanded =
                                                !bluetoothDeviceTriggerExpanded
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
                            if (expandedState) {
                                Spacer(modifier = Modifier.height(8.dp))
                                when (triggerName) {
                                    "Location" -> {
                                        TextField(
                                            value = locationName,
                                            onValueChange = { locationName = it },
                                            label = { Text("Location Name") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextField(
                                            value = locationDetailsInput,
                                            onValueChange = { locationDetailsInput = it },
                                            label = { Text("Coordinates (e.g., lat,lng)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Radius: ${radiusValue.roundToInt()}m")
                                        Slider(
                                            value = radiusValue,
                                            onValueChange = { radiusValue = it },
                                            valueRange = 50f..500f,
                                            steps = ((500f - 50f) / 10f - 1).toInt(),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
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
                                                        .clickable { triggerOnOption = option }
                                                        .padding(
                                                            end = 16.dp,
                                                            top = 8.dp,
                                                            bottom = 8.dp
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = (triggerOnOption == option),
                                                        onClick = { triggerOnOption = option }
                                                    )
                                                    Text(option)
                                                }
                                            }
                                        }
                                    }
                                    "WiFi" -> {
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
                                    "Time" -> {
                                        TextField(
                                            value = timeValue,
                                            onValueChange = { timeValue = it },
                                            label = { Text("Time (Unix Timestamp ms)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    "Bluetooth Device" -> {
                                        TextField(
                                            value = bluetoothDeviceAddress,
                                            onValueChange = { bluetoothDeviceAddress = it },
                                            label = { Text("Device Address") },
                                            modifier = Modifier.fillMaxWidth()
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
                                        val currentIndex =
                                            priorityOptions.indexOf(notificationPriority)
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Run Script Action
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
                            TextField(
                                value = scriptText,
                                onValueChange = { scriptText = it },
                                label = { Text("Script Code") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        // Card 4: Save/Back Buttons
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (taskName.isNotBlank()) {
                            val trigger = when {
                                locationTriggerExpanded && locationDetailsInput.isNotBlank() -> {
                                    Trigger(0, 0, Constants.TRIGGER_LOCATION, "{\"locationName\":\"$locationName\",\"coordinates\":\"$locationDetailsInput\",\"radius\":${radiusValue.roundToInt()},\"triggerOnEntry\":${triggerOnOption == "Entry" || triggerOnOption == "Both"},\"triggerOnExit\":${triggerOnOption == "Exit" || triggerOnOption == "Both"}}")
                                }
                                wifiTriggerExpanded -> Trigger(0, 0, Constants.TRIGGER_BLE, wifiState) // Placeholder: Map to BLE for now
                                timeTriggerExpanded && timeValue.isNotBlank() -> Trigger(0, 0, Constants.TRIGGER_TIME, timeValue)
                                bluetoothDeviceTriggerExpanded && bluetoothDeviceAddress.isNotBlank() -> Trigger(0, 0, Constants.TRIGGER_BLE, bluetoothDeviceAddress)
                                else -> null
                            }
                            val action = when {
                                sendNotificationActionExpanded -> Action(Constants.ACTION_SEND_NOTIFICATION, notificationTitle, notificationMessage, notificationPriority)
                                toggleSettingsActionExpanded -> Action(Constants.ACTION_TOGGLE_WIFI, null, null, "Normal").apply { setValue(toggleSetting) } // Assuming value for TOGGLE_WIFI is set directly via state like toggleSetting
                                runScriptActionExpanded && scriptText.isNotBlank() -> Action("RUN_SCRIPT").apply { setValue(scriptText) }
                                else -> null
                            }
                            // Assuming Trigger has an isValid() method
                            if (trigger != null && action != null /* && trigger.isValid() */) {
                                viewModel.addWorkflow(trigger, action) // Call your ViewModel function
                                onSaveTask(taskName)
                            } /*else {
                                // Handle cases where trigger or action is null or trigger is invalid
                                // Maybe show a Toast or a Snackbar
                            }*/
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Task")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}
@Preview(showBackground = true, name = "Task Creation Screen Preview")
@Composable
fun TaskCreationScreenPreview() {
    AutoFlowTheme { // Apply your app's theme for a consistent look
        TaskCreationScreen(
            onBack = { println("Preview: Back clicked") },
            onSaveTask = { taskName -> println("Preview: Save Task clicked with name: $taskName") }
            // ViewModel will use its default from viewModel() composable,
            // which might be a real ViewModel instance if your DI is set up for previews,
            // or a basic one if not. For preview purposes, this is usually fine.
        )
    }
}