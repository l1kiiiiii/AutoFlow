package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector // Ensure this import is present
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val navItemList = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("CreateTask", Icons.Default.Create),
        NavItem("Profile", Icons.Default.Person),
        NavItem("Settings", Icons.Default.Settings)
    )
    var selectedIndex by remember {
        mutableIntStateOf(0)
    }

    // Navigation lambdas
    val onNavigateHome = { selectedIndex = 0 }
    val onSaveTaskAndNavigateHome = { taskName: String ->
        println("Task saved: $taskName") // Placeholder for actual save logic
        selectedIndex = 0
    }
    val onNavigateToCreateTask = { selectedIndex = 1 }
    val onNavigateToProfile = { selectedIndex = 2 }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = "Tasker") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { /* TODO: Handle notification icon click */ }) {
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
                navItemList.forEachIndexed { index, navItem ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (navItem.badgeCount != 0)
                                        Badge {
                                            Text(text = navItem.badgeCount.toString())
                                        }
                                }
                            ) { 
                                Icon(imageVector = navItem.icon, contentDescription = "Icon")
                            }
                        },
                        label = { Text(text = navItem.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        ContentScreen(
            modifier = Modifier.padding(innerPadding),
            selectedIndex = selectedIndex,
            onNavigateHome = onNavigateHome,
            onSaveTaskAndNavigateHome = onSaveTaskAndNavigateHome,
            onNavigateToCreateTask = onNavigateToCreateTask,
            onNavigateToProfile = onNavigateToProfile
        )
    }
}

@Composable
fun ContentScreen(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onNavigateHome: () -> Unit, 
    onSaveTaskAndNavigateHome: (String) -> Unit,
    onNavigateToCreateTask: () -> Unit, // New callback
    onNavigateToProfile: () -> Unit      // New callback
) {
    when (selectedIndex) {
        0 -> HomeScreen(
            modifier = modifier,
            onNavigateToCreateTask = onNavigateToCreateTask, // Pass down
            onNavigateToProfile = onNavigateToProfile      // Pass down
        )
        1 -> TaskCreationScreen(
            modifier = modifier,
            onBack = onNavigateHome, 
            onSaveTask = onSaveTaskAndNavigateHome 
        )
        2 -> ProfileManagment(modifier = modifier)
        3 -> Settings(modifier = modifier)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    Dashboard()
}
