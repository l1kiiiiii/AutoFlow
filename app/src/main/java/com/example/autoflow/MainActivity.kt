package com.example.autoflow

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsManager
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
import com.example.autoflow.integrations.PhoneStateManager

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

            // ✅ ADD: Check auto-reply system on app start
            checkAllAutoReplyRequirements(this)

            // ✅ ADD: Set flags manually for testing (REMOVE after testing)
            testSetAutoReplyFlags(this)

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

    // Rest of your existing methods remain the same...
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

    // Add to MainActivity.kt or create a utility class
    fun checkAutoReplyPermissions(context: Context): Boolean {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )

        val missing = permissions.filter { permission ->
            ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            Log.e("AutoReplyCheck", "❌ Missing permissions: ${missing.joinToString(", ")}")

            // Show what's missing
            missing.forEach { permission ->
                when (permission) {
                    Manifest.permission.SEND_SMS ->
                        Log.e("AutoReplyCheck", "   📱 SMS Permission: DENIED")
                    Manifest.permission.READ_PHONE_STATE ->
                        Log.e("AutoReplyCheck", "   📞 Phone State Permission: DENIED")
                    Manifest.permission.READ_CALL_LOG ->
                        Log.e("AutoReplyCheck", "   📋 Call Log Permission: DENIED")
                }
            }
            return false
        }

        Log.d("AutoReplyCheck", "✅ All auto-reply permissions granted")
        return true
    }

    // Add to MainActivity.kt to check on app start
    fun checkAllAutoReplyRequirements(context: Context) {
        Log.d("AutoReplyCheck", "🔍 Checking all auto-reply requirements...")

        // 1. Check permissions
        val hasPermissions = checkAutoReplyPermissions(context)

        // 2. Check SharedPreferences
        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        val autoReplyEnabled = prefs.getBoolean("auto_reply_enabled", false)
        val meetingMode = prefs.getBoolean("manual_meeting_mode", false)
        val message = prefs.getString("auto_reply_message", "")

        Log.d("AutoReplyCheck", "📋 SharedPreferences Status:")
        Log.d("AutoReplyCheck", "   auto_reply_enabled: $autoReplyEnabled")
        Log.d("AutoReplyCheck", "   manual_meeting_mode: $meetingMode")
        Log.d("AutoReplyCheck", "   auto_reply_message: '$message'")

        // 3. Check if SMS can be sent
        try {
            val smsManager = SmsManager.getDefault()
            Log.d("AutoReplyCheck", "✅ SMS Manager available")
        } catch (e: Exception) {
            Log.e("AutoReplyCheck", "❌ SMS Manager not available", e)
        }

        // 4. Overall status
        val allGood = hasPermissions && autoReplyEnabled && meetingMode && !message.isNullOrEmpty()
        Log.d("AutoReplyCheck", if (allGood) "✅ Auto-reply fully ready!" else "❌ Auto-reply has issues")
    }

    // Add to MainActivity.kt for testing
    fun testSetAutoReplyFlags(context: Context) {
        Log.d("TestAutoReply", "🧪 Manually setting auto-reply flags for testing...")

        val prefs = context.getSharedPreferences("autoflow_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("auto_reply_enabled", true)
            .putBoolean("manual_meeting_mode", true)
            .putString("auto_reply_message", "I'm currently in a meeting and will get back to you soon.")
            .putBoolean("auto_reply_only_in_dnd", true)
            .apply()

        // Verify immediately
        val verifyEnabled = prefs.getBoolean("auto_reply_enabled", false)
        val verifyMeeting = prefs.getBoolean("manual_meeting_mode", false)
        val verifyMessage = prefs.getString("auto_reply_message", "")

        Log.d("TestAutoReply", "✅ Flags set manually:")
        Log.d("TestAutoReply", "   auto_reply_enabled: $verifyEnabled")
        Log.d("TestAutoReply", "   manual_meeting_mode: $verifyMeeting")
        Log.d("TestAutoReply", "   auto_reply_message: '$verifyMessage'")

        // Start phone monitoring
        val phoneStateManager = PhoneStateManager.getInstance(context)
        phoneStateManager.startListening()

        Toast.makeText(context, "Auto-reply flags set manually - test calling now!", Toast.LENGTH_LONG).show()
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
