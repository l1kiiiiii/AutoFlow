package com.example.autoflow.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.autoflow.integrations.LocationManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ✅ ULTRA-RESPONSIVE GeofenceManager - Instant triggering with zero delays
 */
object GeofenceManager {
    private const val TAG = "GeofenceManager"

    // Constants
    const val MAX_GEOFENCES = 100
    private const val MIN_GEOFENCE_RADIUS = 20f
    private const val MAX_GEOFENCE_RADIUS = 1000f
    private const val RESPONSIVENESS_MS = 0 // ✅ INSTANT: 0ms delay = immediate response
    private const val LOITERING_DELAY_MS = 1000 // ✅ 1 second dwell (minimum allowed)
    private val activeGeofences = mutableSetOf<String>()

    /**
     * ✅ Add ultra-responsive geofence with instant triggering
     */
    fun addGeofence(
        context: Context,
        workflowId: Long,
        latitude: Double,
        longitude: Double,
        radius: Float,
        triggerOnEntry: Boolean = true,
        triggerOnExit: Boolean = true
    ): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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

            val optimizedRadius = when {
                radius < MIN_GEOFENCE_RADIUS -> MIN_GEOFENCE_RADIUS
                radius > MAX_GEOFENCE_RADIUS -> MAX_GEOFENCE_RADIUS
                else -> radius
            }

            // ✅ Monitor ENTER + EXIT + DWELL
            val transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT or
                    Geofence.GEOFENCE_TRANSITION_DWELL

            Log.d(TAG, "⚡ Creating INSTANT-RESPONSE geofence:")
            Log.d(TAG, "   📍 Location: ($latitude, $longitude)")
            Log.d(TAG, "   🎯 Radius: ${optimizedRadius}m")
            Log.d(TAG, "   🚪 Monitoring: ENTER + EXIT + DWELL")
            Log.d(TAG, "   ⚡ Responsiveness: INSTANT (${RESPONSIVENESS_MS}ms)")
            Log.d(TAG, "   ⏱️ Dwell delay: ${LOITERING_DELAY_MS}ms")

            // ✅ Create INSTANT-RESPONSE geofence
            val geofence = Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(latitude, longitude, optimizedRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionTypes)
                .setNotificationResponsiveness(RESPONSIVENESS_MS) // ✅ 0ms = instant
                .setLoiteringDelay(LOITERING_DELAY_MS) // ✅ 1 second minimum
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofence(geofence)
                .build()

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

            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    activeGeofences.add(requestId)
                    Log.d(TAG, "✅ ⚡ INSTANT-RESPONSE Geofence registered: $requestId")
                    Log.d(TAG, "   Ready for immediate re-triggering on every entry")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to add geofence: ${e.message}", e)
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
     * ✅ Validate current location
     */
    suspend fun validateCurrentLocationForWorkflow(
        context: Context,
        workflowId: Long,
        targetLatitude: Double,
        targetLongitude: Double,
        radius: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val locationManager = LocationManager(context)
            val currentLocation = locationManager.getCurrentLocationSync(3000L)

            if (currentLocation == null) {
                Log.e(TAG, "❌ Cannot get current location")
                return@withContext false
            }

            val targetLocation = Location("target").apply {
                latitude = targetLatitude
                longitude = targetLongitude
            }

            val distance = currentLocation.distanceTo(targetLocation)
            val isWithinRadius = distance <= radius

            Log.d(TAG, "📏 Distance: ${distance.toInt()}m (radius: ${radius.toInt()}m) - ${if (isWithinRadius) "✅ INSIDE" else "❌ OUTSIDE"}")

            isWithinRadius

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating location", e)
            false
        }
    }

    /**
     * Remove a geofence
     */
    fun removeGeofence(context: Context, workflowId: Long) {
        try {
            val geofencingClient = LocationServices.getGeofencingClient(context)
            val requestId = "workflow_$workflowId"

            geofencingClient.removeGeofences(listOf(requestId))
                .addOnSuccessListener {
                    activeGeofences.remove(requestId)
                    clearGeofenceState(context, workflowId)
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
            val geofencingClient = LocationServices.getGeofencingClient(context)

            geofencingClient.removeGeofences(activeGeofences.toList())
                .addOnSuccessListener {
                    val count = activeGeofences.size
                    activeGeofences.clear()
                    clearAllGeofenceStates(context)
                    Log.d(TAG, "✅ Removed $count geofences")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to remove all geofences: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error removing all geofences: ${e.message}", e)
        }
    }

    private fun clearGeofenceState(context: Context, workflowId: Long) {
        try {
            val prefs = context.getSharedPreferences("geofence_states", Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove("location_state_${workflowId}_current")
                remove("location_state_${workflowId}_previous")
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing geofence state", e)
        }
    }

    private fun clearAllGeofenceStates(context: Context) {
        try {
            val prefs = context.getSharedPreferences("geofence_states", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing all states", e)
        }
    }

    fun getActiveGeofenceCount(): Int = activeGeofences.size

    fun hasGeofence(workflowId: Long): Boolean {
        return activeGeofences.contains("workflow_$workflowId")
    }
}
