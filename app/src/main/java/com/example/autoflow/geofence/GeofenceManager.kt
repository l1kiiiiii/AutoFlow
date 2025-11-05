package com.example.autoflow.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location // ✅ FIXED: Use correct Location import
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.autoflow.integrations.LocationManager // ✅ ADD: Import your LocationManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeofenceManager {
    private const val TAG = "GeofenceManager"

    // Constants
    const val MAX_GEOFENCES = 100
    private const val MIN_GEOFENCE_RADIUS = 20f // 20 meters minimum
    private const val MAX_GEOFENCE_RADIUS = 1000f // 1km maximum
    private const val RESPONSIVENESS_MS = 10000
    private val activeGeofences = mutableSetOf<String>()  // Track active geofence IDs

    /**
     * Add a geofence for a workflow
     */
    fun addGeofence(
        context: Context,
        workflowId: Long,
        latitude: Double,
        longitude: Double,
        radius: Float,
        triggerOnEntry: Boolean = true,
        triggerOnExit: Boolean = false
    ): Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ Location permission not granted")
            return false
        }

        if (workflowId <= 0L) {
            Log.e(TAG, "❌ Invalid workflowId ($workflowId)")
            return false
        }

        try {
            val geofencingClient = LocationServices.getGeofencingClient(context)
            val requestId = "workflow_$workflowId"

            // ✅ CRITICAL: Optimize radius for better responsiveness
            val optimizedRadius = when {
                radius < MIN_GEOFENCE_RADIUS -> {
                    Log.w(TAG, "⚠️ Radius too small ($radius), using minimum ${MIN_GEOFENCE_RADIUS}m")
                    MIN_GEOFENCE_RADIUS
                }
                radius > MAX_GEOFENCE_RADIUS -> {
                    Log.w(TAG, "⚠️ Radius too large ($radius), using maximum ${MAX_GEOFENCE_RADIUS}m")
                    MAX_GEOFENCE_RADIUS
                }
                else -> radius
            }

            // Determine transition types
            val transitionTypes = when {
                triggerOnEntry && triggerOnExit ->
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                triggerOnEntry -> Geofence.GEOFENCE_TRANSITION_ENTER
                triggerOnExit -> Geofence.GEOFENCE_TRANSITION_EXIT
                else -> Geofence.GEOFENCE_TRANSITION_ENTER
            }

            // ✅ CRITICAL: Create responsive geofence
            val geofence = Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(latitude, longitude, optimizedRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionTypes)
                .setNotificationResponsiveness(RESPONSIVENESS_MS) // ✅ KEY: 10-second response
                .setLoiteringDelay(5000) // ✅ 5-second dwell time
                .build()

            // ✅ CRITICAL: Enhanced geofencing request
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(
                    when {
                        triggerOnEntry -> GeofencingRequest.INITIAL_TRIGGER_ENTER
                        triggerOnExit -> GeofencingRequest.INITIAL_TRIGGER_EXIT
                        else -> GeofencingRequest.INITIAL_TRIGGER_ENTER
                    }
                )
                .addGeofence(geofence)
                .build()

            // ✅ Create pending intent with proper flags
            val intent = Intent(context, GeofenceReceiver::class.java)
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    context,
                    workflowId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    context,
                    workflowId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Add geofence with enhanced logging
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    activeGeofences.add(requestId)
                    Log.d(TAG, "✅ RESPONSIVE Geofence added: $requestId")
                    Log.d(TAG, "   📍 Location: ($latitude, $longitude)")
                    Log.d(TAG, "   🎯 Radius: ${optimizedRadius}m")
                    Log.d(TAG, "   ⚡ Responsiveness: ${RESPONSIVENESS_MS}ms")
                    Log.d(TAG, "   🚪 Entry: $triggerOnEntry, Exit: $triggerOnExit")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to add responsive geofence: ${e.message}", e)
                }

            return true

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException: ${e.message}", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding geofence: ${e.message}", e)
            return false
        }
    }

    /**
     * ✅ FIXED: Validate current location with proper Location class
     */
    suspend fun validateCurrentLocationForWorkflow(
        context: Context, // ✅ ADD: Missing context parameter
        workflowId: Long,
        targetLatitude: Double,
        targetLongitude: Double,
        radius: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Validating current location for workflow $workflowId")

            // ✅ FIXED: Use your LocationManager class to get current location
            val locationManager = LocationManager(context)
            val currentLocation = locationManager.getCurrentLocationSync(3000L) // 3-second timeout

            if (currentLocation == null) {
                Log.e(TAG, "❌ Cannot validate - no current location available")
                return@withContext false
            }

            // ✅ FIXED: Create proper Location object
            val targetLocation = Location("target").apply {
                latitude = targetLatitude
                longitude = targetLongitude
            }

            val distance = currentLocation.distanceTo(targetLocation)
            val isWithinRadius = distance <= radius

            Log.d(TAG, "📍 Current: ${currentLocation.latitude}, ${currentLocation.longitude}")
            Log.d(TAG, "🎯 Target: $targetLatitude, $targetLongitude")
            Log.d(TAG, "📏 Distance: ${distance.toInt()}m (radius: ${radius.toInt()}m)")
            Log.d(TAG, "✅ Within radius: ${if (isWithinRadius) "YES" else "NO"}")

            isWithinRadius

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating location for workflow", e)
            false
        }
    }

    /**
     * Remove a geofence for a workflow
     */
    fun removeGeofence(context: Context, workflowId: Long) {
        try {
            val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
            val requestId = "workflow_$workflowId"

            geofencingClient.removeGeofences(listOf(requestId))
                .addOnSuccessListener {
                    activeGeofences.remove(requestId)
                    Log.d(TAG, "✅ Geofence removed: $requestId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to remove geofence: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error removing geofence: ${e.message}", e)
        }
    }

    /**
     * Remove all geofences
     */
    fun removeAllGeofences(context: Context) {
        try {
            val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

            geofencingClient.removeGeofences(activeGeofences.toList())
                .addOnSuccessListener {
                    val count = activeGeofences.size
                    activeGeofences.clear()
                    Log.d(TAG, "✅ Removed $count geofences")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to remove all geofences: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error removing all geofences: ${e.message}", e)
        }
    }

    /**
     * Check if location permissions are granted
     */
    private fun hasLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get count of active geofences
     */
    fun getActiveGeofenceCount(): Int = activeGeofences.size

    /**
     * Check if a specific workflow has an active geofence
     */
    fun hasGeofence(workflowId: Long): Boolean {
        return activeGeofences.contains("workflow_$workflowId")
    }
}
