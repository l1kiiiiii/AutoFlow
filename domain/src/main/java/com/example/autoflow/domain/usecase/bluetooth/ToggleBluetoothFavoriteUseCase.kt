package com.example.autoflow.domain.usecase.bluetooth

import com.example.autoflow.data.SavedBluetoothDevice
import com.example.autoflow.data.SavedBluetoothDeviceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for toggling Bluetooth device favorite status
 */
class ToggleBluetoothFavoriteUseCase(private val bluetoothDao: SavedBluetoothDeviceDao) {
    
    suspend fun execute(device: SavedBluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = device.copy(isFavorite = !device.isFavorite)
            bluetoothDao.updateDevice(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
