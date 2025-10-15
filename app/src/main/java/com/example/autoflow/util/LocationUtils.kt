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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        locationState = locationState.copy(hasPermission = hasLocationPermission, error = null)

        if (hasLocationPermission) {
            fetchCurrentLocationImmediately(context, scope) { result ->
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
        val hasPermission = checkLocationPermissions(context)
        if (hasPermission) {
            locationState = locationState.copy(hasPermission = true)
            if (isLocationEnabled(context)) {
                fetchCurrentLocationImmediately(context, scope) { result ->
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
            locationState = locationState.copy(hasPermission = false)
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    return locationState
}

private fun checkLocationPermissions(context: Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

//  NEW: INSTANT location fetch - prioritizes fresh GPS
private fun fetchCurrentLocationImmediately(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (LocationState) -> Unit
) {
    Log.d("LocationUtils", "⚡ Starting INSTANT location fetch")
    onResult(LocationState(isLoading = true, hasPermission = true))

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    //  Step 1: Request FRESH location with HIGH priority (no timeout delay)
    requestFreshLocationImmediate(context, fusedLocationClient, scope, onResult)
}

// Request fresh GPS location IMMEDIATELY (no fallback delay)
private fun requestFreshLocationImmediate(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (LocationState) -> Unit
) {
    Log.d("LocationUtils", "📍 Requesting FRESH GPS location with HIGH priority")

    //  AGGRESSIVE location request settings
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, // Highest accuracy
        0L  //  INSTANT updates (0ms interval)
    )
        .setMinUpdateIntervalMillis(0L)
        .setMaxUpdateDelayMillis(0L)
        .setWaitForAccurateLocation(false)
        .build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
            Log.d("LocationUtils", " INSTANT location received!")

            val location = locationResult.lastLocation
            if (location != null) {
                //  Stop updates immediately after first result
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
                Log.w("LocationUtils", "⚠️ GPS not available")
                fusedLocationClient.removeLocationUpdates(this)

                //Fallback to last known location if GPS unavailable
                getLastKnownLocationQuick(context, fusedLocationClient, onResult)
            }
        }
    }

    try {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 
        scope.launch {
            delay(5000)
            fusedLocationClient.removeLocationUpdates(locationCallback)

            //  Quick fallback to last known location
            getLastKnownLocationQuick(context, fusedLocationClient, onResult)
        }
    } catch (e: SecurityException) {
        onResult(LocationState(
            error = "Location permission denied",
            isLoading = false,
            hasPermission = false
        ))
    }
}

//  Quick last known location (no age check)
private fun getLastKnownLocationQuick(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onResult: (LocationState) -> Unit
) {
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d("LocationUtils", "📍 Using last known location")
                onResult(LocationState(
                    location = location,
                    isLoading = false,
                    hasPermission = true
                ))
            } else {
                onResult(LocationState(
                    error = "No location available. Please ensure GPS is on.",
                    isLoading = false,
                    hasPermission = true
                ))
            }
        }.addOnFailureListener {
            onResult(LocationState(
                error = "Unable to get location",
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

// Helper function for manual refresh
fun refreshLocation(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (LocationState) -> Unit
) {
    if (checkLocationPermissions(context) && isLocationEnabled(context)) {
        fetchCurrentLocationImmediately(context, scope, onResult)
    } else {
        onResult(LocationState(
            error = "Location permission or GPS not enabled",
            isLoading = false,
            hasPermission = checkLocationPermissions(context)
        ))
    }
}
