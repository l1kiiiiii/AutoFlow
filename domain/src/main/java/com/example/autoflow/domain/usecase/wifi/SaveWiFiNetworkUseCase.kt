package com.example.autoflow.domain.usecase.wifi

import com.example.autoflow.data.SavedWiFiNetwork
import com.example.autoflow.data.SavedWiFiNetworkDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for saving a WiFi network
 */
class SaveWiFiNetworkUseCase(private val wifiDao: SavedWiFiNetworkDao) {
    
    suspend fun execute(
        ssid: String,
        bssid: String? = null,
        displayName: String,
        isFavorite: Boolean = false
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (ssid.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("SSID cannot be empty"))
            }
            
            if (displayName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Display name cannot be empty"))
            }
            
            val network = SavedWiFiNetwork(
                ssid = ssid,
                bssid = bssid,
                displayName = displayName,
                isFavorite = isFavorite
            )
            
            val id = wifiDao.insertNetwork(network)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
