package com.example.autoflow.ui.theme.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.autoflow.ui.screens.TaskCreationScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced Dashboard with smooth navigation and success feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Track navigation errors and success
    var navigationError by remember { mutableStateOf<String?>(null) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

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
                                val notificationCount = 0
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
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
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
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOut)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = EaseInOut)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOut)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = EaseInOut)
                ) + fadeOut(animationSpec = tween(300))
            }
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
                                // Show success message
                                successMessage = "Task '$taskName' created successfully!"
                                showSuccessAnimation = true

                                // Show snackbar with action
                                val result = snackbarHostState.showSnackbar(
                                    message = "✓ Task '$taskName' created!",
                                    actionLabel = "View",
                                    duration = SnackbarDuration.Short,
                                    withDismissAction = true
                                )

                                // Small delay for UX smoothness
                                delay(300)

                                // Navigate to home and clear back stack
                                navController.navigate("home") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }

                                showSuccessAnimation = false

                                // If user clicked "View", highlight the new task
                                if (result == SnackbarResult.ActionPerformed) {
                                    Log.d("Dashboard", "User wants to view the task")
                                }
                            } catch (e: Exception) {
                                Log.e("Dashboard", "Error after saving task", e)
                                snackbarHostState.showSnackbar("Error saving task: ${e.message}")
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
                                    // Show success message for update
                                    successMessage = "Task '$taskName' updated!"
                                    showSuccessAnimation = true

                                    snackbarHostState.showSnackbar(
                                        message = "✓ Task '$taskName' updated successfully!",
                                        duration = SnackbarDuration.Short,
                                        withDismissAction = true
                                    )

                                    delay(300)

                                    // Navigate back to home
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }

                                    showSuccessAnimation = false
                                } catch (e: Exception) {
                                    Log.e("Dashboard", "Error after updating task", e)
                                    snackbarHostState.showSnackbar("Error updating task")
                                }
                            }
                        }
                    )
                }
            }

            // Profile screen
            composable("profile") {
                ProfileManagment()
            }

            // Settings screen
            composable("settings") {
                Settings()
            }
        }
    }

    // Success animation overlay
    if (showSuccessAnimation) {
        SuccessAnimationOverlay(message = successMessage)
    }
}

// ========== SUCCESS ANIMATION OVERLAY ==========

@Composable
fun SuccessAnimationOverlay(message: String) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(1500)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = scaleOut(targetScale = 0.8f) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ========== DATA CLASSES ==========

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badgeCount: Int? = null
)

// ========== HELPER FUNCTIONS ==========

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
