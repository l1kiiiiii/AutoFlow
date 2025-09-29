// Create this new file: LocationUtils.kt in util package
package com.example.autoflow.util

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

data class LocationState(
    val location: Location? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasPermission: Boolean = false
)

@Composable
fun rememberLocationState(): LocationState {
    var locationState by remember { mutableStateOf(LocationState()) }
    val context = LocalContext.current

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        locationState = locationState.copy(hasPermission = hasLocationPermission)

        if (hasLocationPermission) {
            // Automatically fetch location when permission is granted
            fetchCurrentLocation(context) { result ->
                locationState = result
            }
        }
    }

    LaunchedEffect(Unit) {
        // Check if we already have permission
        val hasPermission = checkLocationPermissions(context)
        locationState = locationState.copy(hasPermission = hasPermission)

        if (!hasPermission) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    return locationState
}

private fun checkLocationPermissions(context: Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun fetchCurrentLocation(
    context: Context,
    onResult: (LocationState) -> Unit
) {
    onResult(LocationState(isLoading = true, hasPermission = true))

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cancellationToken = CancellationTokenSource()

    try {
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                onResult(LocationState(
                    location = location,
                    isLoading = false,
                    hasPermission = true
                ))
            } else {
                // Try getLastLocation as fallback
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    onResult(LocationState(
                        location = lastLocation,
                        isLoading = false,
                        hasPermission = true,
                        error = if (lastLocation == null) "Unable to get location" else null
                    ))
                }
            }
        }.addOnFailureListener { exception ->
            onResult(LocationState(
                isLoading = false,
                hasPermission = true,
                error = "Failed to get location: ${exception.message}"
            ))
        }
    } catch (e: SecurityException) {
        onResult(LocationState(
            isLoading = false,
            hasPermission = false,
            error = "Location permission denied"
        ))
    }
}
