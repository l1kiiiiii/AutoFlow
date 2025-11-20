package com.example.autoflow.domain.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for permission management
 * Handles all runtime permission checks for AutoFlow
 */
object PermissionUtils {

    private const val TAG = "PermissionUtils"
    //  PERMISSION REQUEST CODES
    const val REQUEST_LOCATION_PERMISSION = 1001
    const val REQUEST_BLUETOOTH_PERMISSION = 1002
    const val REQUEST_NOTIFICATION_PERMISSION = 1003
    const val REQUEST_WIFI_PERMISSION = 1004
    const val REQUEST_POST_NOTIFICATIONS = 1005

    //  LOCATION PERMISSIONS 

    fun hasLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_LOCATION_PERMISSION
        )
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below Android 10
        }
    }

    fun getLocationPermissionsForGeofencing(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    fun requestBackgroundLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    //  BLUETOOTH PERMISSIONS 

    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires new Bluetooth permissions
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 and below
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED &&
                    hasLocationPermissions(context) // BLE requires location on older versions
        }
    }

    fun requestBluetoothPermissions(activity: Activity) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        ActivityCompat.requestPermissions(
            activity,
            permissions,
            REQUEST_BLUETOOTH_PERMISSION
        )
    }

    //  NOTIFICATION PERMISSIONS 

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed on older versions
        }
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS
            )
        }
    }

    //  WIFI PERMISSIONS 

    fun hasWifiPermissions(context: Context): Boolean {
        val hasAccessWifiState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val hasChangeWifiState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CHANGE_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        // Android 10+ requires location for WiFi SSID
        val hasLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasLocationPermissions(context)
        } else {
            true
        }

        return hasAccessWifiState && hasChangeWifiState && hasLocation
    }

    fun requestWifiPermissions(activity: Activity) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        // Add location permission for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        ActivityCompat.requestPermissions(
            activity,
            permissions.toTypedArray(),
            REQUEST_WIFI_PERMISSION
        )
    }

    //  DO NOT DISTURB PERMISSION 

    fun hasDndPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as? android.app.NotificationManager
            notificationManager?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    //  ALL PERMISSIONS 

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasLocationPermissions(context) &&
                hasBluetoothPermissions(context) &&
                hasNotificationPermission(context) &&
                hasWifiPermissions(context)
    }

    fun requestAllPermissions(activity: Activity) {
        val allPermissions = mutableListOf<String>()

        // Location
        allPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        allPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            allPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            allPermissions.add(Manifest.permission.BLUETOOTH)
            allPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            allPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // WiFi
        allPermissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        allPermissions.add(Manifest.permission.CHANGE_WIFI_STATE)

        ActivityCompat.requestPermissions(
            activity,
            allPermissions.toTypedArray(),
            REQUEST_LOCATION_PERMISSION
        )
    }

    //  PERMISSION RATIONALE 

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION ->
                "Location permission is needed for location-based automation"

            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH ->
                "Bluetooth permission is needed to detect nearby devices"

            Manifest.permission.POST_NOTIFICATIONS ->
                "Notification permission is needed to send you automation alerts"

            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE ->
                "WiFi permission is needed for WiFi-based automation"

            else -> "This permission is required for the app to function"
        }
    }
}
