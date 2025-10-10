package com.example.autoflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Data Models for Mocking UI ---
data class AutomationFlow(
    val id: Int,
    val name: String,
    val description: String,
    val icon: ImageVector,
    var isEnabled: Boolean
)

data class AutomationTrigger(
    val id: Int, val type: String, val summary: String, val icon: ImageVector, val color: Color
)

data class AutomationAction(
    val id: Int, val type: String, val summary: String, val icon: ImageVector, val color: Color
)

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Outlined.Home)
    object Create : Screen("create", "Create", Icons.Outlined.AddCircle)
    object Profile : Screen("profile", "Profile", Icons.Outlined.AccountCircle)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
}

// --- Main App Entry Point ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoFlowApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val mockFlows = remember {
        mutableStateListOf(
            AutomationFlow(1, "Work Focus", "At work, silence phone & open Slack", Icons.Default.Work, true),
            AutomationFlow(2, "Good Morning", "At 7 AM, turn off Do Not Disturb", Icons.Default.WbSunny, true),
            AutomationFlow(3, "Bedtime Routine", "After 11 PM, enable DND & lower brightness", Icons.Default.Bedtime, true),
            AutomationFlow(4, "Left Home", "If I leave home, turn off WiFi", Icons.Default.MeetingRoom, false),
            AutomationFlow(5, "Driving Mode", "Bluetooth connects to car, launch Maps", Icons.Default.DirectionsCar, true)
        )
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigation(
                currentScreen = currentScreen,
                onScreenSelected = { screen -> currentScreen = screen }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    flows = mockFlows,
                    onFlowToggled = { flow, isEnabled ->
                        val index = mockFlows.indexOf(flow)
                        if (index != -1) {
                            mockFlows[index] = flow.copy(isEnabled = isEnabled)
                        }
                    },
                    onCreateFlowClicked = { currentScreen = Screen.Create }
                )
                Screen.Create -> CreateFlowScreen(onBack = { currentScreen = Screen.Home })
                Screen.Profile -> PlaceholderScreen("Profile")
                Screen.Settings -> PlaceholderScreen("Settings")
            }
        }
    }
}

// --- Screen Composable: Home Dashboard ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    flows: List<AutomationFlow>,
    onFlowToggled: (AutomationFlow, Boolean) -> Unit,
    onCreateFlowClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateFlowClicked) {
                Icon(Icons.Default.Add, contentDescription = "Create new flow")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    "Your Flows (${flows.count { it.isEnabled }}/${flows.size} active)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp, top=8.dp)
                )
            }
            items(flows) { flow ->
                FlowItemCard(
                    flow = flow,
                    onToggle = { isEnabled -> onFlowToggled(flow, isEnabled) }
                )
            }
        }
    }
}

// --- Screen Composable: Create Flow ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFlowScreen(onBack: () -> Unit) {
    // This screen is largely the same as the previous example, structured for navigation
    var flowName by remember { mutableStateOf("") }
    val triggers = remember { mutableStateListOf<AutomationTrigger>() }
    val actions = remember { mutableStateListOf<AutomationAction>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Flow", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(onClick = { /* Handle save */ onBack() }) {
                        Text("SAVE")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                OutlinedTextField(
                    value = flowName,
                    onValueChange = { flowName = it },
                    label = { Text("Flow Name (e.g., 'Work Mode')") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                AutomationSection(
                    title = "Triggers",
                    subtitle = "IF ALL of these are met...",
                    items = triggers,
                    onAddItem = {
                        triggers.add(
                            AutomationTrigger(
                                (triggers.size + 1), "Location", "Entering 200m radius of 'Work'",
                                Icons.Default.LocationOn, Color(0xFF4CAF50)
                            )
                        )
                    },
                    itemContent = { trigger ->
                        AutomationItemCard(
                            icon = trigger.icon, title = trigger.type, summary = trigger.summary,
                            iconColor = trigger.color, onDelete = { triggers.remove(trigger) }
                        )
                    }
                )
            }
            item {
                AutomationSection(
                    title = "Actions",
                    subtitle = "THEN do these actions...",
                    items = actions,
                    onAddItem = {
                        actions.add(
                            AutomationAction(
                                (actions.size + 1), "Sound Profile", "Set ringer mode to Vibrate",
                                Icons.Default.Vibration, Color(0xFF9C27B0)
                            )
                        )
                    },
                    itemContent = { action ->
                        AutomationItemCard(
                            icon = action.icon, title = action.type, summary = action.summary,
                            iconColor = action.color, onDelete = { actions.remove(action) }
                        )
                    }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// --- UI Components ---

@Composable
fun FlowItemCard(flow: AutomationFlow, onToggle: (Boolean) -> Unit) {
    var isEnabled by remember { mutableStateOf(flow.isEnabled) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = flow.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(flow.name, fontWeight = FontWeight.Bold)
                Text(flow.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    isEnabled = it
                    onToggle(it)
                }
            )
        }
    }
}

@Composable
fun AppBottomNavigation(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val items = listOf(Screen.Home, Screen.Create, Screen.Profile, Screen.Settings)
    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) }
            )
        }
    }
}

@Composable
fun <T> AutomationSection(
    title: String, subtitle: String, items: List<T>, onAddItem: () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                AnimatedVisibility(
                    visible = true, // In a real app, this would be animated on add/remove
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    itemContent(item)
                }
            }
            Button(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = "Add", Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add ${title.dropLast(1)}")
            }
        }
    }
}

@Composable
fun AutomationItemCard(
    icon: ImageVector, title: String, summary: String, iconColor: Color, onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(screenTitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "$screenTitle Screen\n(Coming Soon)",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

// --- Preview ---
@Preview(showBackground = true, widthDp = 380, heightDp = 800)
@Composable
fun AutoFlowAppPreview() {
    MaterialTheme {
        AutoFlowApp()
    }
}
