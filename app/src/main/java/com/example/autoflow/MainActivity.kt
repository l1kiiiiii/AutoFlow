package com.example.autoflow

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.example.autoflow.ui.theme.AutoFlowTheme
import com.example.autoflow.ui.theme.screens.Dashboard
import com.example.autoflow.util.NotificationHelper
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.WorkflowEntity
import androidx.room.Room
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE_BASIC = 1001
        private const val PERMISSION_REQUEST_CODE_LOCATION = 1002
        private const val PERMISSION_REQUEST_CODE_BACKGROUND_LOCATION = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // ✅ Initialize notification channels
            try {
                NotificationHelper.createNotificationChannels(this)
                Log.d(TAG, "✅ Notification channels created")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create notification channels", e)
                showError("Failed to initialize notifications: ${e.message}")
            }



            // ✅ Request permissions
            checkAndRequestAllPermissions()

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

    // ✅ FIXED: Cleanup function with proper imports and database access
    private fun cleanupDuplicateMeetingModes() {
        GlobalScope.launch {
            try {
                // ✅ Create database instance with proper name
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "autoflow_database"  // Use your actual database name
                ).build()

                // ✅ FIXED: Use suspending function instead of LiveData
                val dao = db.workflowDao()

                // You need to add a suspending version to your DAO:
                // @Query("SELECT * FROM workflows")
                // suspend fun getAllWorkflowsSync(): List<WorkflowEntity>

                // ✅ ALTERNATIVE: Use existing method if you have it
                val allWorkflows = dao.getAllWorkflowsSync() // Suspending version

                val meetingModes = allWorkflows.filter { workflow ->
                    workflow.workflowName.contains("Meeting Mode", ignoreCase = true)
                }

                if (meetingModes.size > 1) {
                    Log.d(TAG, "🧹 Found ${meetingModes.size} duplicate Meeting Modes to clean up")

                    // Keep only the most recent one, delete the rest
                    val latestMeetingMode = meetingModes.maxByOrNull { workflow -> workflow.id }
                    val duplicatesToDelete = meetingModes.filter { workflow ->
                        workflow.id != latestMeetingMode?.id
                    }

                    duplicatesToDelete.forEach { workflow ->
                        dao.delete(workflow)
                        Log.d(TAG, "🗑️ Deleted duplicate Meeting Mode: ID ${workflow.id}")
                    }

                    Log.d(TAG, "✅ Cleaned up ${duplicatesToDelete.size} duplicate Meeting Modes")

                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "🧹 Cleaned up ${duplicatesToDelete.size} duplicate Meeting Modes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Close database
                db.close()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cleaning up duplicates", e)
            }
        }
    }

    private fun checkAndRequestAllPermissions() {
        val basicPermissions = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            basicPermissions.add(Manifest.permission.SEND_SMS)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            basicPermissions.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            basicPermissions.add(Manifest.permission.READ_CALL_LOG)
        }

        if (basicPermissions.isNotEmpty()) {
            Log.d(TAG, "🔍 Requesting basic permissions: ${basicPermissions.joinToString(", ")}")
            ActivityCompat.requestPermissions(this, basicPermissions.toTypedArray(), PERMISSION_REQUEST_CODE_BASIC)
        } else {
            checkLocationPermissions()
        }
    }

    private fun checkLocationPermissions() {
        val locationPermissions = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (locationPermissions.isNotEmpty()) {
            Log.d(TAG, "🌍 Requesting location permissions: ${locationPermissions.joinToString(", ")}")
            Toast.makeText(this, "📍 Location permissions needed for location-based workflows", Toast.LENGTH_LONG).show()
            ActivityCompat.requestPermissions(this, locationPermissions.toTypedArray(), PERMISSION_REQUEST_CODE_LOCATION)
        } else {
            checkBackgroundLocationPermission()
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "🌍📱 Requesting background location permission")
                Toast.makeText(this, "📍 Background location needed for geofences when app is closed", Toast.LENGTH_LONG).show()

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    PERMISSION_REQUEST_CODE_BACKGROUND_LOCATION
                )
            } else {
                Log.d(TAG, "✅ Background location permission already granted")
                enableAutoReplyForTesting()
            }
        } else {
            Log.d(TAG, "✅ Background location not needed on Android < 10")
            enableAutoReplyForTesting()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE_BASIC -> {
                handleBasicPermissionResults(permissions, grantResults)
            }
            PERMISSION_REQUEST_CODE_LOCATION -> {
                handleLocationPermissionResults(permissions, grantResults)
            }
            PERMISSION_REQUEST_CODE_BACKGROUND_LOCATION -> {
                handleBackgroundLocationPermissionResult(permissions, grantResults)
            }
        }
    }

    private fun handleBasicPermissionResults(permissions: Array<String>, grantResults: IntArray) {
        var allGranted = true
        permissions.forEachIndexed { index, permission ->
            val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "📋 Basic Permission $permission: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
            if (!granted) allGranted = false
        }

        if (allGranted) {
            Log.d(TAG, "✅ All basic permissions granted")
            checkLocationPermissions()
        } else {
            Log.w(TAG, "⚠️ Some basic permissions denied")
            Toast.makeText(this, "⚠️ Some permissions denied - features may not work", Toast.LENGTH_LONG).show()
            checkLocationPermissions()
        }
    }

    private fun handleLocationPermissionResults(permissions: Array<String>, grantResults: IntArray) {
        var locationGranted = false
        permissions.forEachIndexed { index, permission ->
            val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "🌍 Location Permission $permission: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
            if (granted && (permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                        permission == Manifest.permission.ACCESS_COARSE_LOCATION)) {
                locationGranted = true
            }
        }

        if (locationGranted) {
            Log.d(TAG, "✅ Location permissions granted")
            checkBackgroundLocationPermission()
        } else {
            Log.w(TAG, "⚠️ Location permissions denied - geofencing won't work")
            Toast.makeText(this, "⚠️ Location denied - location-based workflows disabled", Toast.LENGTH_LONG).show()
            enableAutoReplyForTesting()
        }
    }

    private fun handleBackgroundLocationPermissionResult(permissions: Array<String>, grantResults: IntArray) {
        val backgroundGranted = grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED

        if (backgroundGranted) {
            Log.d(TAG, "✅ Background location permission granted")
            Toast.makeText(this, "✅ All permissions granted - full functionality enabled", Toast.LENGTH_LONG).show()
        } else {
            Log.w(TAG, "⚠️ Background location permission denied")
            Toast.makeText(this, "⚠️ Background location denied - geofences won't work when app is closed", Toast.LENGTH_LONG).show()
        }

        enableAutoReplyForTesting()
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
                Toast.makeText(this, "⏰ Please allow 'Alarms & reminders' permission for scheduled workflows", Toast.LENGTH_LONG).show()

                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
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

    private fun fallbackToAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(intent)
            Toast.makeText(this, "Please enable all permissions for AutoFlow in app settings", Toast.LENGTH_LONG).show()
            Log.d(TAG, "✅ Opened fallback app settings")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open app settings", e)
            showError("Cannot open settings. Please manually enable permissions.")
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, "❌ $message", Toast.LENGTH_LONG).show()
        }
        Log.e(TAG, "Error shown to user: $message")
    }

    override fun onDestroy() {
        try {
            Log.d(TAG, "🧹 MainActivity destroyed")
            super.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onDestroy", e)
            super.onDestroy()
        }
    }

    override fun onResume() {
        try {
            super.onResume()

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

    fun requestUsageStatsPermission() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please enable 'Usage Access' for AutoFlow to block apps", Toast.LENGTH_LONG).show()
            Log.d(TAG, "✅ Opened usage stats permission settings")
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "❌ Usage stats settings not available", e)
            showError("Usage stats settings not available on this device")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error requesting usage stats permission", e)
            showError("Failed to open usage stats settings")
        }
    }

    private fun enableAutoReplyForTesting() {
        try {
            val prefs = getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("auto_reply_enabled", true)
                .putString("auto_reply_message", "I'm in a meeting. Will call you back soon!")
                .putBoolean("auto_reply_only_in_dnd", true)
                .apply()

            Log.d(TAG, "🔧 Auto-reply enabled for testing:")
            Log.d(TAG, "   📱 Enabled: true")
            Log.d(TAG, "   💬 Message: 'I'm in a meeting. Will call you back soon!'")
            Log.d(TAG, "   🔇 Only in DND: true")

            Toast.makeText(this, "Auto-reply SMS enabled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enabling auto-reply", e)
        }
    }
}
