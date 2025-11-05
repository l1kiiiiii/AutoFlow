package com.example.autoflow.integrations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationManager(context: Context) {

    private val context: Context = context.applicationContext
    private val androidLocationManager: android.location.LocationManager? =
        this.context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(this.context)

    private val locationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var callback: Callback? = null
    private var locationCallback: LocationCallback? = null

    companion object {
        private const val TAG = "AutoFlow-LocationManager"
        private const val FAST_LOCATION_TIMEOUT_MS = 5000L // 5 seconds max wait
        private const val HIGH_ACCURACY_INTERVAL_MS = 1000L // 1 second for critical updates
        private const val BALANCED_INTERVAL_MS = 5000L // 5 seconds for regular updates
        private const val MIN_DISTANCE_METERS = 5f // 5 meters minimum movement
    }

    init {
        if (androidLocationManager == null) {
            Log.e(TAG, "LocationManager service not available")
        }
    }

    //  CALLBACK INTERFACE 

    interface Callback {
        fun onLocationReceived(location: Location)
        fun onLocationError(errorMessage: String)
        fun onPermissionDenied()
    }
    suspend fun getCurrentLocationSync(timeoutMs: Long = 5000L): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermissions() || !isLocationEnabled()) {
            return@withContext null
        }

        return@withContext withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                try {
                    // Try cached location first (fastest)
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { cachedLocation ->
                            if (cachedLocation != null && isLocationFresh(cachedLocation, 60000L)) {
                                Log.d(TAG, "✅ Using fresh cached location")
                                continuation.resume(cachedLocation)
                                return@addOnSuccessListener
                            }

                            // Request fresh location
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { freshLocation ->
                                    continuation.resume(freshLocation)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Fresh location failed: ${e.message}")
                                    continuation.resume(cachedLocation) // Use cached as fallback
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Location request failed: ${e.message}")
                            continuation.resume(null)
                        }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                }
            }
        }
    }


    // ✅ NEW: Get cached location synchronously
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getCachedLocationSync(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Cached location failed: ${e.message}")
                    continuation.resume(null)
                }
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }

    // ✅ NEW: Get fresh location with high accuracy
    private suspend fun getFreshLocationSync(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            // ✅ CRITICAL: High-accuracy, fast location request
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                HIGH_ACCURACY_INTERVAL_MS
            )
                .setMinUpdateIntervalMillis(500L) // Update every 500ms if possible
                .setMaxUpdateDelayMillis(2000L) // Max 2 second delay
                .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
                .setWaitForAccurateLocation(false) // Don't wait for perfect accuracy
                .build()

            var locationReceived = false
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (!locationReceived) {
                        locationReceived = true
                        val location = locationResult.lastLocation

                        // Stop updates immediately
                        fusedLocationClient.removeLocationUpdates(this)

                        if (location != null) {
                            Log.d(TAG, "✅ Fresh location: ${location.latitude}, ${location.longitude} (${location.accuracy}m)")
                            continuation.resume(location)
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // ✅ Cleanup on cancellation
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }

        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    // ✅ NEW: Check if location is fresh enough
    private fun isLocationFresh(location: Location, maxAgeMs: Long): Boolean {
        val age = System.currentTimeMillis() - location.time
        return age < maxAgeMs
    }
    // ✅ NEW: Get immediate location for trigger evaluation
    suspend fun getLocationForTriggerEvaluation(): Location? {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ STRATEGY: Multi-source location with 3-second timeout
                Log.d(TAG, "🎯 Getting location for trigger evaluation...")

                val location = getCurrentLocationSync(3000L) // 3 second timeout

                if (location != null) {
                    Log.d(TAG, "✅ Trigger location: ${location.latitude}, ${location.longitude} (${location.accuracy}m)")
                    location
                } else {
                    Log.w(TAG, "⚠️ No location available for trigger evaluation")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting trigger location", e)
                null
            }
        }
    }


    //  PUBLIC METHODS 

    fun isLocationAvailable(): Boolean = androidLocationManager != null

    fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    suspend fun startHighAccuracyLocationUpdates(callback: Callback) {
        withContext(Dispatchers.Main) {
            if (!hasLocationPermissions()) {
                callback.onPermissionDenied()
                return@withContext
            }

            if (!isLocationEnabled()) {
                callback.onLocationError("Location services are disabled")
                return@withContext
            }

            try {
                // ✅ CRITICAL: High-frequency, high-accuracy location updates
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    HIGH_ACCURACY_INTERVAL_MS
                )
                    .setMinUpdateIntervalMillis(500L) // Update every 500ms
                    .setMaxUpdateDelayMillis(1000L) // Max 1 second delay
                    .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
                    .setWaitForAccurateLocation(false) // Get location ASAP
                    .build()

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { location ->
                            Log.d(TAG, "🎯 High-accuracy location: ${location.latitude}, ${location.longitude} (${location.accuracy}m)")
                            callback.onLocationReceived(location)
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback!!,
                    Looper.getMainLooper()
                )

                Log.d(TAG, "✅ High-accuracy location updates started")

            } catch (e: SecurityException) {
                callback.onLocationError("Location permission denied: ${e.message}")
            } catch (e: Exception) {
                callback.onLocationError("Error starting high-accuracy updates: ${e.message}")
            }
        }
    }
    fun isLocationEnabled(): Boolean {
        if (!isLocationAvailable()) return false

        return try {
            androidLocationManager!!.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    androidLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking location enabled: ${e.message}")
            false
        }
    }

    fun getLastLocation(callback: Callback) {
        this.callback = callback

        if (!hasLocationPermissions()) {
            callback.onPermissionDenied()
            return
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled")
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        callback.onLocationReceived(location)
                    } else {
                        getLastKnownLocationFallback(callback)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "FusedLocationProvider failed: ${e.message}")
                    getLastKnownLocationFallback(callback)
                }
        } catch (e: SecurityException) {
            callback.onLocationError("Location permission denied: ${e.message}")
        } catch (e: Exception) {
            callback.onLocationError("Error getting location: ${e.message}")
        }
    }

    fun getCurrentLocation(callback: Callback) {
        this.callback = callback

        if (!hasLocationPermissions()) {
            callback.onPermissionDenied()
            return
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled")
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        callback.onLocationReceived(location)
                    } else {
                        callback.onLocationError("Unable to get current location")
                    }
                }
                .addOnFailureListener { e ->
                    callback.onLocationError("Error getting current location: ${e.message}")
                }
        } catch (e: SecurityException) {
            callback.onLocationError("Location permission denied: ${e.message}")
        } catch (e: Exception) {
            callback.onLocationError("Error requesting current location: ${e.message}")
        }
    }

    fun startLocationUpdates(callback: Callback, intervalMs: Long) {
        this.callback = callback

        if (!hasLocationPermissions()) {
            callback.onPermissionDenied()
            return
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled")
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        callback.onLocationReceived(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            callback.onLocationError("Location permission denied: ${e.message}")
        } catch (e: Exception) {
            callback.onLocationError("Error starting location updates: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping location updates: ${e.message}")
            }
        }
    }

    fun cleanup() {
        stopLocationUpdates()
        callback = null
    }

    //  PRIVATE HELPER METHODS 

    private fun getLastKnownLocationFallback(callback: Callback) {
        if (!isLocationAvailable()) {
            callback.onLocationError("Location services not available")
            return
        }

        try {
            var bestLocation: Location? = null

            // Check GPS provider
            if (androidLocationManager!!.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                val gpsLocation = androidLocationManager.getLastKnownLocation(
                    android.location.LocationManager.GPS_PROVIDER
                )
                if (gpsLocation != null) {
                    bestLocation = gpsLocation
                }
            }

            // Check Network provider
            if (androidLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                val networkLocation = androidLocationManager.getLastKnownLocation(
                    android.location.LocationManager.NETWORK_PROVIDER
                )
                if (networkLocation != null) {
                    if (bestLocation == null || networkLocation.time > bestLocation.time) {
                        bestLocation = networkLocation
                    }
                }
            }

            if (bestLocation != null) {
                callback.onLocationReceived(bestLocation)
            } else {
                callback.onLocationError("No cached location available")
            }
        } catch (e: SecurityException) {
            callback.onLocationError("Location permission denied: ${e.message}")
        } catch (e: Exception) {
            callback.onLocationError("Error getting fallback location: ${e.message}")
        }
    }
}
