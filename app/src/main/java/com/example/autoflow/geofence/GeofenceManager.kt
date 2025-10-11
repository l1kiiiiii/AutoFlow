// GeofenceManager.kt (improved version)
package com.example.autoflow.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {
    private const val TAG = "GeofenceManager"
    private const val GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE
    private const val MIN_RADIUS_METERS = 100f  // Android recommended minimum

    /**
     * Add geofence for a workflow
     * @param workflowId Unique workflow identifier
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param radius Geofence radius in meters (minimum 100m recommended)
     * @param triggerOnEntry True to trigger when entering the area
     * @param triggerOnExit True to trigger when exiting the area
     */
    fun addGeofence(
        context: Context,
        workflowId: Long,
        latitude: Double,
        longitude: Double,
        radius: Float,
        triggerOnEntry: Boolean,
        triggerOnExit: Boolean
    ): Boolean {
        Log.d(TAG, "🌍 Adding geofence for workflow $workflowId")
        Log.d(TAG, "   Location: $latitude, $longitude")

        // Enforce minimum radius for reliability
        val adjustedRadius = if (radius < MIN_RADIUS_METERS) {
            Log.w(TAG, "⚠️ Radius ${radius}m is below recommended minimum. Adjusting to ${MIN_RADIUS_METERS}m")
            MIN_RADIUS_METERS
        } else {
            radius
        }

        Log.d(TAG, "   Radius: ${adjustedRadius}m")
        Log.d(TAG, "   Triggers: Entry=$triggerOnEntry, Exit=$triggerOnExit")

        // Check permissions
        if (!hasLocationPermission(context)) {
            Log.e(TAG, "❌ Location permission not granted")
            return false
        }

        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

        // Calculate transition types
        var transitionTypes = 0
        if (triggerOnEntry) {
            transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_ENTER
        }
        if (triggerOnExit) {
            transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_EXIT
        }

        if (transitionTypes == 0) {
            Log.e(TAG, "❌ No transition types specified")
            return false
        }

        // Create geofence
        val geofence = Geofence.Builder()
            .setRequestId("workflow_$workflowId")
            .setCircularRegion(latitude, longitude, adjustedRadius)
            .setExpirationDuration(GEOFENCE_EXPIRATION)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(5000)  // 5 seconds before DWELL triggers
            .build()

        // Build geofencing request
        // Use 0 to disable initial triggers - only trigger on actual transitions
        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(0)  // Don't trigger if already inside/outside
            addGeofence(geofence)
        }.build()

        // Create pending intent
        val geofencePendingIntent = getGeofencePendingIntent(context)

        // Add geofence
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "❌ Location permission check failed")
                return false
            }

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d(TAG, "✅ Geofence added successfully for workflow $workflowId")
                }
                addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to add geofence: ${e.message}", e)
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception adding geofence: ${e.message}", e)
            return false
        }
    }

    /**
     * Remove geofence for a workflow
     */
    fun removeGeofence(context: Context, workflowId: Long) {
        Log.d(TAG, "🗑️ Removing geofence for workflow $workflowId")

        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
        val requestIds = listOf("workflow_$workflowId")

        geofencingClient.removeGeofences(requestIds).run {
            addOnSuccessListener {
                Log.d(TAG, "✅ Removed geofence for workflow $workflowId")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to remove geofence: ${e.message}", e)
            }
        }
    }

    /**
     * Remove all geofences
     */
    fun removeAllGeofences(context: Context) {
        Log.d(TAG, "🗑️ Removing all geofences")

        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
        val pendingIntent = getGeofencePendingIntent(context)

        geofencingClient.removeGeofences(pendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "✅ All geofences removed")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to remove geofences: ${e.message}", e)
            }
        }
    }

    /**
     * Get pending intent for geofence broadcasts
     */
    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
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
}
