package com.example.autoflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.SavedWiFiNetwork
import com.example.autoflow.domain.usecase.wifi.SaveWiFiNetworkUseCase
import com.example.autoflow.domain.usecase.wifi.DeleteWiFiNetworkUseCase
import com.example.autoflow.domain.usecase.wifi.ToggleWiFiFavoriteUseCase
import kotlinx.coroutines.launch

/**
 * WiFiViewModel refactored to use Use Cases
 * Follows Clean Architecture - delegates business logic to domain layer
 */
class WiFiViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiDao = AppDatabase.getDatabase(application).savedWiFiNetworkDao()
    
    // Use Cases - encapsulate business logic
    private val saveWiFiNetworkUseCase = SaveWiFiNetworkUseCase(wifiDao)
    private val deleteWiFiNetworkUseCase = DeleteWiFiNetworkUseCase(wifiDao)
    private val toggleWiFiFavoriteUseCase = ToggleWiFiFavoriteUseCase(wifiDao)

    val allNetworks: LiveData<List<SavedWiFiNetwork>> = wifiDao.getAllNetworks()

    fun saveNetwork(
        ssid: String,
        bssid: String? = null,
        displayName: String,
        isFavorite: Boolean = false
    ) {
        viewModelScope.launch {
            saveWiFiNetworkUseCase.execute(ssid, bssid, displayName, isFavorite)
        }
    }

    fun deleteNetwork(network: SavedWiFiNetwork) {
        viewModelScope.launch {
            deleteWiFiNetworkUseCase.execute(network)
        }
    }

    fun toggleFavorite(network: SavedWiFiNetwork) {
        viewModelScope.launch {
            toggleWiFiFavoriteUseCase.execute(network)
        }
    }
}
