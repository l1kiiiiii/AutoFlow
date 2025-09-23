package com.example.autoflow.integrations;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log; // Added for logging potential issues

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

// Removed "extends Context"
public class BLEManager {
    private static final String TAG = "BLEManager"; // Added for logging
    private final Context context; // This context will be used
    private final BluetoothAdapter bluetoothAdapter;
    private BLECallback callback;

    public interface BLECallback {
        void onDeviceDetected(@NonNull String deviceAddress);
    }

    public BLEManager(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            this.bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            this.bluetoothAdapter = null;
            Log.e(TAG, "BluetoothManager not available.");
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void startScanning(BLECallback callback) {
        this.callback = callback;
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "BluetoothAdapter not available or not enabled.");
            return;
        }

        // Use the provided context for permission checks
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_SCAN permission not granted. Cannot start discovery.");
            // TODO: Request permissions from the Activity/Fragment that uses BLEManager
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        try {
            context.registerReceiver(receiver, filter);
        } catch (Exception e) {
            Log.e(TAG, "Error registering broadcast receiver: " + e.getMessage());
            return; // Can't proceed if receiver can't be registered
        }
        
        try {
            if (bluetoothAdapter.isDiscovering()) {
                // BLUETOOTH_SCAN is needed for cancelDiscovery too on API 31+
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.cancelDiscovery(); 
                    Log.d(TAG, "Cancelled previous discovery before starting a new one.");
                } else {
                    Log.w(TAG, "BLUETOOTH_SCAN permission not granted. Cannot cancel previous discovery.");
                }
            }
            bluetoothAdapter.startDiscovery();
            Log.d(TAG, "Bluetooth discovery started.");
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException during startDiscovery: " + se.getMessage());
             try {
                context.unregisterReceiver(receiver); // Clean up receiver if discovery fails to start
            } catch (IllegalArgumentException e) {
                // Receiver might not have been registered if startDiscovery failed early
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopScanning() {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not available, cannot stop scanning.");
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_SCAN permission not granted. Cannot properly stop discovery.");
        } else {
            if (bluetoothAdapter.isDiscovering()) {
                try {
                    bluetoothAdapter.cancelDiscovery();
                    Log.d(TAG, "Bluetooth discovery stopped.");
                } catch (SecurityException se) {
                    Log.e(TAG, "SecurityException during cancelDiscovery: " + se.getMessage());
                }
            }
        }
        
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Receiver was not registered or already unregistered.");
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        // Added @RequiresPermission as a note for operations like getName/getAlias on API 31+
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && callback != null) {
                    String deviceAddress = device.getAddress(); // Requires BLUETOOTH permission (classic), not BLUETOOTH_CONNECT for address itself.
                    String deviceName = "Unknown"; // Default
                    // Getting name or alias requires BLUETOOTH_CONNECT on API 31+
                    if (ActivityCompat.checkSelfPermission(BLEManager.this.context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            deviceName = device.getName();
                            if (deviceName == null || deviceName.isEmpty()) {
                                deviceName = "Unknown Device";
                            }
                        } catch (SecurityException se) {
                            Log.e(TAG, "SecurityException getting device name: " + se.getMessage());
                            deviceName = "Unknown (No Permission)";
                        }
                    } else {
                        Log.w(TAG, "BLUETOOTH_CONNECT permission not granted, cannot get device name.");
                        deviceName = "Unknown (No Permission)";
                    }
                    Log.d(TAG, "Device found: " + deviceAddress + " (Name: " + deviceName + ")");
                    callback.onDeviceDetected(deviceAddress); // Or pass a combined string, or a custom object
                }
            }
        }
    };
}