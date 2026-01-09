package com.example.autoflow.ui.theme.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
/**
 * Settings Screen for AutoFlow
 * Provides access to app settings, permissions, and information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Shared Preferences
    val prefs = remember { context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE) }

    // Settings State
    var autoReplyEnabled by remember { mutableStateOf(prefs.getBoolean("auto_reply_enabled", false)) }
    var autoReplyMessage by remember { mutableStateOf(prefs.getString("auto_reply_message", "") ?: "") }
    var onlyInDnd by remember { mutableStateOf(prefs.getBoolean("auto_reply_only_in_dnd", true)) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var darkModeEnabled by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }


    // Location Launcher
    var locationPermissionGranted by remember { mutableStateOf(checkLocationPermission(context)) }
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Update state based on result
        locationPermissionGranted = checkLocationPermission(context)
        if (locationPermissionGranted) {
            scope.launch { snackbarHostState.showSnackbar("Location permission granted") }
        }
    }

    // Bluetooth Launcher
    var bluetoothPermissionGranted by remember { mutableStateOf(checkBluetoothPermission(context)) }
    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        bluetoothPermissionGranted = checkBluetoothPermission(context)
    }

    // SMS Launcher
    var smsPermissionGranted by remember { mutableStateOf(checkSmsPermission(context)) }
    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        smsPermissionGranted = isGranted
        if (isGranted) scope.launch { snackbarHostState.showSnackbar("SMS permission granted") }
    }

    // Notification Launcher
    var notificationPermissionGranted by remember { mutableStateOf(checkNotificationPermission(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }

    // Exact Alarm state
    var exactAlarmGranted by remember { mutableStateOf(checkExactAlarmPermission(context)) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // App Settings Section
            item {
                SettingsSectionHeader(title = "App Settings")
            }

            item {
                SettingsCard {
                    Column {
                        SettingsToggleItem(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            description = "Enable workflow notifications",
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                prefs.edit().putBoolean("notifications_enabled", enabled).apply()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = if (enabled) "Notifications enabled" else "Notifications disabled",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsToggleItem(
                            icon = Icons.Default.DarkMode,
                            title = "Dark Mode",
                            description = "Use dark theme",
                            checked = darkModeEnabled,
                            onCheckedChange = { enabled ->
                                darkModeEnabled = enabled
                                prefs.edit().putBoolean("dark_mode", enabled).apply()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Restart app to apply theme changes",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Auto-Reply Settings Section
            item {
                SettingsSectionHeader(title = "Auto-Reply Settings")
            }

            item {
                SettingsCard {
                    Column {
                        SettingsToggleItem(
                            icon = Icons.Default.Message,
                            title = "Auto-Reply SMS",
                            description = "Automatically reply to incoming calls/SMS",
                            checked = autoReplyEnabled,
                            onCheckedChange = { enabled ->
                                autoReplyEnabled = enabled
                                prefs.edit().putBoolean("auto_reply_enabled", enabled).apply()
                            }
                        )

                        AnimatedVisibility(visible = autoReplyEnabled) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                SettingsClickableItem(
                                    icon = Icons.Default.Edit,
                                    title = "Auto-Reply Message",
                                    description = if (autoReplyMessage.isEmpty())
                                        "Tap to set message"
                                    else
                                        autoReplyMessage,
                                    onClick = { showMessageDialog = true }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                SettingsToggleItem(
                                    icon = Icons.Default.DoNotDisturb,
                                    title = "Only in Do Not Disturb",
                                    description = "Auto-reply only when DND is active",
                                    checked = onlyInDnd,
                                    onCheckedChange = { enabled ->
                                        onlyInDnd = enabled
                                        prefs.edit().putBoolean("auto_reply_only_in_dnd", enabled).apply()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Permissions Section
            item {
                SettingsSectionHeader(title = "Permissions")
            }

            items(
                listOf(
                    PermissionItem(
                        icon = Icons.Default.LocationOn,
                        title = "Location",
                        description = "Required for location-based triggers",
                        granted = locationPermissionGranted,
                        onClick = {
                            if (!locationPermissionGranted) {
                                //  Launch the popup instead of opening settings
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                openAppSettings(context)
                            }
                        }
                    ),
                    PermissionItem(
                        icon = Icons.Default.Bluetooth,
                        title = "Bluetooth",
                        description = "Required for Bluetooth triggers",
                        granted = bluetoothPermissionGranted,
                        onClick = { if (!bluetoothPermissionGranted) {
                            //  Handle Android 12+ vs older versions
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                bluetoothLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    )
                                )
                            } else {
                                // Older phones need location for Bluetooth scanning
                                locationLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                )
                            }
                        } else {
                            openAppSettings(context)
                        }
                        }
                    ),
                    PermissionItem(
                        icon = Icons.Default.Sms,
                        title = "SMS",
                        description = "Required for auto-reply feature",
                        granted = smsPermissionGranted,
                        onClick = {
                            if (!smsPermissionGranted) {
                                //  Launch SMS permission popup
                                smsLauncher.launch(Manifest.permission.SEND_SMS)
                            } else {
                                openAppSettings(context)
                            }
                        }
                    ),
                    PermissionItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Notifications",
                        description = "Required for workflow notifications",
                        granted = notificationPermissionGranted,
                        onClick = {
                            if (!notificationPermissionGranted) {
                                //  Launch Notification popup (Android 13+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                openAppSettings(context)
                            }
                        }
                    ),
                    PermissionItem(
                        icon = Icons.Default.Alarm,
                        title = "Exact Alarms",
                        description = "Required for time-based workflows",
                        granted = exactAlarmGranted,
                        onClick = { openExactAlarmSettings(context) }
                    )
                )
            ) { permission ->
                SettingsCard {
                    PermissionItemView(permission = permission)
                }
            }

            // About Section
            item {
                SettingsSectionHeader(title = "About")
            }

            item {
                SettingsCard {
                    Column {
                        SettingsClickableItem(
                            icon = Icons.Default.Info,
                            title = "About AutoFlow",
                            description = "Version 1.0 (luv branch)",
                            onClick = { showAboutDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsClickableItem(
                            icon = Icons.Default.Code,
                            title = "GitHub Repository",
                            description = "View source code",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://github.com/l1kiiiiii/AutoFlow")
                                }
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsClickableItem(
                            icon = Icons.Default.BugReport,
                            title = "Report Issue",
                            description = "Report bugs or request features",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://github.com/l1kiiiiii/AutoFlow/issues")
                                }
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsClickableItem(
                            icon = Icons.Default.Settings,
                            title = "App Settings",
                            description = "Open system app settings",
                            onClick = { openAppSettings(context) }
                        )
                    }
                }
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AutoFlow v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Made with  by l1kiiiiii",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Auto-Reply Message Dialog
    if (showMessageDialog) {
        AutoReplyMessageDialog(
            currentMessage = autoReplyMessage,
            onDismiss = { showMessageDialog = false },
            onConfirm = { newMessage ->
                autoReplyMessage = newMessage
                prefs.edit().putString("auto_reply_message", newMessage).apply()
                showMessageDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Auto-reply message updated",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

// ============================================
// COMPOSABLE COMPONENTS
// ============================================

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionItemView(permission: PermissionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = permission.onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = permission.icon,
                contentDescription = permission.title,
                tint = if (permission.granted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
            Column {
                Text(
                    text = permission.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Badge(
            containerColor = if (permission.granted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                text = if (permission.granted) "Granted" else "Required",
                style = MaterialTheme.typography.labelSmall,
                color = if (permission.granted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun AutoReplyMessageDialog(
    currentMessage: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var message by remember { mutableStateOf(currentMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Message, contentDescription = null) },
        title = { Text("Auto-Reply Message") },
        text = {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                placeholder = { Text("I'm currently busy and will get back to you soon.") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(message) },
                enabled = message.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("About AutoFlow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Version 1.0 (luv branch)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "AutoFlow is a sophisticated Android automation application that allows you to create powerful workflows based on triggers and actions.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Features:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "• Time-based automation\n• Location-based triggers\n• WiFi & Bluetooth triggers\n• Custom script execution\n• Auto-reply SMS\n• Sound mode control",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Developed by l1kiiiiii",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ============================================
// DATA CLASSES
// ============================================

data class PermissionItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val granted: Boolean,
    val onClick: () -> Unit
)

// ============================================
// HELPER FUNCTIONS
// ============================================

private fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkBluetoothPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Permission not required before Android 13
    }
}

private fun checkExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.canScheduleExactAlarms() ?: false
    } else {
        true // Permission not required before Android 12
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context)
        }
    } else {
        openAppSettings(context)
    }
}
