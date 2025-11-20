package com.example.autoflow.domain.usecase.location

import com.example.autoflow.data.SavedLocation
import com.example.autoflow.data.SavedLocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for retrieving saved locations
 */
class GetLocationsUseCase(private val locationDao: SavedLocationDao) {
    
    suspend fun execute(): Result<List<SavedLocation>> = withContext(Dispatchers.IO) {
        try {
            val locations = locationDao.getAllLocationsSync()
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
