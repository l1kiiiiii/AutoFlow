package com.example.autoflow.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.autoflow.model.AppNotification
import com.example.autoflow.model.NotificationType
import com.example.autoflow.util.InAppNotificationManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full-screen notifications page showing all AutoFlow notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val notificationManager = remember { InAppNotificationManager.getInstance(context) }
    val notifications by notificationManager.notifications.collectAsStateWithLifecycle()
    val unreadCount by notificationManager.unreadCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Mark all as read button
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { notificationManager.markAllAsRead() }
                        ) {
                            Text("Mark All Read")
                        }
                    }

                    // Clear all button
                    IconButton(
                        onClick = { notificationManager.clearAllClearable() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear All"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header stats
            NotificationStats(
                totalCount = notifications.size,
                unreadCount = unreadCount
            )

            // Notifications list
            if (notifications.isEmpty()) {
                EmptyNotificationsView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClear = {
                                notificationManager.clearNotification(notification.id)
                            },
                            onMarkAsRead = {
                                notificationManager.markAsRead(notification.id)
                            },
                            onDeactivateMeeting = if (notification.type == NotificationType.MEETING_MODE) {
                                { notificationManager.deactivateMeetingMode() }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stats header showing notification counts
 */
@Composable
private fun NotificationStats(
    totalCount: Int,
    unreadCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column {
                Text(
                    text = "Unread",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = unreadCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (unreadCount > 0) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Empty state when no notifications
 */
@Composable
private fun EmptyNotificationsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Notifications",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AutoFlow notifications will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Individual notification card
 */
/**
 * ✅ PERSISTENT: Individual notification card (no auto-mark as read)
 */
@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClear: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDeactivateMeeting: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                notification.type == NotificationType.MEETING_MODE ->
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                !notification.isRead ->
                    MaterialTheme.colorScheme.surfaceVariant  // ✅ Highlight unread
                else ->
                    MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 6.dp else 2.dp  // ✅ Higher elevation for unread
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with icon, title, and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icon and title
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getNotificationIcon(notification.type),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = getNotificationColor(notification.type)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // ✅ PERSISTENT: Show unread badge prominently
                        if (!notification.isRead) {
                            Text(
                                text = "UNREAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        Color.Red,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Action buttons
                Row {
                    // Meeting mode deactivate button
                    if (notification.type == NotificationType.MEETING_MODE && onDeactivateMeeting != null) {
                        Button(
                            onClick = onDeactivateMeeting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deactivate", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // ✅ MANUAL: Mark as read button (only show for unread notifications)
                    if (!notification.isRead) {
                        IconButton(
                            onClick = onMarkAsRead,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = "Mark as read",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Clear button (only for clearable notifications)
                    if (notification.isClearable) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Timestamp and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                        .format(notification.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Notification type badge
                Text(
                    text = getTypeDisplayName(notification.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = getNotificationColor(notification.type),
                    modifier = Modifier
                        .background(
                            getNotificationColor(notification.type).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}


/**
 * Get display icon for notification type
 */
@Composable
private fun getNotificationIcon(type: NotificationType) = when (type) {
    NotificationType.TASK_EXECUTED -> Icons.Default.CheckCircle
    NotificationType.SMS_SENT -> Icons.Default.Message
    NotificationType.MEETING_MODE -> Icons.Default.DoNotDisturb
    NotificationType.WORKFLOW_TRIGGERED -> Icons.Default.PlayArrow
    NotificationType.ERROR -> Icons.Default.Error
    NotificationType.SUCCESS -> Icons.Default.CheckCircle
    NotificationType.INFO -> Icons.Default.Info
}

/**
 * Get color for notification type
 */
@Composable
private fun getNotificationColor(type: NotificationType) = when (type) {
    NotificationType.TASK_EXECUTED, NotificationType.SUCCESS -> Color(0xFF4CAF50)
    NotificationType.SMS_SENT -> Color(0xFF2196F3)
    NotificationType.MEETING_MODE -> Color(0xFFFF5722)
    NotificationType.WORKFLOW_TRIGGERED -> MaterialTheme.colorScheme.primary
    NotificationType.ERROR -> Color(0xFFF44336)
    NotificationType.INFO -> MaterialTheme.colorScheme.onSurface
}

/**
 * Get display name for notification type
 */
private fun getTypeDisplayName(type: NotificationType) = when (type) {
    NotificationType.TASK_EXECUTED -> "Task"
    NotificationType.SMS_SENT -> "SMS"
    NotificationType.MEETING_MODE -> "Meeting"
    NotificationType.WORKFLOW_TRIGGERED -> "Workflow"
    NotificationType.ERROR -> "Error"
    NotificationType.SUCCESS -> "Success"
    NotificationType.INFO -> "Info"
}
