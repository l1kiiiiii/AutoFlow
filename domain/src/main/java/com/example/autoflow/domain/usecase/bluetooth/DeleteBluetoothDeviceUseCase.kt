package com.example.autoflow.domain.usecase.bluetooth

import com.example.autoflow.data.SavedBluetoothDevice
import com.example.autoflow.data.SavedBluetoothDeviceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for deleting a Bluetooth device
 */
class DeleteBluetoothDeviceUseCase(private val bluetoothDao: SavedBluetoothDeviceDao) {
    
    suspend fun execute(device: SavedBluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            bluetoothDao.deleteDevice(device)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
