package com.example.autoflow.worker;

import android.Manifest;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
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

    public BLETriggerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        repository = new WorkflowRepository(AppDatabase.getDatabase(context.getApplicationContext()));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @NonNull
    @Override
    public Result doWork() {
        if (!PermissionUtils.hasBluetoothPermissions(getApplicationContext())) {
            Log.e(TAG, "Bluetooth permissions not granted");
            return Result.failure();
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth not available or disabled");
            return Result.failure();
        }

        final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "BLE scanner not available");
            return Result.failure();
        }

        String targetDeviceAddress = getInputData().getString(Constants.KEY_BLE_DEVICE_ADDRESS);
        if (targetDeviceAddress == null) {
            Log.e(TAG, "No BLE device address provided");
            return Result.failure();
        }

        try {
            Log.d(TAG, "Starting BLE scan for device: " + targetDeviceAddress);
            final ScanCallback scanCallback = new ScanCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    String foundDeviceAddress = result.getDevice().getAddress();
                    if (targetDeviceAddress.equals(foundDeviceAddress)) {
                        Log.d(TAG, "Target BLE device found: " + foundDeviceAddress);
                        executeActions();
                        scanner.stopScan(this);
                    }
                }
            };
            scanner.startScan(scanCallback);

            new Handler().postDelayed(() -> {
                scanner.stopScan(scanCallback);
                Log.d(TAG, "BLE scan timed out");
            }, 30000); // 30 seconds timeout

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in BLETriggerWorker: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    private void executeActions() {
        long workflowId = getInputData().getLong(Constants.KEY_WORKFLOW_ID, -1);
        if (workflowId == -1) {
            Log.e(TAG, "No workflow ID provided");
            return;
        }

        WorkflowEntity workflowEntity = repository.getWorkflowEntityForActionExecution(workflowId);
        if (workflowEntity != null) {
            Action action = workflowEntity.toAction();
            if (action != null) {
                performAction(action);
                Log.d(TAG, "Action executed for workflow ID: " + workflowId);
            } else {
                Log.e(TAG, "Action null for workflow ID: " + workflowId);
            }
        } else {
            Log.e(TAG, "No WorkflowEntity found for ID: " + workflowId);
        }
    }

    private void performAction(Action action) {
        Context context = getApplicationContext();
        switch (action.getType()) {
            case Constants.ACTION_TOGGLE_WIFI:
                // Placeholder: Use WifiManager to toggle Wi-Fi
                Log.d(TAG, "Toggling Wi-Fi to: " + action.getValue());
                break;
            case Constants.ACTION_SEND_NOTIFICATION:
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TimeTriggerWorker.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(action.getTitle())
                        .setContentText(action.getMessage())
                        .setPriority(TimeTriggerWorker.getPriorityLevel(action.getPriority()))
                        .setAutoCancel(true);
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                Log.d(TAG, "Notification sent");
                break;
            default:
                Log.w(TAG, "Unsupported action type: " + action.getType());
        }
    }
}