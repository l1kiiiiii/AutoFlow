package com.example.autoflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.autoflow.util.Constants
import kotlin.math.roundToInt

/**
 * UI Components for Brightness and Volume Control Actions in TaskCreationScreen
 */

// Data classes for brightness and volume options
data class BrightnessActionOption(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String
)

data class VolumeActionOption(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String
)

data class BrightnessLevelOption(
    val level: String,
    val name: String,
    val percentage: Int,
    val description: String
)

data class VolumeStreamOption(
    val stream: String,
    val name: String,
    val icon: ImageVector,
    val description: String
)

/**
 * Brightness Action Configuration Component
 */
@Composable
fun BrightnessActionCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedAction: String,
    onActionChange: (String) -> Unit,
    actionValue: String,
    onValueChange: (String) -> Unit
) {
    val brightnessActions = listOf(
        BrightnessActionOption(
            Constants.ACTION_SET_BRIGHTNESS,
            "Set Fixed Brightness",
            Icons.Default.Brightness6,
            "Set brightness to specific level (0-100%)"
        ),
        BrightnessActionOption(
            Constants.ACTION_INCREASE_BRIGHTNESS,
            "Increase Brightness",
            Icons.Default.BrightnessHigh,
            "Increase brightness by amount"
        ),
        BrightnessActionOption(
            Constants.ACTION_DECREASE_BRIGHTNESS,
            "Decrease Brightness",
            Icons.Default.BrightnessLow,
            "Decrease brightness by amount"
        ),
        BrightnessActionOption(
            Constants.ACTION_ADJUST_BRIGHTNESS_TIME,
            "Auto Time-Based",
            Icons.Default.Brightness4,
            "Automatically adjust based on time of day"
        ),
        BrightnessActionOption(
            Constants.ACTION_BRIGHTNESS_ENVIRONMENT,
            "Environment-Based",
            Icons.Default.BrightnessMedium,
            "Adjust based on environment (indoor/outdoor/etc)"
        ),
        BrightnessActionOption(
            Constants.ACTION_BRIGHTNESS_LEVEL,
            "Preset Level",
            Icons.Default.Settings,
            "Use predefined brightness levels"
        )
    )

    ExpandableActionSection(
        title = "Brightness Control",
        icon = Icons.Default.Brightness6,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        BrightnessActionContent(
            actions = brightnessActions,
            selectedAction = selectedAction,
            onActionChange = onActionChange,
            actionValue = actionValue,
            onValueChange = onValueChange
        )
    }
}

@Composable
fun ExpandableActionSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    TODO("Not yet implemented")
}

