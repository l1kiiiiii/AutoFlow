package com.example.autoflow.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {
    private const val TAG = "GeofenceManager"

    // Constants
    const val MAX_GEOFENCES = 100  // Android limit
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
        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "❌ Location permission not granted")
            return false
        }
        if (workflowId <= 0L) {
            Log.e(TAG, "❌ Invalid workflowId ($workflowId). Aborting geofence registration.")
            return false
        }

        try {
            val geofencingClient = LocationServices.getGeofencingClient(context)

            // FIXED: Use actual workflow ID (not "workflow_0")
            val requestId = "workflow_$workflowId"

            // Determine transition types
            val transitionTypes = when {
                triggerOnEntry && triggerOnExit -> Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                triggerOnEntry -> Geofence.GEOFENCE_TRANSITION_ENTER
                triggerOnExit -> Geofence.GEOFENCE_TRANSITION_EXIT
                else -> Geofence.GEOFENCE_TRANSITION_ENTER // Default
            }

            // Build geofence
            val geofence = Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionTypes)
                .build()

            // Build request
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            // Create pending intent - FIXED for Android 12+
            val intent = Intent(context, GeofenceReceiver::class.java)

            // CRITICAL FIX: Use correct flags based on Android version
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ (API 31+) requires explicit mutability
                PendingIntent.getBroadcast(
                    context,
                    workflowId.toInt(), // Use workflow ID as request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                // Android 11 and below
                PendingIntent.getBroadcast(
                    context,
                    workflowId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Add geofence
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    activeGeofences.add(requestId)
                    Log.d(TAG, "✅ Geofence added: $requestId at ($latitude, $longitude) radius=${radius}m")
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
