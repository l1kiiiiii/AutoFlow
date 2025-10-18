package com.example.autoflow.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
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
import com.example.autoflow.model.AppNotification
import com.example.autoflow.model.NotificationType
import com.example.autoflow.util.InAppNotificationManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Notification bell icon with badge
 */
@Composable
fun NotificationBell(
    modifier: Modifier = Modifier,
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val notificationManager = remember { InAppNotificationManager.getInstance(context) }
    val unreadCount by notificationManager.unreadCount.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        IconButton(onClick = onNotificationClick) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Unread count badge
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(
                        Color.Red,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Notification panel/drawer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notificationManager = remember { InAppNotificationManager.getInstance(context) }
    val notifications by notificationManager.notifications.collectAsStateWithLifecycle()
    val meetingModeActive by notificationManager.meetingModeActive.collectAsStateWithLifecycle()

    if (isVisible) {
        ModalBottomSheetLayout(
            sheetContent = {
                NotificationContent(
                    notifications = notifications,
                    meetingModeActive = meetingModeActive,
                    onMarkAllRead = { notificationManager.markAllAsRead() },
                    onClearAll = { notificationManager.clearAllClearable() },
                    onClearNotification = { notificationManager.clearNotification(it) },
                    onDeactivateMeeting = { notificationManager.deactivateMeetingMode() },
                    onMarkAsRead = { notificationManager.markAsRead(it) }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            // Background content - can be empty or your main content
        }
    }
}

@Composable
private fun NotificationContent(
    notifications: List<AppNotification>,
    meetingModeActive: Boolean,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    onClearNotification: (String) -> Boolean,
    onDeactivateMeeting: () -> Unit,
    onMarkAsRead: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark All Read")
                }
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Notifications list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notifications) { notification ->
                NotificationItem(
                    notification = notification,
                    onClear = { onClearNotification(notification.id) },
                    onMarkAsRead = { onMarkAsRead(notification.id) },
                    onDeactivateMeeting = if (notification.type == NotificationType.MEETING_MODE) onDeactivateMeeting else null
                )
            }

            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notifications",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: AppNotification,
    onClear: () -> Boolean,
    onMarkAsRead: () -> Unit,
    onDeactivateMeeting: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                notification.type == NotificationType.MEETING_MODE -> MaterialTheme.colorScheme.primaryContainer
                !notification.isRead -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getNotificationIcon(notification.type),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = getNotificationColor(notification.type)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(notification.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    // Meeting mode deactivate button
                    if (notification.type == NotificationType.MEETING_MODE && onDeactivateMeeting != null) {
                        Button(
                            onClick = onDeactivateMeeting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.size(width = 100.dp, height = 36.dp)
                        ) {
                            Text(
                                text = "Deactivate",
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Clear button (only for clearable notifications)
                    if (notification.isClearable) {
                        Spacer(modifier = Modifier.height(4.dp))
                        IconButton(
                            onClick = { onClear() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Auto-mark as read when displayed
    LaunchedEffect(notification.id) {
        if (!notification.isRead) {
            onMarkAsRead()
        }
    }
}

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

@Composable
private fun getNotificationColor(type: NotificationType) = when (type) {
    NotificationType.TASK_EXECUTED, NotificationType.SUCCESS -> Color.Green
    NotificationType.SMS_SENT -> Color.Blue
    NotificationType.MEETING_MODE -> Color(0xFFFF6B35)
    NotificationType.WORKFLOW_TRIGGERED -> MaterialTheme.colorScheme.primary
    NotificationType.ERROR -> Color.Red
    NotificationType.INFO -> MaterialTheme.colorScheme.onSurface
}
