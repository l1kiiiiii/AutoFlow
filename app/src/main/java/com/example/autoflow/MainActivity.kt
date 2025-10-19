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

            // ✅ Check and request SMS/Phone permissions FIRST
            checkAndRequestPermissions()

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

    // ✅ FIXED: SMS and Phone permission checking
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }

        // ✅ ADD: Request call log permission for getting caller numbers
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CALL_LOG)
        }

        if (permissions.isNotEmpty()) {
            Log.d(TAG, "🔍 Requesting permissions: ${permissions.joinToString(", ")}")
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
        } else {
            Log.d(TAG, "✅ All permissions already granted")
            enableAutoReplyForTesting()
        }
    }


    // ✅ FIXED: Correct method signature with override
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, // ✅ FIXED: Changed from Array<out String> to Array<String>
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            var allGranted = true

            permissions.forEachIndexed { index, permission ->
                val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "📋 Permission $permission: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
                if (!granted) allGranted = false
            }

            if (allGranted) {
                Log.d(TAG, "✅ All auto-reply permissions granted")
                enableAutoReplyForTesting()
            } else {
                Log.w(TAG, "⚠️ Some permissions denied - auto-reply may not work")
                Toast.makeText(
                    this,
                    "SMS permissions needed for auto-reply feature",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ✅ Enable auto-reply for testing
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
