package com.example.autoflow.worker;

import android.Manifest;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager; // Import BluetoothManager
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build; // Import Build for version checks if needed for Handler
import android.os.Handler;
import android.os.Looper; // Import Looper for Handler
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.autoflow.data.AppDatabase;
import com.example.autoflow.data.WorkflowEntity;
import com.example.autoflow.data.WorkflowRepository;
import com.example.autoflow.model.Action;
import com.example.autoflow.util.Constants;
import com.example.autoflow.util.PermissionUtils;

public class BLETriggerWorker extends Worker {
    private static final String TAG = "BLETriggerWorker";
    private WorkflowRepository repository;
    private Handler scanTimeoutHandler; // Declare Handler
    private BluetoothLeScanner bleScanner; // Make scanner a field to be accessible by handler
    private ScanCallback currentScanCallback; // Make callback a field


    public BLETriggerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        repository = new WorkflowRepository(AppDatabase.getDatabase(context.getApplicationContext()));
        // Initialize Handler on the main looper, or a looper from a background thread if preferred
        // For simplicity, using main looper here. For long scans, consider a dedicated thread's looper.
        scanTimeoutHandler = new Handler(Looper.getMainLooper());
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @NonNull
    @Override
    public Result doWork() {
        if (!PermissionUtils.hasBluetoothPermissions(getApplicationContext())) {
            Log.e(TAG, "Bluetooth permissions not granted");
            // Consider rescheduling or specific failure reason
            return Result.failure();
        }

        // Get BluetoothAdapter using BluetoothManager
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter;
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            Log.e(TAG, "BluetoothManager not available");
            return Result.failure();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth not available or disabled");
            return Result.failure();
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner(); // Assign to field
        if (bleScanner == null) {
            Log.e(TAG, "BLE scanner not available");
            return Result.failure();
        }

        String targetDeviceAddress = getInputData().getString(Constants.KEY_BLE_DEVICE_ADDRESS);
        if (targetDeviceAddress == null || targetDeviceAddress.isEmpty()) { // Check for empty too
            Log.e(TAG, "No valid BLE device address provided");
            return Result.failure();
        }

        // Using try-with-resources or ensuring stopScan is always called is important
        // The current Handler approach for timeout is a common way.
        try {
            Log.d(TAG, "Starting BLE scan for device: " + targetDeviceAddress);
            
            currentScanCallback = new ScanCallback() { // Assign to field
                @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN) // Good to have, but outer check is primary
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    if (result == null || result.getDevice() == null) return; // Basic null check
                    
                    String foundDeviceAddress = result.getDevice().getAddress();
                    if (targetDeviceAddress.equals(foundDeviceAddress)) {
                        Log.d(TAG, "Target BLE device found: " + foundDeviceAddress);
                        executeActions();
                        stopBleScan(); // Use a common method to stop scan and remove callback
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.e(TAG, "BLE Scan Failed with error code: " + errorCode);
                    // Consider how to handle scan failure (e.g., retry, permanent failure)
                }
            };

            // Before starting a new scan, ensure any previous one (if worker is reused or runs quickly) is stopped.
            // However, WorkManager usually creates new Worker instances.

            bleScanner.startScan(currentScanCallback);

            // Timeout mechanism
            scanTimeoutHandler.postDelayed(() -> {
                Log.d(TAG, "BLE scan timed out. Attempting to stop scan.");
                stopBleScan();
                // Depending on requirements, you might want to return Result.failure() or Result.success() here
                // if timeout means the task didn't complete as expected.
                // For now, it just stops the scan. The worker might have already returned success.
            }, 30000); // 30 seconds timeout

            return Result.success(); // Assuming success unless an immediate error occurs
                                   // The actual "work" (finding device) happens asynchronously.
                                   // For long-running async work, consider ListenableWorker.
        } catch (SecurityException se) { // Catch SecurityException specifically if permissions change
            Log.e(TAG, "SecurityException during BLE scan: " + se.getMessage(), se);
            return Result.failure();
        } catch (Exception e) { // Catch broader exceptions
            Log.e(TAG, "Error in BLETriggerWorker: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void stopBleScan() {
        if (bleScanner != null && currentScanCallback != null) {
            try {
                bleScanner.stopScan(currentScanCallback);
                Log.d(TAG, "BLE Scan stopped.");
            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException stopping BLE scan: " + se.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Exception stopping BLE scan: " + e.getMessage());
            }
            currentScanCallback = null; // Clear callback after stopping
        }
        // Remove any pending timeout callbacks
        if (scanTimeoutHandler != null) {
            scanTimeoutHandler.removeCallbacksAndMessages(null);
        }
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public void onStopped() {
        super.onStopped();
        Log.d(TAG, "BLETriggerWorker stopped. Cleaning up scan.");
        // Ensure scan is stopped if the worker is stopped by WorkManager
        // This requires BLUETOOTH_SCAN permission.
        if (PermissionUtils.hasBluetoothPermissions(getApplicationContext())) {
            stopBleScan();
        } else {
            Log.w(TAG, "Cannot stop BLE scan onStopped: BLUETOOTH_SCAN permission missing.");
        }
    }


    private void executeActions() {
        long workflowId = getInputData().getLong(Constants.KEY_WORKFLOW_ID, -1);
        if (workflowId == -1) {
            Log.e(TAG, "No workflow ID provided for action execution");
            return;
        }

        WorkflowEntity workflowEntity = repository.getWorkflowEntityForActionExecution(workflowId);
        if (workflowEntity != null) {
            Action action = workflowEntity.toAction();
            if (action != null) {
                Log.d(TAG, "Executing action for workflow ID: " + workflowId + ", Action: " + action.getType());
                performAction(action);
            } else {
                Log.e(TAG, "Action is null after parsing for workflow ID: " + workflowId);
            }
        } else {
            Log.e(TAG, "No WorkflowEntity found for ID: " + workflowId + " to execute action.");
        }
    }

    private void performAction(Action action) {
        Context context = getApplicationContext();
        switch (action.getType()) {
            case Constants.ACTION_TOGGLE_WIFI:
                // Placeholder: Use WifiManager to toggle Wi-Fi
                Log.d(TAG, "Attempting to toggle Wi-Fi based on value: " + action.getValue());
                // Implement actual Wi-Fi toggle using WifiManager and check permissions
                break;
            case Constants.ACTION_SEND_NOTIFICATION:
                if (action.getTitle() == null || action.getMessage() == null) {
                    Log.e(TAG, "Notification title or message is null. Cannot send notification.");
                    return;
                }
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager == null) {
                    Log.e(TAG, "NotificationManager not available.");
                    return;
                }
                // Ensure Channel ID is created (ideally in Application class or on first use)
                // Assuming TimeTriggerWorker.CHANNEL_ID is valid and channel is created elsewhere.
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TimeTriggerWorker.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's icon
                        .setContentTitle(action.getTitle())
                        .setContentText(action.getMessage())
                        .setPriority(TimeTriggerWorker.getPriorityLevel(action.getPriority()))
                        .setAutoCancel(true);
                try {
                    notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                    Log.d(TAG, "Notification sent: " + action.getTitle());
                } catch (Exception e) {
                    Log.e(TAG, "Error sending notification: " + e.getMessage());
                }
                break;
            // Add cases for ACTION_TOGGLE_SETTINGS and ACTION_RUN_SCRIPT
            case Constants.ACTION_TOGGLE_SETTINGS:
                Log.d(TAG, "Attempting to toggle setting: " + action.getValue());
                // Parse action.getValue() (which should be JSON) to determine setting and target state
                // Implement logic to toggle the specific setting (e.g., Bluetooth, Location, etc.)
                // This will require appropriate permissions for each setting.
                break;
            case Constants.ACTION_RUN_SCRIPT:
                Log.d(TAG, "Attempting to run script: " + action.getValue());
                // Implement script execution logic. Be very careful about security implications.
                break;
            default:
                Log.w(TAG, "Unsupported action type in BLETriggerWorker: " + action.getType());
        }
    }
}
