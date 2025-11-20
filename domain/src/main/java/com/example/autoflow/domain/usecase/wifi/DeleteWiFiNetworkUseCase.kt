package com.example.autoflow.domain.usecase.wifi

import com.example.autoflow.data.SavedWiFiNetwork
import com.example.autoflow.data.SavedWiFiNetworkDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for deleting a WiFi network
 */
class DeleteWiFiNetworkUseCase(private val wifiDao: SavedWiFiNetworkDao) {
    
    suspend fun execute(network: SavedWiFiNetwork): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            wifiDao.deleteNetwork(network)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
