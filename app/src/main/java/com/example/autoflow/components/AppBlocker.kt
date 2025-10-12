package com.example.autoflow.ui.components

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    LaunchedEffect(preSelectedApps) {
        selectedApps = preSelectedApps
    }

    LaunchedEffect(showSystemApps) {
        isLoading = true
        error = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d("AppSelector", "🤖 Running on Android ${Build.VERSION.SDK_INT}, using QUERY_ALL_PACKAGES")
        }

        scope.launch {
            try {
                val apps = fetchInstalledApps(context, showSystemApps)
                installedApps = apps
                Log.d("AppSelector", "✅ Successfully loaded ${apps.size} apps")
            } catch (e: Exception) {
                error = "Failed to load apps: ${e.message}"
                Log.e("AppSelector", "❌ Error loading apps", e)
            } finally {
                isLoading = false
            }
        }
    }

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Apps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${selectedApps.size} of ${installedApps.size} selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

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

        error?.let { errorMsg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Error Loading Apps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val apps = fetchInstalledApps(context, showSystemApps)
                                    installedApps = apps
                                } catch (e: Exception) {
                                    error = "Failed to load apps: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Refresh, "Retry", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
            }
        }

        if (!isLoading && error == null) {
            if (installedApps.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedApps = filteredApps.map { it.packageName }
                            onAppsSelected(selectedApps)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select All", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            selectedApps = emptyList()
                            onAppsSelected(selectedApps)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedApps.isNotEmpty()
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    item {
                        Text(
                            text = "Showing ${filteredApps.size} apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
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

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (filteredApps.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No apps found matching \"$searchQuery\""
                            } else {
                                "No apps available"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

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

        //  Simple approach that works on all Android versions
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        Log.d("AppSelector", "📱 Found ${packages.size} total packages")

        packages
            .asSequence()
            .filter { appInfo ->
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                if (!includeSystemApps && isSystemApp) {
                    false
                } else {
                    // Only include apps with launch intent
                    packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
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
            .also {
                Log.d("AppSelector", "✅ Filtered to ${it.size} launchable apps")
            }
    } catch (e: Exception) {
        Log.e("AppFetcher", "❌ Error fetching apps: ${e.message}", e)
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
