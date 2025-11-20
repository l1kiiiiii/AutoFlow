package com.example.autoflow.domain.trigger

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Handler for location-based triggers
 * Follows Single Responsibility Principle

class LocationTriggerHandler(private val context: Context) : TriggerHandler {
    
    private val locationManager = LocationManager(context)
    
    override fun canHandle(trigger: Trigger): Boolean {
        return trigger.type == Constants.TRIGGER_LOCATION
    }
    
    override fun getSupportedType(): String = Constants.TRIGGER_LOCATION
    
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override suspend fun evaluate(trigger: Trigger): Result<Boolean> {
        try {
            // Check permissions
            if (!PermissionUtils.hasLocationPermissions(context)) {
                return Result.failure(SecurityException("Location permissions not granted"))
            }
            
            // Parse location data from trigger
            val locationData = TriggerParser.parseLocationData(trigger)
                ?: return Result.failure(IllegalArgumentException("Invalid location data"))
            
            // Get current location
            val currentLocation = suspendCancellableCoroutine<Location?> { continuation ->
                locationManager.getLastLocation(object : LocationManager.Callback {
                    override fun onLocationReceived(location: Location) {
                        continuation.resume(location)
                    }
                    
                    override fun onLocationError(errorMessage: String) {
                        continuation.resume(null)
                    }
                    
                    override fun onPermissionDenied() {
                        continuation.resume(null)
                    }
                })
            }
            
            if (currentLocation == null) {
                return Result.failure(IllegalStateException("Unable to get current location"))
            }
            
            // Calculate distance
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                locationData.latitude,
                locationData.longitude,
                results
            )
            
            val distance = results[0]
            val isInRange = distance <= locationData.radius
            
            Result.success(isInRange)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
 */