package com.example.autoflow.domain.usecase.location

import com.example.autoflow.data.SavedLocation
import com.example.autoflow.data.SavedLocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case for saving a location
 * Encapsulates business logic for location persistence
 */
class SaveLocationUseCase(private val locationDao: SavedLocationDao) {
    
    suspend fun execute(
        name: String,
        latitude: Double,
        longitude: Double,
        radius: Double = 100.0,
        address: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Validate inputs
            if (name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Location name cannot be empty"))
            }
            
            if (latitude < -90 || latitude > 90) {
                return@withContext Result.failure(IllegalArgumentException("Invalid latitude: $latitude"))
            }
            
            if (longitude < -180 || longitude > 180) {
                return@withContext Result.failure(IllegalArgumentException("Invalid longitude: $longitude"))
            }
            
            if (radius <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Radius must be positive"))
            }
            
            // Create location entity
            val savedLocation = SavedLocation(
                name = name.trim(),
                address = address.trim(),
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                createdAt = System.currentTimeMillis(),
                isFavorite = false
            )
            
            // Save to database
            val id = locationDao.insertLocation(savedLocation)
            Result.success(id)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
