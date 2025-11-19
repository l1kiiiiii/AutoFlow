package com.example.autoflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.SavedWiFiNetwork
import kotlinx.coroutines.launch

class WiFiViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiDao = AppDatabase.getDatabase(application).savedWiFiNetworkDao()

    val allNetworks: LiveData<List<SavedWiFiNetwork>> = wifiDao.getAllNetworks()

    fun saveNetwork(
        ssid: String,
        bssid: String? = null,
        displayName: String,
        isFavorite: Boolean = false
    ) {
        viewModelScope.launch {
            val network = SavedWiFiNetwork(
                ssid = ssid,
                bssid = bssid,
                displayName = displayName,
                isFavorite = isFavorite
            )
            wifiDao.insertNetwork(network)
        }
    }

    fun deleteNetwork(network: SavedWiFiNetwork) {
        viewModelScope.launch {
            wifiDao.deleteNetwork(network)
        }
    }

    fun toggleFavorite(network: SavedWiFiNetwork) {
        viewModelScope.launch {
            val updated = network.copy(isFavorite = !network.isFavorite)
            wifiDao.updateNetwork(updated)
        }
    }
}
