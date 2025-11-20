package com.example.autoflow.domain.usecase.wifi

import com.example.autoflow.data.SavedWiFiNetwork
import com.example.autoflow.data.SavedWiFiNetworkDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for toggling WiFi network favorite status
 */
class ToggleWiFiFavoriteUseCase(private val wifiDao: SavedWiFiNetworkDao) {
    
    suspend fun execute(network: SavedWiFiNetwork): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = network.copy(isFavorite = !network.isFavorite)
            wifiDao.updateNetwork(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
