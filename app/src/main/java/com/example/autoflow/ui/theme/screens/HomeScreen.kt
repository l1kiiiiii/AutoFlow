package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreateTask: () -> Unit, // New callback
    onNavigateToProfile: () -> Unit      // New callback
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = "Active Profiles", style = MaterialTheme.typography.headlineSmall)
        // Placeholder for Active Profiles content
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Recent Executions", style = MaterialTheme.typography.headlineSmall)
        // Placeholder for Recent Executions content
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Quick Actions", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp)) // Small spacer before buttons
        Button(
            onClick = onNavigateToCreateTask, // Updated onClick
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New Task")
        }
        Spacer(modifier = Modifier.height(8.dp)) // Spacer between buttons
        Button(
            onClick = onNavigateToProfile, // Updated onClick
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Profile")
        }

        // To make scrolling more apparent for testing, you can add more spacers or content:
        /*
        Spacer(modifier = Modifier.height(300.dp))
        Text(text = "More Content to enable scrolling", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(300.dp))
        Text(text = "Even More Content", style = MaterialTheme.typography.bodyLarge)
        */
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        onNavigateToCreateTask = { println("Preview: Navigate to Create Task") },
        onNavigateToProfile = { println("Preview: Navigate to Profile") }
    )
}
