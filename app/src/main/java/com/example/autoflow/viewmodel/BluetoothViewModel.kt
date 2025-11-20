package com.example.autoflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.SavedBluetoothDevice
import com.example.autoflow.domain.usecase.bluetooth.SaveBluetoothDeviceUseCase
import com.example.autoflow.domain.usecase.bluetooth.DeleteBluetoothDeviceUseCase
import com.example.autoflow.domain.usecase.bluetooth.ToggleBluetoothFavoriteUseCase
import kotlinx.coroutines.launch

/**
 * BluetoothViewModel refactored to use Use Cases
 * Follows Clean Architecture - delegates business logic to domain layer
 */
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothDao = AppDatabase.getDatabase(application).savedBluetoothDeviceDao()
    
    // Use Cases - encapsulate business logic
    private val saveBluetoothDeviceUseCase = SaveBluetoothDeviceUseCase(bluetoothDao)
    private val deleteBluetoothDeviceUseCase = DeleteBluetoothDeviceUseCase(bluetoothDao)
    private val toggleBluetoothFavoriteUseCase = ToggleBluetoothFavoriteUseCase(bluetoothDao)

    val allDevices: LiveData<List<SavedBluetoothDevice>> = bluetoothDao.getAllDevices()

    fun saveDevice(
        macAddress: String,
        deviceName: String,
        displayName: String,
        isFavorite: Boolean = false
    ) {
        viewModelScope.launch {
            saveBluetoothDeviceUseCase.execute(macAddress, deviceName, displayName, isFavorite)
        }
    }

    fun deleteDevice(device: SavedBluetoothDevice) {
        viewModelScope.launch {
            deleteBluetoothDeviceUseCase.execute(device)
        }
    }

    fun toggleFavorite(device: SavedBluetoothDevice) {
        viewModelScope.launch {
            toggleBluetoothFavoriteUseCase.execute(device)
        }
    }
}
