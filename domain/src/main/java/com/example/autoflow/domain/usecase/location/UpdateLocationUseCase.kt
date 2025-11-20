package com.example.autoflow.domain.usecase.location

import com.example.autoflow.data.SavedLocation
import com.example.autoflow.data.SavedLocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for updating a saved location
 */
class UpdateLocationUseCase(private val locationDao: SavedLocationDao) {
    
    suspend fun execute(location: SavedLocation): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (location.name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Location name cannot be empty"))
            }
            locationDao.updateLocation(location)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateFavorite(locationId: Long, isFavorite: Boolean): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                if (locationId <= 0) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid location ID"))
                }
                locationDao.updateFavorite(locationId, isFavorite)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
