package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
fun TaskCreation(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSaveTask: (taskName: String) -> Unit // Modified to accept a task name
) {
    val scrollState = rememberScrollState()
    var taskName by remember { mutableStateOf("") }
    var taskDetails by remember { mutableStateOf("") }

        Column(modifier = modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
        ) {
        Text("Create New Task")
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = taskName,
            onValueChange = { taskName = it },
            label = { Text("Task Name") },
            modifier = Modifier.fillMaxWidth()
        )
            Text("Configure Triggres")
            Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = taskDetails,
            onValueChange = { taskDetails = it },
            label = { Text("Select triggres") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSaveTask(taskName) }) {
            Text("Save Task")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskCreationPreview() {
    TaskCreation(onBack = {}, onSaveTask = {})
}
