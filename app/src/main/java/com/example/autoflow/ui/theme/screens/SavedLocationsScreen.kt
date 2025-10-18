package com.example.autoflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autoflow.model.SavedLocation
import com.example.autoflow.viewmodel.LocationViewModel

/**
 * ✅ Complete SavedLocationsScreen with all features
 * - List all saved locations
 * - Add new locations (manual/current)
 * - Edit existing locations
 * - Delete locations
 * - Toggle favorites
 * - Search locations
 * - Location selection for workflows
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsScreen(
    onLocationSelected: (SavedLocation) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationViewModel = viewModel()
) {
    // State variables
    val locations by viewModel.allLocations.observeAsState(emptyList())
    val statusMessage by viewModel.statusMessage.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    val currentLocation by viewModel.currentLocation.observeAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    // Filter locations based on search
    val filteredLocations = remember(locations, searchQuery) {
        if (searchQuery.isBlank()) {
            locations
        } else {
            locations.filter { location ->
                location.name.contains(searchQuery, ignoreCase = true) ||
                        location.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Clear status message after delay
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchTopAppBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onCloseSearch = {
                        showSearchBar = false
                        searchQuery = ""
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Saved Locations (${locations.size})") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (locations.isNotEmpty()) {
                            IconButton(onClick = { showSearchBar = true }) {
                                Icon(Icons.Default.Search, "Search")
                            }
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, "Add Location")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.getCurrentLocation()
                },
                icon = { Icon(Icons.Default.MyLocation, "Get Location") },
                text = { Text("Current Location") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status message
            statusMessage?.let { message ->
                StatusMessageCard(
                    message = message,
                    isError = message.startsWith("❌"),
                    onDismiss = { viewModel.clearStatusMessage() }
                )
            }

            // Loading indicator
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Content
            if (filteredLocations.isEmpty()) {
                if (searchQuery.isNotBlank()) {
                    EmptySearchState(
                        searchQuery = searchQuery,
                        onClearSearch = { searchQuery = "" },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    EmptyLocationsState(
                        onAddClick = { showAddDialog = true },
                        onGetCurrentClick = { viewModel.getCurrentLocation() },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current location info
                    currentLocation?.let { location ->
                        item {
                            CurrentLocationCard(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                onSaveClick = { showAddDialog = true }
                            )
                        }
                    }

                    // Favorite locations first
                    val favoriteLocations = filteredLocations.filter { it.isFavorite }
                    val regularLocations = filteredLocations.filter { !it.isFavorite }

                    if (favoriteLocations.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "⭐ Favorites",
                                count = favoriteLocations.size
                            )
                        }
                        items(favoriteLocations) { location ->
                            LocationCard(
                                location = location,
                                onLocationClick = { onLocationSelected(location) },
                                onEditClick = {
                                    selectedLocation = location
                                    showEditDialog = true
                                },
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(location.id, !location.isFavorite)
                                },
                                onDeleteClick = {
                                    viewModel.deleteLocation(location)
                                }
                            )
                        }
                    }

                    if (regularLocations.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "📍 All Locations",
                                count = regularLocations.size
                            )
                        }
                        items(regularLocations) { location ->
                            LocationCard(
                                location = location,
                                onLocationClick = { onLocationSelected(location) },
                                onEditClick = {
                                    selectedLocation = location
                                    showEditDialog = true
                                },
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(location.id, !location.isFavorite)
                                },
                                onDeleteClick = {
                                    viewModel.deleteLocation(location)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddLocationDialog(
            currentLocation = currentLocation,
            onDismiss = { showAddDialog = false },
            onSave = { name, lat, lng, radius, address ->
                viewModel.saveLocation(
                    name = name,
                    latitude = lat,
                    longitude = lng,
                    radius = radius,
                    address = address
                )
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && selectedLocation != null) {
        EditLocationDialog(
            location = selectedLocation!!,
            onDismiss = {
                showEditDialog = false
                selectedLocation = null
            },
            onSave = { updatedLocation ->
                viewModel.updateLocation(updatedLocation)
                showEditDialog = false
                selectedLocation = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search locations...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.Default.ArrowBack, "Close Search")
            }
        }
    )
}

@Composable
fun StatusMessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = if (isError)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    "Dismiss",
                    tint = if (isError)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CurrentLocationCard(
    latitude: Double,
    longitude: Double,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📍 Current Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Lat: ${String.format("%.6f", latitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Lng: ${String.format("%.6f", longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Button(onClick = onSaveClick) {
                    Icon(Icons.Default.Save, "Save", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun LocationCard(
    location: SavedLocation,
    onLocationClick: () -> Unit,
    onEditClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLocationClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (location.isFavorite) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Favorite",
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(x = 4.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (location.address.isNotBlank()) {
                        Text(
                            text = location.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📍 ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🎯 ${location.radius.toInt()}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                TextButton(onClick = onFavoriteClick) {
                    Icon(
                        if (location.isFavorite) Icons.Filled.Star else Icons.Default.StarBorder,
                        "Favorite",
                        modifier = Modifier.size(18.dp),
                        tint = if (location.isFavorite)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (location.isFavorite) "Favorited" else "Favorite")
                }

                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun AddLocationDialog(
    currentLocation: android.location.Location?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember {
        mutableStateOf(currentLocation?.latitude?.toString() ?: "")
    }
    var longitude by remember {
        mutableStateOf(currentLocation?.longitude?.toString() ?: "")
    }
    var radius by remember { mutableStateOf("100") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add New Location",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (meters)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val lat = latitude.toDoubleOrNull() ?: 0.0
                            val lng = longitude.toDoubleOrNull() ?: 0.0
                            val rad = radius.toDoubleOrNull() ?: 100.0
                            if (name.isNotBlank() && lat != 0.0 && lng != 0.0) {
                                onSave(name.trim(), lat, lng, rad, address.trim())
                            }
                        },
                        enabled = name.isNotBlank() &&
                                latitude.toDoubleOrNull() != null &&
                                longitude.toDoubleOrNull() != null
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun EditLocationDialog(
    location: SavedLocation,
    onDismiss: () -> Unit,
    onSave: (SavedLocation) -> Unit
) {
    var name by remember { mutableStateOf(location.name) }
    var address by remember { mutableStateOf(location.address) }
    var latitude by remember { mutableStateOf(location.latitude.toString()) }
    var longitude by remember { mutableStateOf(location.longitude.toString()) }
    var radius by remember { mutableStateOf(location.radius.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Location",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radius (meters)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val lat = latitude.toDoubleOrNull() ?: location.latitude
                            val lng = longitude.toDoubleOrNull() ?: location.longitude
                            val rad = radius.toDoubleOrNull() ?: location.radius

                            val updatedLocation = location.copy(
                                name = name.trim(),
                                address = address.trim(),
                                latitude = lat,
                                longitude = lng,
                                radius = rad
                            )
                            onSave(updatedLocation)
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLocationsState(
    onAddClick: () -> Unit,
    onGetCurrentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No saved locations yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start by adding your favorite places or getting your current location",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onGetCurrentClick) {
                    Icon(Icons.Default.MyLocation, "Get Location", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Current")
                }
                OutlinedButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, "Add Location", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Manually")
                }
            }
        }
    }
}

@Composable
fun EmptySearchState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No locations found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No locations match \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onClearSearch) {
                Text("Clear Search")
            }
        }
    }
}
