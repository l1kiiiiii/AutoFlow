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

class LocationManager(context: Context) {

    private val context: Context = context.applicationContext
    private val androidLocationManager: android.location.LocationManager? =
        this.context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(this.context)
    private var callback: Callback? = null
    private var locationCallback: LocationCallback? = null

    companion object {
        private const val TAG = "AutoFlow-LocationManager"
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
