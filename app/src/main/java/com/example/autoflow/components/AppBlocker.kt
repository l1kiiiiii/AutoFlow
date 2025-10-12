package com.example.autoflow.ui.components

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data class representing an installed app
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean = false
)

/**
 * Composable function to fetch and display installed apps
 * @param onAppsSelected Callback when apps are selected
 * @param preSelectedApps List of already selected app package names
 * @param showSystemApps Whether to show system apps (default: false)
 */
@Composable
fun AppSelectorComposable(
    onAppsSelected: (List<String>) -> Unit,
    preSelectedApps: List<String> = emptyList(),
    showSystemApps: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf(preSelectedApps) }

    // Load apps on composition
    LaunchedEffect(showSystemApps) {
        isLoading = true
        error = null

        scope.launch {
            try {
                val apps = fetchInstalledApps(context, showSystemApps)
                installedApps = apps
            } catch (e: Exception) {
                error = "Failed to load apps: ${e.message}"
                Log.e("AppSelector", "Error loading apps", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Filter apps based on search query
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isEmpty()) {
            installedApps
        } else {
            installedApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with count
        Text(
            text = "Select Apps (${selectedApps.size} selected)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading apps...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Error state
        error?.let { errorMsg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMsg,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Apps list
        if (!isLoading && error == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(filteredApps) { app ->
                        AppListItem(
                            app = app,
                            isSelected = app.packageName in selectedApps,
                            onToggle = { isSelected ->
                                selectedApps = if (isSelected) {
                                    selectedApps + app.packageName
                                } else {
                                    selectedApps - app.packageName
                                }
                                onAppsSelected(selectedApps)
                            }
                        )

                        if (app != filteredApps.last()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }

            // Summary
            if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                Text(
                    text = "No apps found matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * Individual app list item
 */
@Composable
private fun AppListItem(
    app: InstalledAppInfo,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // App icon placeholder (you can enhance this to show actual drawable)
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (app.isSystemApp)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (app.isSystemApp) {
                    Text(
                        text = "System App",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = onToggle
        )
    }
}

/**
 * Suspend function to fetch installed apps
 */
private suspend fun fetchInstalledApps(
    context: Context,
    includeSystemApps: Boolean
): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
    try {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        packages
            .asSequence()
            .filter { appInfo ->
                // Filter system apps if requested
                if (!includeSystemApps) {
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                } else {
                    true
                }
            }
            .map { appInfo ->
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    appName = try {
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        appInfo.packageName
                    },
                    icon = try {
                        packageManager.getApplicationIcon(appInfo)
                    } catch (e: Exception) {
                        null
                    },
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    } catch (e: Exception) {
        Log.e("AppFetcher", "Error fetching apps", e)
        emptyList()
    }
}

/**
 * Utility function to get app name from package name
 */
fun getAppNameFromPackage(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        packageName
    }
}

/**
 * Utility function to check if an app is installed
 */
fun isAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
