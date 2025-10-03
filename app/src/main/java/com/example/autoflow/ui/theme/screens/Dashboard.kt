package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.autoflow.ui.screens.TaskCreationScreen
import kotlinx.coroutines.launch

/**
 * Production-ready Dashboard with:
 * - Proper navigation state management
 * - Error handling for navigation
 * - Memory leak prevention
 * - Bottom navigation with Material Design 3
 * - Top app bar with notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Track navigation errors
    var navigationError by remember { mutableStateOf<String?>(null) }

    // Bottom navigation items
    val bottomNavItems = remember {
        listOf(
            BottomNavItem(
                route = "home",
                label = "Home",
                icon = Icons.Default.Home,
                selectedIcon = Icons.Filled.Home
            ),
            BottomNavItem(
                route = "create_task",
                label = "Create",
                icon = Icons.Default.Add,
                selectedIcon = Icons.Filled.Add
            ),
            BottomNavItem(
                route = "profile",
                label = "Profile",
                icon = Icons.Default.Person,
                selectedIcon = Icons.Filled.Person
            ),
            BottomNavItem(
                route = "settings",
                label = "Settings",
                icon = Icons.Default.Settings,
                selectedIcon = Icons.Filled.Settings
            )
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = getScreenTitle(currentDestination?.route ?: "home"),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Notifications button
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "No new notifications",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    ) {
                        BadgedBox(
                            badge = {
                                // Show badge only if there are notifications
                                val notificationCount = 0 // TODO: Connect to notification system
                                if (notificationCount > 0) {
                                    Badge {
                                        Text(
                                            text = if (notificationCount > 9) "9+" else notificationCount.toString()
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentDestination?.route?.startsWith(item.route) == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = {
                            try {
                                if (currentDestination?.route != item.route) {
                                    navController.navigate(item.route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            } catch (e: Exception) {
                                navigationError = "Navigation failed: ${e.message}"
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Navigation error occurred",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home screen
            composable("home") {
                HomeScreen(
                    onNavigateToCreateTask = {
                        try {
                            navController.navigate("create_task") {
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Unable to navigate to create task")
                            }
                        }
                    },
                    onNavigateToEditTask = { workflowId ->
                        try {
                            navController.navigate("edit_task/$workflowId") {
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Unable to navigate to edit task")
                            }
                        }
                    },
                    onNavigateToProfile = {
                        try {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Unable to navigate to profile")
                            }
                        }
                    }
                )
            }

            // Create task screen
            composable("create_task") {
                TaskCreationScreen(
                    workflowId = null,
                    onBack = {
                        try {
                            if (!navController.popBackStack()) {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Unable to go back")
                            }
                        }
                    },
                    onSaveTask = { taskName ->
                        scope.launch {
                            try {
                                snackbarHostState.showSnackbar(
                                    message = "Task '$taskName' saved successfully!",
                                    duration = SnackbarDuration.Short
                                )
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error saving task")
                            }
                        }
                    }
                )
            }

            // Edit task screen with workflow ID parameter
            composable(
                route = "edit_task/{workflowId}",
                arguments = listOf(
                    navArgument("workflowId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val workflowId = try {
                    backStackEntry.arguments?.getLong("workflowId") ?: 0L
                } catch (e: Exception) {
                    0L
                }

                if (workflowId == 0L) {
                    // Invalid workflow ID, navigate back
                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar("Invalid workflow ID")
                        navController.popBackStack()
                    }
                } else {
                    TaskCreationScreen(
                        workflowId = workflowId,
                        onBack = {
                            try {
                                if (!navController.popBackStack()) {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Unable to go back")
                                }
                            }
                        },
                        onSaveTask = { taskName ->
                            scope.launch {
                                try {
                                    snackbarHostState.showSnackbar(
                                        message = "Task '$taskName' updated successfully!",
                                        duration = SnackbarDuration.Short
                                    )
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error updating task")
                                }
                            }
                        }
                    )
                }
            }

            // Profile screen - NO onBack parameter
            composable("profile") {
                ProfileManagment()
            }

            // Settings screen - NO onBack parameter
            composable("settings") {
                Settings()
            }
        }
    }
}

// ========== DATA CLASSES ==========

/**
 * Data class for bottom navigation items
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badgeCount: Int? = null
)

// ========== HELPER FUNCTIONS ==========

/**
 * Get screen title based on current route
 */
private fun getScreenTitle(route: String): String {
    return when {
        route.startsWith("home") -> "AutoFlow"
        route.startsWith("create_task") -> "Create Task"
        route.startsWith("edit_task") -> "Edit Task"
        route.startsWith("profile") -> "Profile"
        route.startsWith("settings") -> "Settings"
        else -> "AutoFlow"
    }
}
