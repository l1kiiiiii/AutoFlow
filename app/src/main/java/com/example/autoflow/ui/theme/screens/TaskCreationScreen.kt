package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TaskCreationScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSaveTask: (taskName: String) -> Unit
) {
    var taskName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth() // Added for better layout
    ) {
        Text("Create New Task", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = taskName,
            onValueChange = { taskName = it },
            label = { Text("Task Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (taskName.isNotBlank()) { // Simple validation
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

@Preview(showBackground = true)
@Composable
fun TaskCreationScreenPreview() {
    // Provide dummy actions for the preview
    TaskCreationScreen(onBack = {}, onSaveTask = { taskName -> println("Preview Save: $taskName") })
}
