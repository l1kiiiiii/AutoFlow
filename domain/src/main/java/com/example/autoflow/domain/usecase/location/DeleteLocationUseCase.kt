package com.example.autoflow.domain.usecase.location

import com.example.autoflow.data.SavedLocation
import com.example.autoflow.data.SavedLocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for deleting a saved location
 */
class DeleteLocationUseCase(private val locationDao: SavedLocationDao) {
    
    suspend fun execute(location: SavedLocation): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            locationDao.deleteLocation(location)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun executeById(locationId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (locationId <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid location ID"))
            }
            locationDao.deleteLocationById(locationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
