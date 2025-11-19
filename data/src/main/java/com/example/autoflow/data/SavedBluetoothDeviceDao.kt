package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.autoflow.data.SavedBluetoothDevice

@Dao
interface SavedBluetoothDeviceDao {

    @Query("SELECT * FROM saved_bluetooth_devices ORDER BY is_favorite DESC, created_at DESC")
    fun getAllDevices(): LiveData<List<SavedBluetoothDevice>>

    @Query("SELECT * FROM saved_bluetooth_devices")
    suspend fun getAllDevicesSync(): List<SavedBluetoothDevice>

    @Query("SELECT * FROM saved_bluetooth_devices WHERE mac_address = :macAddress LIMIT 1")
    suspend fun getDeviceByMacAddress(macAddress: String): SavedBluetoothDevice?

    @Query("SELECT * FROM saved_bluetooth_devices WHERE id = :id")
    suspend fun getDeviceById(id: Long): SavedBluetoothDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: SavedBluetoothDevice): Long

    @Update
    suspend fun updateDevice(device: SavedBluetoothDevice)

    @Delete
    suspend fun deleteDevice(device: SavedBluetoothDevice)

    @Query("DELETE FROM saved_bluetooth_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)
}
