package com.example.autoflow.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.policy.BlockPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ✅ Enhanced BlockActivity - Shows when apps are blocked by workflows
 */
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedPackage = intent.getStringExtra("package_name") ?: "Unknown App"
        val appName = intent.getStringExtra("app_name") ?: blockedPackage
        val workflowName = intent.getStringExtra("workflow_name") ?: "Workflow"

        setContent {
            AutoFlowTheme {
                BlockScreen(
                    appName = appName,
                    workflowName = workflowName,
                    onEmergencyUnblock = {
                        // Unblock all apps and notify
                        BlockPolicy.clearBlockedPackages(this)
                        finish()
                    },
                    onBack = {
                        // Just go back to launcher
                        finish()
                    }
                )
            }
        }
    }
}

/**
 * ✅ Enhanced BlockScreen with emergency unblock and better UX
 */
@Composable
fun BlockScreen(
    appName: String,
    workflowName: String = "Workflow",
    onEmergencyUnblock: () -> Unit,
    onBack: () -> Unit
) {
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var isLongPressing by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Block icon
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = "Blocked",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main title
            Text(
                text = "App Blocked",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App name
            Text(
                text = appName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Workflow name
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "Blocked by: $workflowName",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Motivational message
            Text(
                text = "This app is blocked by your workflow rules.\n\nStay focused! 🎯\nYour goals are more important.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Go Back button
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Go Back", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tips for staying focused
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💡 Focus Tips",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Take a deep breath\n• Think about your goals\n• Try a productive activity instead",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Emergency Unblock Button (Bottom Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showEmergencyDialog = true },
                containerColor = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                scope.launch {
                                    isLongPressing = true
                                    // Animate progress over 3 seconds
                                    for (i in 0..100) {
                                        if (!isLongPressing) break
                                        pressProgress = i / 100f
                                        delay(30) // 3 seconds total
                                    }

                                    if (isLongPressing && pressProgress >= 1f) {
                                        onEmergencyUnblock()
                                    }

                                    isLongPressing = false
                                    pressProgress = 0f
                                }
                            },
                            onPress = {
                                val press = tryAwaitRelease()
                                if (!press) {
                                    isLongPressing = false
                                    pressProgress = 0f
                                }
                            }
                        )
                    }
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Emergency Unblock",
                    tint = Color.White
                )
            }

            // Progress indicator during long press
            if (isLongPressing) {
                CircularProgressIndicator(
                    progress = pressProgress,
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.Center),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            }
        }

        // Emergency unblock hint text
        Text(
            text = "Emergency? Long-press 🚨",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        // Emergency confirmation dialog
        if (showEmergencyDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("⚠️ Emergency Unblock") },
                text = {
                    Text(
                        "This will unblock ALL apps immediately.\n\n" +
                                "Use only in true emergencies. Your workflow will remain active but app blocking will be disabled.\n\n" +
                                "Are you sure you need emergency access?",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEmergencyDialog = false
                            onEmergencyUnblock()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("🚨 Unblock Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencyDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
