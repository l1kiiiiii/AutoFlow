package com.example.autoflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.autoflow.policy.BlockPolicy

// ✅ IMPROVED: Emergency unblock with context
class EmergencyUnblockReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "EmergencyUnblock"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🚨 Emergency unblock triggered")

        val blockedApps = BlockPolicy.getBlockedPackages(context)
        if (blockedApps.isEmpty()) {
            Log.d(TAG, "⚠️ No apps currently blocked")
            return
        }

        // Show confirmation dialog or directly unblock
        when (intent.action) {
            "EMERGENCY_UNBLOCK_ALL" -> {
                BlockPolicy.emergencyUnblockWithContext(context, "Emergency button pressed")

                // Show toast
                Toast.makeText(context, "🚨 Emergency unblock activated", Toast.LENGTH_LONG).show()
            }
            "UNBLOCK_SPECIFIC" -> {
                val packageName = intent.getStringExtra("package_name")
                if (!packageName.isNullOrEmpty()) {
                    BlockPolicy.removeBlockedPackages(context, listOf(packageName))
                    Toast.makeText(context, "✅ App unblocked", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

