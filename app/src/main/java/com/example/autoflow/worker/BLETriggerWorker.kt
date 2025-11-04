package com.example.autoflow.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.toActions
import com.example.autoflow.data.toTriggers
import com.example.autoflow.integrations.BLEManager
import com.example.autoflow.util.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ✅ COMPLETELY FIXED BLE Trigger Worker
 */
class BLETriggerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BLETriggerWorker"
        const val KEY_WORKFLOW_ID = "workflow_id"
        const val KEY_DEVICE_ADDRESS = "device_address"
        const val KEY_DEVICE_NAME = "device_name"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val workflowId = inputData.getLong(KEY_WORKFLOW_ID, -1L)

            if (workflowId == -1L) {
                Log.e(TAG, "❌ Invalid workflow ID")
                return@withContext Result.failure()
            }

            // Check BLE permissions
            if (!hasBLEPermissions()) {
                Log.w(TAG, "⚠️ Missing BLE permissions")
                return@withContext Result.failure()
            }

            // Get workflow from database
            val database = AppDatabase.getDatabase(applicationContext)
            val workflow = database.workflowDao().getByIdSync(workflowId)

            if (workflow == null || !workflow.isEnabled) {
                return@withContext Result.success()
            }

            // Check BLE triggers
            val triggers = workflow.toTriggers()
            val bleTriggersFound = triggers.any { it.type == "BLUETOOTH" || it.type == "BLE" }

            if (bleTriggersFound) {
                // Execute workflow actions
                val actions = workflow.toActions()
                val actionExecutor = ActionExecutor.getInstance()

                actions.forEach { action ->
                    // ✅ FIXED: Use proper executeAction method with coroutine scope
                    actionExecutor.executeAction(
                        applicationContext,
                        action,
                        CoroutineScope(Dispatchers.IO)
                    )
                }

                Log.d(TAG, "✅ BLE workflow executed: ${workflow.workflowName}")
            }

            return@withContext Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in BLE worker", e)
            return@withContext Result.retry()
        }
    }

    private fun hasBLEPermissions(): Boolean {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
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

        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
