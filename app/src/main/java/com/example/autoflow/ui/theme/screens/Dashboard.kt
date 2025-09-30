package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "AutoFlow") },
                actions = {
                    IconButton(onClick = { /* TODO: Handle notification */ }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                // Home
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = currentDestination?.route == "home",
                    onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )

                // Create Task
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, "Create") },
                    label = { Text("Create") },
                    selected = currentDestination?.route == "create_task",
                    onClick = {
                        navController.navigate("create_task") {
                            launchSingleTop = true
                        }
                    }
                )

                // Profile
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profile") },
                    label = { Text("Profile") },
                    selected = currentDestination?.route == "profile",
                    onClick = {
                        navController.navigate("profile") {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                )

                // Settings
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = currentDestination?.route == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                )
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
                        navController.navigate("create_task")
                    },
                    onNavigateToEditTask = { workflowId ->
                        navController.navigate("edit_task/$workflowId")
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile")
                    }
                )
            }

            // Create task screen
            composable("create_task") {
                TaskCreationScreen(
                    workflowId = null,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSaveTask = { taskName ->
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            }

            // Edit task screen
            composable(
                route = "edit_task/{workflowId}",
                arguments = listOf(navArgument("workflowId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workflowId = backStackEntry.arguments?.getLong("workflowId") ?: 0L
                TaskCreationScreen(
                    workflowId = workflowId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onSaveTask = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
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
}
