package com.example.autoflow.ui.theme.screens

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
import com.example.autoflow.ui.theme.AutoFlowTheme
import kotlin.math.roundToInt

@Composable
fun TaskCreationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSaveTask: (taskName: String) -> Unit
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
    var cellularNetworkTriggerExpanded by remember { mutableStateOf(false) }
    var timeTriggerExpanded by remember { mutableStateOf(false) }
    var bluetoothDeviceTriggerExpanded by remember { mutableStateOf(false) }
    var sensorsTriggerExpanded by remember { mutableStateOf(false) }

    // State for Send Notification Action
    var sendNotificationActionExpanded by remember { mutableStateOf(false) }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationPriority by remember { mutableStateOf("Normal") }

    Column(
        modifier = modifier
            .padding(16.dp) // Outer padding for the whole screen
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Space between cards
    ) {

        // --- Card 1: Task Name ---
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

        // --- Card 2: Configure Triggers ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Configure Triggers", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    val triggerOptions = listOf(
                        "Location", "WiFi", "Cellular Network",
                        "Time", "Bluetooth Device", "Sensors"
                    )
                    triggerOptions.forEach { triggerName ->
                        when (triggerName) {
                            "Location" -> {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { locationTriggerExpanded = !locationTriggerExpanded }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(triggerName, style = MaterialTheme.typography.bodyLarge)
                                        Icon(
                                            imageVector = if (locationTriggerExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                            contentDescription = if (locationTriggerExpanded) "Collapse" else "Expand"
                                        )
                                    }
                                    if (locationTriggerExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
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
                                            label = { Text("Coordinates/Address (e.g., Pick on map)") },
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
                                                        .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = (triggerOnOption == option),
                                                        onClick = { triggerOnOption = option }
                                                    )
                                                    Text(
                                                        text = option,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.padding(start = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                var expandedState by remember { mutableStateOf(false) }
                                when (triggerName) {
                                    "WiFi" -> expandedState = wifiTriggerExpanded
                                    "Cellular Network" -> expandedState = cellularNetworkTriggerExpanded
                                    "Time" -> expandedState = timeTriggerExpanded
                                    "Bluetooth Device" -> expandedState = bluetoothDeviceTriggerExpanded
                                    "Sensors" -> expandedState = sensorsTriggerExpanded
                                }
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                when (triggerName) {
                                                    "WiFi" -> wifiTriggerExpanded = !wifiTriggerExpanded
                                                    "Cellular Network" -> cellularNetworkTriggerExpanded = !cellularNetworkTriggerExpanded
                                                    "Time" -> timeTriggerExpanded = !timeTriggerExpanded
                                                    "Bluetooth Device" -> bluetoothDeviceTriggerExpanded = !bluetoothDeviceTriggerExpanded
                                                    "Sensors" -> sensorsTriggerExpanded = !sensorsTriggerExpanded
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
                                        Text("$triggerName Configuration Details Here", modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp)) // Spacer after each trigger item
                    }
                }
            }
        }

        // --- Card 3: Define Actions ---
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
                                .clickable { sendNotificationActionExpanded = !sendNotificationActionExpanded }
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { /* TODO: Handle Toggle Settings action configuration */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Toggle Settings")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { /* TODO: Handle Run Script action configuration */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Run Script")
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // Spacer after last action item
                }
            }
        }

        // --- Card 4: Save/Back Buttons ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (taskName.isNotBlank()) {
                            onSaveTask(taskName)
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

@Preview(showBackground = true)
@Composable
fun TaskCreationScreenPreview() {
    AutoFlowTheme {
        TaskCreationScreen(onBack = {}, onSaveTask = { taskName -> println("Preview Save: $taskName") })
    }
}
