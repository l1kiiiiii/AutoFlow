package com.example.autoflow.integrations

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat

class BLEManager(context: Context) {

    private val context: Context = context.applicationContext
    private val bluetoothAdapter: BluetoothAdapter?
    private var callback: BLECallback? = null
    private var isReceiverRegistered = false

    companion object {
        private const val TAG = "BLEManager"
    }

    init {
        val bluetoothManager = this.context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter is null - device may not support Bluetooth")
        }
    }

    //  CALLBACK INTERFACE 

    interface BLECallback {
        fun onDeviceDetected(deviceAddress: String, deviceName: String)
        fun onScanStarted()
        fun onScanStopped()
        fun onError(errorMessage: String)
    }

    //  PUBLIC METHODS 

    fun isBluetoothAvailable(): Boolean {
        if (bluetoothAdapter == null) return false

        // Check permission for API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                return false
            }
        }

        return try {
            bluetoothAdapter.isEnabled
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking Bluetooth: ${e.message}")
            false
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning(callback: BLECallback?): Boolean {
        this.callback = callback

        // Validate prerequisites
        if (!isBluetoothAvailable()) {
            val error = "Bluetooth not available or not enabled"
            Log.w(TAG, error)
            callback?.onError(error)
            return false
        }

        if (!hasRequiredPermissions()) {
            val error = "Required Bluetooth permissions not granted"
            Log.w(TAG, error)
            callback?.onError(error)
            return false
        }

        return try {
            // Register receiver if not already registered
            if (!isReceiverRegistered) {
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                context.registerReceiver(receiver, filter)
                isReceiverRegistered = true
                Log.d(TAG, "Broadcast receiver registered")
            }

            // Cancel previous discovery if running
            if (checkDiscoveryState()) {
                cancelDiscoveryWithPermissionCheck()
                Log.d(TAG, "Cancelled previous discovery")
            }

            // Start discovery
            val started = startDiscoveryWithPermissionCheck()
            if (started) {
                Log.d(TAG, "✅ Bluetooth discovery started")
                callback?.onScanStarted()
                true
            } else {
                val error = "Failed to start Bluetooth discovery"
                Log.e(TAG, error)
                callback?.onError(error)
                cleanupReceiver()
                false
            }
        } catch (e: SecurityException) {
            val error = "SecurityException: ${e.message}"
            Log.e(TAG, error)
            callback?.onError(error)
            cleanupReceiver()
            false
        } catch (e: Exception) {
            val error = "Unexpected error: ${e.message}"
            Log.e(TAG, error)
            callback?.onError(error)
            cleanupReceiver()
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (checkDiscoveryState()) {
            val cancelled = cancelDiscoveryWithPermissionCheck()
            if (cancelled) {
                Log.d(TAG, "✅ Bluetooth discovery stopped")
            } else {
                Log.w(TAG, "Failed to cancel discovery")
            }
        }

        cleanupReceiver()
        callback?.onScanStopped()
    }

    fun cleanup() {
        stopScanning()
        callback = null
    }

    //  PRIVATE HELPER METHODS 

    @SuppressLint("MissingPermission")
    private fun checkDiscoveryState(): Boolean {
        if (bluetoothAdapter == null) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.isDiscovering
                } else {
                    false
                }
            } else {
                bluetoothAdapter.isDiscovering
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking discovery: ${e.message}")
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscoveryWithPermissionCheck(): Boolean {
        if (bluetoothAdapter == null) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.startDiscovery()
                } else {
                    Log.w(TAG, "BLUETOOTH_SCAN permission not granted")
                    false
                }
            } else {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.startDiscovery()
                } else {
                    Log.w(TAG, "ACCESS_FINE_LOCATION permission not granted")
                    false
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during startDiscovery: ${e.message}")
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun cancelDiscoveryWithPermissionCheck(): Boolean {
        if (bluetoothAdapter == null) return false

        return try {
            bluetoothAdapter.cancelDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during cancelDiscovery: ${e.message}")
            false
        }
    }

    private fun cleanupReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
                Log.d(TAG, "Receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.d(TAG, "Receiver already unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver: ${e.message}")
            } finally {
                isReceiverRegistered = false
            }
        }
    }

    //  BROADCAST RECEIVER 

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_FOUND) return

            // Get device (API 33+ type-safe method)
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            if (device == null) {
                Log.w(TAG, "Device is null")
                return
            }

            if (callback == null) {
                Log.d(TAG, "No callback registered")
                return
            }

            // Get device address (no permission needed)
            val deviceAddress = try {
                device.address ?: ""
            } catch (e: Exception) {
                Log.e(TAG, "Error getting address: ${e.message}")
                return
            }

            // Get device name (requires permission)
            val deviceName = getDeviceNameSafely(device)

            Log.d(TAG, "Device found - Address: $deviceAddress, Name: $deviceName")

            try {
                callback?.onDeviceDetected(deviceAddress, deviceName)
            } catch (e: Exception) {
                Log.e(TAG, "Error in callback: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceNameSafely(device: BluetoothDevice): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                    device.name?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown Device"
                } else {
                    "Unknown (No Permission)"
                }
            } else {
                device.name?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown Device"
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting name: ${e.message}")
            "Unknown (Permission Error)"
        }
    }
}
