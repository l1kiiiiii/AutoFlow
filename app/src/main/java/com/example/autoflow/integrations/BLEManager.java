package com.example.autoflow.integrations;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

public class BLEManager {
    private static final String TAG = "BLEManager";
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BLECallback callback;
    private boolean isReceiverRegistered = false;

    public interface BLECallback {
        // Removed redundant 'public' modifier - all interface methods are implicitly public
        void onDeviceDetected(@NonNull String deviceAddress, @NonNull String deviceName);
        void onScanStarted();
        void onScanStopped();
        void onError(@NonNull String errorMessage);
    }

    public BLEManager(@NonNull Context context) {
        this.context = context.getApplicationContext();

        BluetoothManager bluetoothManager = (BluetoothManager)
                this.context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            this.bluetoothAdapter = bluetoothManager.getAdapter();
            if (this.bluetoothAdapter == null) {
                Log.e(TAG, "BluetoothAdapter is null - device may not support Bluetooth");
            }
        } else {
            this.bluetoothAdapter = null;
            Log.e(TAG, "BluetoothManager service not available on this device");
        }
    }

    /**
     * Checks if Bluetooth is supported and enabled on this device
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH
    })
    public boolean isBluetoothAvailable() {
        if (bluetoothAdapter == null) {
            return false;
        }

        // Explicit permission check before calling isEnabled() for API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted, cannot check if Bluetooth is enabled");
                return false;
            }
        }

        try {
            return bluetoothAdapter.isEnabled();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException checking if Bluetooth is enabled: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the required permissions are granted based on Android version
     */
    public boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED;
        } else {
            // For older APIs, ACCESS_FINE_LOCATION implies necessary discovery permissions
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Gets required permissions array based on Android version
     */
    @NonNull
    public String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            return new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH, // Legacy requirement
                    Manifest.permission.BLUETOOTH_ADMIN // Legacy requirement
            };
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public boolean startScanning(@Nullable BLECallback callback) {
        this.callback = callback;

        // Validate prerequisites
        if (!isBluetoothAvailable()) {
            String errorMsg = "Bluetooth not available or not enabled";
            Log.w(TAG, errorMsg);
            if (callback != null) {
                callback.onError(errorMsg);
            }
            return false;
        }

        if (!hasRequiredPermissions()) {
            String errorMsg = "Required Bluetooth permissions not granted";
            Log.w(TAG, errorMsg);
            if (callback != null) {
                callback.onError(errorMsg);
            }
            return false;
        }

        try {
            // Register receiver if not already registered
            if (!isReceiverRegistered) {
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                context.registerReceiver(receiver, filter);
                isReceiverRegistered = true;
                Log.d(TAG, "Broadcast receiver registered for ACTION_FOUND");
            }

            // Check discovery state before attempting to cancel
            boolean isCurrentlyDiscovering = checkDiscoveryState();
            if (isCurrentlyDiscovering) {
                cancelDiscoveryWithPermissionCheck();
                Log.d(TAG, "Cancelled previous discovery");
            }

            // Start discovery
            boolean discoveryStarted = startDiscoveryWithPermissionCheck();
            if (discoveryStarted) {
                Log.d(TAG, "Bluetooth discovery started successfully");
                if (callback != null) {
                    callback.onScanStarted();
                }
                return true;
            } else {
                String errorMsg = "Failed to start Bluetooth discovery";
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
                cleanupReceiver();
                return false;
            }

        } catch (SecurityException e) {
            String errorMsg = "SecurityException during receiver registration or discovery: " + e.getMessage();
            Log.e(TAG, errorMsg);
            if (callback != null) {
                callback.onError(errorMsg);
            }
            cleanupReceiver();
            return false;
        } catch (Exception e) {
            String errorMsg = "Unexpected error starting discovery: " + e.getMessage();
            Log.e(TAG, errorMsg);
            if (callback != null) {
                callback.onError(errorMsg);
            }
            cleanupReceiver();
            return false;
        }
    }

    /**
     * Check if discovery is currently running with proper permission handling
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH
    })
    private boolean checkDiscoveryState() {
        if (bluetoothAdapter == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    return bluetoothAdapter.isDiscovering();
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException checking discovery state: " + e.getMessage());
                    return false;
                }
            } else {
                Log.w(TAG, "BLUETOOTH_SCAN permission not granted for isDiscovering()");
                return false;
            }
        } else {
            try {
                return bluetoothAdapter.isDiscovering();
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException checking discovery state: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Helper method to start discovery with proper permission checks
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    private boolean startDiscoveryWithPermissionCheck() {
        if (bluetoothAdapter == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    return bluetoothAdapter.startDiscovery();
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException during startDiscovery: " + e.getMessage());
                    return false;
                }
            } else {
                Log.w(TAG, "BLUETOOTH_SCAN permission not granted for startDiscovery");
                return false;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    return bluetoothAdapter.startDiscovery();
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException during startDiscovery: " + e.getMessage());
                    return false;
                }
            } else {
                Log.w(TAG, "ACCESS_FINE_LOCATION permission not granted for startDiscovery");
                return false;
            }
        }
    }

    /**
     * Helper method to cancel discovery with proper permission checks
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN
    })
    private boolean cancelDiscoveryWithPermissionCheck() {
        if (bluetoothAdapter == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    return bluetoothAdapter.cancelDiscovery();
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException during cancelDiscovery: " + e.getMessage());
                    return false;
                }
            } else {
                Log.w(TAG, "BLUETOOTH_SCAN permission not granted for cancelDiscovery");
                return false;
            }
        } else {
            try {
                return bluetoothAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException during cancelDiscovery: " + e.getMessage());
                return false;
            }
        }
    }

    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN
    })
    public void stopScanning() {
        boolean isCurrentlyDiscovering = checkDiscoveryState();

        if (isCurrentlyDiscovering) {
            boolean cancelled = cancelDiscoveryWithPermissionCheck();
            if (cancelled) {
                Log.d(TAG, "Bluetooth discovery stopped successfully");
            } else {
                Log.w(TAG, "Failed to cancel Bluetooth discovery");
            }
        }

        cleanupReceiver();

        if (callback != null) {
            callback.onScanStopped();
        }
    }

    /**
     * Cleanup method to safely unregister the broadcast receiver
     */
    private void cleanupReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver);
                Log.d(TAG, "Broadcast receiver unregistered successfully");
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Receiver was already unregistered: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error unregistering receiver: " + e.getMessage());
            } finally {
                isReceiverRegistered = false;
            }
        }
    }

    /**
     * Public method to clean up resources when BLEManager is no longer needed
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN
    })
    public void cleanup() {
        stopScanning();
        callback = null;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (intent == null) {
                Log.w(TAG, "Received null intent in BroadcastReceiver");
                return;
            }

            String action = intent.getAction();
            if (!BluetoothDevice.ACTION_FOUND.equals(action)) {
                return;
            }

            // Handle getParcelableExtra deprecation properly for API 33+
            BluetoothDevice device;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use the new type-safe method for API 33+
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
            } else {
                // Suppress deprecation warning for older API levels
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }

            if (device == null) {
                Log.w(TAG, "Received ACTION_FOUND intent with null device");
                return;
            }

            if (callback == null) {
                Log.d(TAG, "Device found but no callback registered");
                return;
            }

            String deviceAddress = getDeviceAddressSafely(device);
            if (deviceAddress == null || deviceAddress.trim().isEmpty()) {
                Log.w(TAG, "Device found but address is null or empty");
                return;
            }

            String deviceName = getDeviceNameWithPermissionCheck(device);

            Log.d(TAG, String.format("Device found - Address: %s, Name: %s", deviceAddress, deviceName));

            try {
                callback.onDeviceDetected(deviceAddress, deviceName);
            } catch (Exception e) {
                Log.e(TAG, "Error in callback.onDeviceDetected: " + e.getMessage());
            }
        }

        /**
         * Get device address safely - typically doesn't require runtime permissions
         */
        private String getDeviceAddressSafely(BluetoothDevice device) {
            try {
                return device.getAddress();
            } catch (Exception e) {
                Log.e(TAG, "Exception getting device address: " + e.getMessage());
                return null;
            }
        }

        /**
         * Get device name with explicit permission check
         */
        @RequiresPermission(anyOf = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH
        })
        private String getDeviceNameWithPermissionCheck(BluetoothDevice device) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(BLEManager.this.context,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        String name = device.getName();
                        return (name != null && !name.trim().isEmpty()) ? name.trim() : "Unknown Device";
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException getting device name: " + e.getMessage());
                        return "Unknown (Permission Error)";
                    }
                } else {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted, cannot get device name");
                    return "Unknown (No Permission)";
                }
            } else {
                try {
                    String name = device.getName();
                    return (name != null && !name.trim().isEmpty()) ? name.trim() : "Unknown Device";
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException getting device name: " + e.getMessage());
                    return "Unknown (Permission Error)";
                }
            }
        }
    };
}
