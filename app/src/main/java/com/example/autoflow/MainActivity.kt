package com.example.autoflow

import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.ui.theme.screens.Dashboard
import com.example.autoflow.util.NotificationHelper

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // ✅ Initialize notification channels with error handling
            try {
                NotificationHelper.createNotificationChannels(this)
                Log.d(TAG, "✅ Notification channels created")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create notification channels", e)
                showError("Failed to initialize notifications: ${e.message}")
            }

            // ✅ Request exact alarm permission for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestExactAlarmPermission()
            }
            enableEdgeToEdge()
            setContent {
                AutoFlowTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Dashboard()
                    }
                }
            }
            Log.d(TAG, "✅ MainActivity created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in onCreate", e)
            showError("App initialization failed: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestExactAlarmPermission() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager

            if (alarmManager == null) {
                Log.e(TAG, "❌ AlarmManager service unavailable")
                showError("Alarm service unavailable")
                return
            }

            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "⏰ Please allow 'Alarms & reminders' permission for scheduled workflows",
                    Toast.LENGTH_LONG
                ).show()

                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        // ✅ Using KTX extension function
                        data = "package:$packageName".toUri()
                    }
                    startActivity(intent)
                    Log.d(TAG, "✅ Navigated to exact alarm permission settings")
                } catch (e: ActivityNotFoundException) {
                    Log.w(TAG, "⚠️ Exact alarm settings not available, using fallback")
                    fallbackToAppSettings()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error opening alarm permission settings", e)
                    fallbackToAppSettings()
                }
            } else {
                Log.d(TAG, "✅ Exact alarm permission already granted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error requesting alarm permission", e)
            showError("Permission request failed: ${e.message}")
        }
    }

    /**
     * Fallback to app settings when exact alarm settings unavailable
     */
    private fun fallbackToAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                // ✅ Using KTX extension function
                data = "package:$packageName".toUri()
            }
            startActivity(intent)

            Toast.makeText(
                this,
                "Please enable all permissions for AutoFlow in app settings",
                Toast.LENGTH_LONG
            ).show()

            Log.d(TAG, "✅ Opened fallback app settings")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open app settings", e)
            showError("Cannot open settings. Please manually enable permissions.")
        }
    }

    /**
     * Show error messages to user
     */
    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, "❌ $message", Toast.LENGTH_LONG).show()
        }
        Log.e(TAG, "Error shown to user: $message")
    }

    /**
     * Enhanced lifecycle management with error handling
     */
    override fun onDestroy() {
        try {
            Log.d(TAG, "🧹 MainActivity destroyed")
            super.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onDestroy", e)
            super.onDestroy()
        }
    }

    /**
     * Enhanced lifecycle management with error handling
     */
    override fun onResume() {
        try {
            super.onResume()

            // Check if exact alarm permission is still granted (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager?.canScheduleExactAlarms() == false) {
                    Log.w(TAG, "⚠️ Exact alarm permission revoked")
                }
            }

            Log.d(TAG, "✅ MainActivity resumed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onResume", e)
            super.onResume()
        }
    }

    /**
     * ✅ UTILITY METHODS - Only keep if used elsewhere in the app
     * These can be called from other parts of the app when needed
     */

    /**
     * Check if Usage Stats permission is granted
     * Call this method when you need to check app blocking permissions
     */
    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            val hasPermission = mode == AppOpsManager.MODE_ALLOWED
            Log.d(TAG, "Usage stats permission: $hasPermission")
            hasPermission
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking usage stats permission", e)
            false
        }
    }

    /**
     * Request Usage Stats permission with error handling
     * Call this method when you need to enable app blocking features
     */
    fun requestUsageStatsPermission() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)

            Toast.makeText(
                this,
                "Please enable 'Usage Access' for AutoFlow to block apps",
                Toast.LENGTH_LONG
            ).show()

            Log.d(TAG, "✅ Opened usage stats permission settings")
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "❌ Usage stats settings not available", e)
            showError("Usage stats settings not available on this device")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error requesting usage stats permission", e)
            showError("Failed to open usage stats settings")
        }
    }
}
