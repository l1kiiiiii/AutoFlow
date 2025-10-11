package com.example.autoflow.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.util.ActionExecutor
import com.example.autoflow.util.Constants
import com.example.autoflow.util.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * BLE Trigger Worker - Scans for Bluetooth devices
 * Uses CoroutineWorker for proper async handling
 */
class BLETriggerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "BLETriggerWorker"
        private const val SCAN_TIMEOUT_MS = 30000L // 30 seconds
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        // Check permissions
        if (!PermissionUtils.hasBluetoothPermissions(applicationContext)) {
            Log.e(TAG, "❌ Bluetooth permissions not granted")
            return Result.failure()
        }

        // Get input data
        val workflowId = inputData.getLong(Constants.KEY_WORKFLOW_ID, -1L)
        val targetDeviceAddress = inputData.getString(Constants.KEY_BLE_DEVICE_ADDRESS)

        if (workflowId == -1L || targetDeviceAddress.isNullOrBlank()) {
            Log.e(TAG, "❌ Invalid input: workflowId=$workflowId, address=$targetDeviceAddress")
            return Result.failure()
        }

        // Get Bluetooth adapter
        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "❌ Bluetooth not available or disabled")
            return Result.failure()
        }

        val bleScanner = bluetoothAdapter.bluetoothLeScanner
        if (bleScanner == null) {
            Log.e(TAG, "❌ BLE scanner not available")
            return Result.failure()
        }

        Log.d(TAG, "🔵 Starting BLE scan for: $targetDeviceAddress")

        // Scan for device
        val deviceFound = try {
            scanForDevice(bleScanner, targetDeviceAddress)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException: ${e.message}")
            return Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Scan error: ${e.message}")
            return Result.retry()
        }

        if (deviceFound) {
            Log.d(TAG, "✅ Target device found, executing action")
            return executeAction(workflowId)
        }

        Log.d(TAG, "⚠️ Device not found")
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private suspend fun scanForDevice(
        bleScanner: android.bluetooth.le.BluetoothLeScanner,
        targetAddress: String
    ): Boolean = suspendCancellableCoroutine { continuation ->

        var isResumed = false

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (isResumed) return

                val foundAddress = result?.device?.address
                if (foundAddress == targetAddress) {
                    Log.d(TAG, "🎯 Device found: $foundAddress")
                    isResumed = true
                    bleScanner.stopScan(this)
                    handler.removeCallbacksAndMessages(null)
                    continuation.resume(true)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                if (isResumed) return
                Log.e(TAG, "❌ Scan failed: $errorCode")
                isResumed = true
                handler.removeCallbacksAndMessages(null)
                continuation.resume(false)
            }
        }

        // Start scan
        try {
            bleScanner.startScan(callback)

            // Timeout
            handler.postDelayed({
                if (!isResumed) {
                    Log.d(TAG, "⏱️ Scan timeout")
                    bleScanner.stopScan(callback)
                    isResumed = true
                    continuation.resume(false)
                }
            }, SCAN_TIMEOUT_MS)

            // Handle cancellation
            continuation.invokeOnCancellation {
                try {
                    bleScanner.stopScan(callback)
                    handler.removeCallbacksAndMessages(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during cleanup: ${e.message}")
                }
            }
        } catch (e: SecurityException) {
            isResumed = true
            continuation.resume(false)
        }
    }

    private suspend fun executeAction(workflowId: Long): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val workflow = database.workflowDao().getByIdSync(workflowId)

            if (workflow == null) {
                Log.e(TAG, "❌ Workflow not found: $workflowId")
                return Result.failure()
            }

            if (!workflow.isEnabled) {
                Log.d(TAG, "⚠️ Workflow disabled: $workflowId")
                return Result.success()
            }

            val action = workflow.toAction()
            if (action == null) {
                Log.e(TAG, "❌ No valid action")
                return Result.failure()
            }

            val success = ActionExecutor.executeAction(applicationContext, action)
            if (success) {
                Log.d(TAG, "✅ Action executed")
                Result.success()
            } else {
                Log.e(TAG, "❌ Action execution failed")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error executing action", e)
            Result.retry()
        }
    }
}
