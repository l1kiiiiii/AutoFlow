package com.example.autoflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.model.SavedBluetoothDevice
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothDao = AppDatabase.getDatabase(application).savedBluetoothDeviceDao()

    val allDevices: LiveData<List<SavedBluetoothDevice>> = bluetoothDao.getAllDevices()

    fun saveDevice(
        macAddress: String,
        deviceName: String,
        displayName: String,
        isFavorite: Boolean = false
    ) {
        viewModelScope.launch {
            val device = SavedBluetoothDevice(
                macAddress = macAddress,
                deviceName = deviceName,
                displayName = displayName,
                isFavorite = isFavorite
            )
            bluetoothDao.insertDevice(device)
        }
    }

    fun deleteDevice(device: SavedBluetoothDevice) {
        viewModelScope.launch {
            bluetoothDao.deleteDevice(device)
        }
    }

    fun toggleFavorite(device: SavedBluetoothDevice) {
        viewModelScope.launch {
            val updated = device.copy(isFavorite = !device.isFavorite)
            bluetoothDao.updateDevice(updated)
        }
    }
}
