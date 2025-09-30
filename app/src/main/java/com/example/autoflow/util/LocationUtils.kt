// Fixed LocationUtils.kt
package com.example.autoflow.util

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        locationState = locationState.copy(hasPermission = hasLocationPermission, error = null)

        if (hasLocationPermission) {
            // Fetch location when permission is granted
            fetchCurrentLocationWithRetry(context, scope) { result ->
                locationState = result
            }
        } else {
            locationState = locationState.copy(
                error = "Location permission denied",
                isLoading = false
            )
        }
    }

    LaunchedEffect(Unit) {
        // Check if we already have permission
        val hasPermission = checkLocationPermissions(context)

        if (hasPermission) {
            locationState = locationState.copy(hasPermission = true)

            // Check if location services are enabled
            if (isLocationEnabled(context)) {
                fetchCurrentLocationWithRetry(context, scope) { result ->
                    locationState = result
                }
            } else {
                locationState = locationState.copy(
                    error = "Please enable GPS in device settings",
                    isLoading = false,
                    hasPermission = true
                )
            }
        } else {
            // Request permissions
            locationState = locationState.copy(hasPermission = false)
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

private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private fun fetchCurrentLocationWithRetry(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (LocationState) -> Unit
) {
    Log.d("LocationUtils", "Starting location fetch")
    onResult(LocationState(isLoading = true, hasPermission = true))

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Method 1: Try getCurrentLocation first
    getCurrentLocationWithTimeout(context, fusedLocationClient) { success, location, error ->
        if (success && location != null) {
            Log.d("LocationUtils", "Got current location: $location")
            onResult(LocationState(
                location = location,
                isLoading = false,
                hasPermission = true
            ))
        } else {
            Log.w("LocationUtils", "getCurrentLocation failed: $error")
            // Method 2: Try getLastLocation as fallback
            getLastKnownLocation(context, fusedLocationClient) { lastSuccess, lastLocation, lastError ->
                if (lastSuccess && lastLocation != null) {
                    Log.d("LocationUtils", "Got last known location: $lastLocation")
                    onResult(LocationState(
                        location = lastLocation,
                        isLoading = false,
                        hasPermission = true
                    ))
                } else {
                    Log.w("LocationUtils", "getLastLocation failed: $lastError")
                    // Method 3: Request fresh location updates
                    requestLocationUpdates(context, fusedLocationClient, scope, onResult)
                }
            }
        }
    }
}

private fun getCurrentLocationWithTimeout(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    callback: (Boolean, Location?, String?) -> Unit
) {
    try {
        val cancellationToken = CancellationTokenSource()

        // Set timeout for location request
        val timeoutRunnable = Runnable {
            cancellationToken.cancel()
            callback(false, null, "Location request timeout")
        }

        val handler = android.os.Handler(Looper.getMainLooper())
        handler.postDelayed(timeoutRunnable, 15000) // 15 second timeout

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            handler.removeCallbacks(timeoutRunnable)
            if (location != null) {
                callback(true, location, null)
            } else {
                callback(false, null, "Location is null")
            }
        }.addOnFailureListener { exception ->
            handler.removeCallbacks(timeoutRunnable)
            callback(false, null, "getCurrentLocation failed: ${exception.message}")
        }
    } catch (e: SecurityException) {
        callback(false, null, "Location permission denied")
    }
}

private fun getLastKnownLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    callback: (Boolean, Location?, String?) -> Unit
) {
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Check if location is recent (within 5 minutes)
                val currentTime = System.currentTimeMillis()
                val locationAge = currentTime - location.time

                if (locationAge < 300000) { // 5 minutes
                    callback(true, location, null)
                } else {
                    callback(false, null, "Last known location too old")
                }
            } else {
                callback(false, null, "No last known location")
            }
        }.addOnFailureListener { exception ->
            callback(false, null, "getLastLocation failed: ${exception.message}")
        }
    } catch (e: SecurityException) {
        callback(false, null, "Location permission denied")
    }
}

private fun requestLocationUpdates(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (LocationState) -> Unit
) {
    Log.d("LocationUtils", "Requesting fresh location updates")

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setMinUpdateIntervalMillis(2000)
        .setMaxUpdateDelayMillis(10000)
        .build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            Log.d("LocationUtils", "Received location update")

            // Get the most recent location from the result
            val locations = locationResult.locations
            if (locations.isNotEmpty()) {
                val location = locations.last() // Get the most recent location

                // Stop updates once we get a location
                fusedLocationClient.removeLocationUpdates(this)
                onResult(LocationState(
                    location = location,
                    isLoading = false,
                    hasPermission = true
                ))
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            if (!availability.isLocationAvailable) {
                Log.w("LocationUtils", "Location not available")
                fusedLocationClient.removeLocationUpdates(this)
                onResult(LocationState(
                    error = "GPS signal not available. Please go to an open area.",
                    isLoading = false,
                    hasPermission = true
                ))
            }
        }
    }

    try {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // Set timeout for location updates
        scope.launch {
            delay(30000) // 30 second timeout
            fusedLocationClient.removeLocationUpdates(locationCallback)
            onResult(LocationState(
                error = "Unable to get location. Please check GPS settings and try again.",
                isLoading = false,
                hasPermission = true
            ))
        }
    } catch (e: SecurityException) {
        onResult(LocationState(
            error = "Location permission denied",
            isLoading = false,
            hasPermission = false
        ))
    }
}

// Helper function for manual location refresh (to be used in UI)
fun refreshLocation(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (LocationState) -> Unit
) {
    if (checkLocationPermissions(context) && isLocationEnabled(context)) {
        fetchCurrentLocationWithRetry(context, scope, onResult)
    } else {
        onResult(LocationState(
            error = "Location permission or GPS not enabled",
            isLoading = false,
            hasPermission = checkLocationPermissions(context)
        ))
    }
}
