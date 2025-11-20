package com.example.autoflow.domain.usecase.bluetooth

import com.example.autoflow.data.SavedBluetoothDevice
import com.example.autoflow.data.SavedBluetoothDeviceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for saving a Bluetooth device
 */
class SaveBluetoothDeviceUseCase(private val bluetoothDao: SavedBluetoothDeviceDao) {
    
    suspend fun execute(
        macAddress: String,
        deviceName: String,
        displayName: String,
        isFavorite: Boolean = false
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (macAddress.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("MAC address cannot be empty"))
            }
            
            if (displayName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Display name cannot be empty"))
            }
            
            val device = SavedBluetoothDevice(
                macAddress = macAddress,
                deviceName = deviceName,
                displayName = displayName,
                isFavorite = isFavorite
            )
            
            val id = bluetoothDao.insertDevice(device)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
