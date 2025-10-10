// GeofenceReceiver.kt (handles geofence transitions)
package com.example.autoflow.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.autoflow.policy.BlockPolicy
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e("GeofenceReceiver", "Geofence error: ${event.errorCode}")
            return
        }

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                BlockPolicy.setBlockingEnabled(context, true)
                Log.i("GeofenceReceiver", "🚫 App blocking enabled (entered restricted zone)")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                BlockPolicy.setBlockingEnabled(context, false)
                Log.i("GeofenceReceiver", "✅ App blocking disabled (exited restricted zone)")
            }
        }
    }
}