@Composable
fun BrightnessActionContent(
    actions: List<BrightnessActionOption>,
    selectedAction: String,
    onActionChange: (String) -> Unit,
    actionValue: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Select Brightness Action",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Action selection
        actions.forEach { action ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionChange(action.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedAction == action.id)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (selectedAction == action.id)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (selectedAction == action.id)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            action.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedAction == action.id) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            action.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (selectedAction == action.id) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Configuration based on selected action
        if (selectedAction.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            BrightnessActionConfiguration(
                selectedAction = selectedAction,
                currentValue = actionValue,
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
fun BrightnessActionConfiguration(
    selectedAction: String,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (selectedAction) {
                Constants.ACTION_SET_BRIGHTNESS -> {
                    Text("Set Brightness Level (0-100%)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var brightness by remember { mutableFloatStateOf(currentValue.toFloatOrNull() ?: 50f) }
                    
                    Text("Brightness: ${brightness.roundToInt()}%")
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            onValueChange(it.roundToInt().toString())
                        },
                        valueRange = 1f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    // Quick presets
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(10, 25, 50, 75, 100)) { preset ->
                            FilterChip(
                                selected = brightness.roundToInt() == preset,
                                onClick = {
                                    brightness = preset.toFloat()
                                    onValueChange(preset.toString())
                                },
                                label = { Text("$preset%") }
                            )
                        }
                    }
                }
                
                Constants.ACTION_INCREASE_BRIGHTNESS, Constants.ACTION_DECREASE_BRIGHTNESS -> {
                    val actionWord = if (selectedAction.contains("INCREASE")) "increase" else "decrease"
                    Text("Amount to $actionWord brightness (%)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var amount by remember { mutableFloatStateOf(currentValue.toFloatOrNull() ?: 20f) }
                    
                    Text("Amount: ${amount.roundToInt()}%")
                    Slider(
                        value = amount,
                        onValueChange = {
                            amount = it
                            onValueChange(it.roundToInt().toString())
                        },
                        valueRange = 5f..50f,
                        steps = 9
                    )
                }
                
                Constants.ACTION_BRIGHTNESS_ENVIRONMENT -> {
                    Text("Select Environment", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val environments = listOf(
                        "outdoor" to "Outdoor/Sunny (100%)",
                        "office" to "Office/Work (75%)",
                        "home" to "Home/Indoor (60%)",
                        "evening" to "Evening (40%)",
                        "night" to "Night (20%)",
                        "cinema" to "Cinema/Theater (5%)"
                    )
                    
                    environments.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onValueChange(value) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentValue == value,
                                onClick = { onValueChange(value) }
                            )
                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                Constants.ACTION_BRIGHTNESS_LEVEL -> {
                    Text("Select Brightness Level", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val levels = listOf(
                        BrightnessLevelOption("minimum", "Minimum", 1, "Barely visible"),
                        BrightnessLevelOption("very_low", "Very Low", 10, "Very dim"),
                        BrightnessLevelOption("low", "Low", 25, "Dim for dark environments"),
                        BrightnessLevelOption("medium", "Medium", 50, "Balanced brightness"),
                        BrightnessLevelOption("high", "High", 80, "Bright for outdoor use"),
                        BrightnessLevelOption("maximum", "Maximum", 100, "Maximum brightness")
                    )
                    
                    levels.forEach { level ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onValueChange(level.level) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentValue == level.level)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (currentValue == level.level)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        level.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (currentValue == level.level) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        level.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Text(
                                    "${level.percentage}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                if (currentValue == level.level) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                Constants.ACTION_ADJUST_BRIGHTNESS_TIME -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        "Automatic Time-Based Brightness",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "\u2022 Morning (6-8 AM): 40%\n" +
                                        "\u2022 Day (9 AM-5 PM): 70-80%\n" +
                                        "\u2022 Evening (6-8 PM): 60%\n" +
                                        "\u2022 Night (9 PM+): 10-30%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Volume Action Configuration Component
 */
@Composable
fun VolumeActionCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedAction: String,
    onActionChange: (String) -> Unit,
    actionValue: String,
    onValueChange: (String) -> Unit,
    streamType: String,
    onStreamTypeChange: (String) -> Unit
) {
    val volumeActions = listOf(
        VolumeActionOption(
            Constants.ACTION_SET_MEDIA_VOLUME,
            "Set Media Volume",
            Icons.Default.VolumeUp,
            "Set media/music volume level"
        ),
        VolumeActionOption(
            Constants.ACTION_SET_RING_VOLUME,
            "Set Ring Volume",
            Icons.Default.PhoneCallback,
            "Set ringtone volume level"
        ),
        VolumeActionOption(
            Constants.ACTION_SET_NOTIFICATION_VOLUME,
            "Set Notification Volume",
            Icons.Default.Notifications,
            "Set notification sound volume"
        ),
        VolumeActionOption(
            Constants.ACTION_INCREASE_VOLUME,
            "Increase Volume",
            Icons.Default.VolumeUp,
            "Increase volume by amount"
        ),
        VolumeActionOption(
            Constants.ACTION_DECREASE_VOLUME,
            "Decrease Volume",
            Icons.Default.VolumeDown,
            "Decrease volume by amount"
        ),
        VolumeActionOption(
            Constants.ACTION_MUTE_VOLUME,
            "Mute Audio",
            Icons.Default.VolumeMute,
            "Mute specific audio stream"
        ),
        VolumeActionOption(
            Constants.ACTION_SET_VOLUME_PROFILE,
            "Volume Profile",
            Icons.Default.Settings,
            "Apply predefined volume profile"
        )
    )

    ExpandableActionSection(
        title = "Volume Control",
        icon = Icons.Default.VolumeUp,
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        VolumeActionContent(
            actions = volumeActions,
            selectedAction = selectedAction,
            onActionChange = onActionChange,
            actionValue = actionValue,
            onValueChange = onValueChange,
            streamType = streamType,
            onStreamTypeChange = onStreamTypeChange
        )
    }
}

@Composable
fun VolumeActionContent(
    actions: List<VolumeActionOption>,
    selectedAction: String,
    onActionChange: (String) -> Unit,
    actionValue: String,
    onValueChange: (String) -> Unit,
    streamType: String,
    onStreamTypeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Select Volume Action",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Action selection
        actions.forEach { action ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionChange(action.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedAction == action.id)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (selectedAction == action.id)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (selectedAction == action.id)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            action.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedAction == action.id) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            action.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (selectedAction == action.id) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Configuration based on selected action
        if (selectedAction.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            VolumeActionConfiguration(
                selectedAction = selectedAction,
                currentValue = actionValue,
                onValueChange = onValueChange,
                streamType = streamType,
                onStreamTypeChange = onStreamTypeChange
            )
        }
    }
}

@Composable
fun VolumeActionConfiguration(
    selectedAction: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    streamType: String,
    onStreamTypeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (selectedAction) {
                Constants.ACTION_SET_MEDIA_VOLUME, 
                Constants.ACTION_SET_RING_VOLUME,
                Constants.ACTION_SET_NOTIFICATION_VOLUME,
                Constants.ACTION_SET_ALARM_VOLUME,
                Constants.ACTION_SET_CALL_VOLUME -> {
                    val streamName = selectedAction.replace("SET_", "").replace("_VOLUME", "")
                    Text("Set $streamName Volume Level (0-100%)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var volume by remember { mutableFloatStateOf(currentValue.toFloatOrNull() ?: 50f) }
                    
                    Text("Volume: ${volume.roundToInt()}%")
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            onValueChange(it.roundToInt().toString())
                        },
                        valueRange = 0f..100f,
                        steps = 20
                    )
                    
                    // Quick presets
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0, 25, 50, 75, 100)) { preset ->
                            FilterChip(
                                selected = volume.roundToInt() == preset,
                                onClick = {
                                    volume = preset.toFloat()
                                    onValueChange(preset.toString())
                                },
                                label = { Text(if (preset == 0) "Silent" else "$preset%") }
                            )
                        }
                    }
                }
                
                Constants.ACTION_INCREASE_VOLUME, Constants.ACTION_DECREASE_VOLUME -> {
                    Text("Audio Stream Type", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val streams = listOf(
                        VolumeStreamOption("media", "Media/Music", Icons.Default.VolumeUp, "Music, videos, games"),
                        VolumeStreamOption("ring", "Ring/Phone", Icons.Default.PhoneCallback, "Incoming calls"),
                        VolumeStreamOption("notification", "Notifications", Icons.Default.Notifications, "App notifications"),
                        VolumeStreamOption("alarm", "Alarms", Icons.Default.Settings, "Alarm clock sounds")
                    )
                    
                    streams.forEach { stream ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStreamTypeChange(stream.stream) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (streamType == stream.stream)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(stream.icon, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stream.name,
                                        fontWeight = if (streamType == stream.stream) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        stream.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (streamType == stream.stream) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    if (streamType.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        val actionWord = if (selectedAction.contains("INCREASE")) "increase" else "decrease"
                        Text("Amount to $actionWord volume (%)", style = MaterialTheme.typography.titleMedium)
                        
                        var amount by remember { mutableFloatStateOf(currentValue.toFloatOrNull() ?: 10f) }
                        
                        Text("Amount: ${amount.roundToInt()}%")
                        Slider(
                            value = amount,
                            onValueChange = {
                                amount = it
                                onValueChange(it.roundToInt().toString())
                            },
                            valueRange = 5f..30f,
                            steps = 5
                        )
                    }
                }
                
                Constants.ACTION_MUTE_VOLUME -> {
                    Text("Select Audio Stream to Mute", style = MaterialTheme.typography.titleMedium)
                    VolumeStreamSelector(streamType, onStreamTypeChange)
                }
                
                Constants.ACTION_SET_VOLUME_PROFILE -> {
                    Text("Select Volume Profile", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val profiles = listOf(
                        "silent" to "Silent - All audio muted",
                        "low" to "Low - Quiet environment",
                        "medium" to "Medium - Balanced levels",
                        "high" to "High - Loud environment",
                        "meeting" to "Meeting - Silent media, alarm only",
                        "work" to "Work - Medium media, low ring",
                        "night" to "Night - Very quiet, alarms only"
                    )
                    
                    profiles.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onValueChange(value) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentValue == value,
                                onClick = { onValueChange(value) }
                            )
                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeStreamSelector(
    streamType: String,
    onStreamTypeChange: (String) -> Unit
) {
    val streams = listOf(
        VolumeStreamOption("media", "Media/Music", Icons.Default.VolumeUp, "Music, videos, games"),
        VolumeStreamOption("ring", "Ring/Phone", Icons.Default.PhoneCallback, "Incoming calls"),
        VolumeStreamOption("notification", "Notifications", Icons.Default.Notifications, "App notifications"),
        VolumeStreamOption("alarm", "Alarms", Icons.Default.Settings, "Alarm clock sounds")
    )
    
    streams.forEach { stream ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStreamTypeChange(stream.stream) },
            colors = CardDefaults.cardColors(
                containerColor = if (streamType == stream.stream)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(stream.icon, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stream.name,
                        fontWeight = if (streamType == stream.stream) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        stream.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (streamType == stream.stream) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}