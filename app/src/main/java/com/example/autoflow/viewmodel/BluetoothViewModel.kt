package com.example.autoflow.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.model.SavedBluetoothDevice
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    // --- Database Operations (Your existing code) ---
    private val bluetoothDao = AppDatabase.getDatabase(application).savedBluetoothDeviceDao()
    val allDevices: LiveData<List<SavedBluetoothDevice>> = bluetoothDao.getAllDevices()

    // --- Bluetooth Adapter Operations (Missing code needed for TriggerContent) ---
    private val _pairedDevices = MutableLiveData<List<SavedBluetoothDevice>>()
    val pairedDevices: LiveData<List<SavedBluetoothDevice>> = _pairedDevices

    @SuppressLint("MissingPermission") // Permissions are checked in UI before calling this
    fun refreshPairedDevices() {
        try {
            val bluetoothManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter

            if (adapter != null && adapter.isEnabled) {
                val bondedDevices = adapter.bondedDevices
                if (bondedDevices != null) {
                    val deviceList = bondedDevices.map { device ->
                        SavedBluetoothDevice(
                            macAddress = device.address,
                            deviceName = device.name ?: "Unknown Device",
                            displayName = device.name ?: "Unknown Device",
                            isFavorite = false
                        )
                    }
                    _pairedDevices.value = deviceList
                } else {
                    _pairedDevices.value = emptyList()
                }
            } else {
                _pairedDevices.value = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _pairedDevices.value = emptyList()
        }
    }

    // --- Your Existing Database Functions ---
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
