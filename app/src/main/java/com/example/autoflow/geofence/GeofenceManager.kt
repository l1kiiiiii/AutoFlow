// GeofenceManager.kt (manages geofence registration)

package com.example.autoflow.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {
    private const val GEOFENCE_ID = "app-block-zone"

    fun registerGeofence(ctx: Context, lat: Double, lng: Double, radiusMeters: Float) {
        val client: GeofencingClient = LocationServices.getGeofencingClient(ctx)
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(lat, lng, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val pendingIntent = PendingIntent.getBroadcast(
            ctx, 0,
            Intent(ctx, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        client.addGeofences(request, pendingIntent)
            .addOnSuccessListener {
                Log.i("GeofenceManager", "Geofence registered successfully")
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to register geofence", e)
            }
    }

    fun removeGeofences(ctx: Context) {
        val client = LocationServices.getGeofencingClient(ctx)
        client.removeGeofences(listOf(GEOFENCE_ID))
    }
}
