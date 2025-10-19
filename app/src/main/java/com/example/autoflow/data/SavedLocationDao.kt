package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.autoflow.model.SavedLocation

@Dao
interface SavedLocationDao {

    // ✅ FIXED: Use column name 'is_favorite' instead of 'isFavorite'
    @Query("SELECT * FROM saved_locations ORDER BY is_favorite DESC, created_at DESC")
    fun getAllLocations(): LiveData<List<SavedLocation>>

    // ✅ FIXED: Use column name 'is_favorite'
    @Query("SELECT * FROM saved_locations WHERE is_favorite = 1 ORDER BY name ASC")
    fun getFavoriteLocations(): LiveData<List<SavedLocation>>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getLocationById(id: Long): SavedLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocation): Long

    @Update
    suspend fun updateLocation(location: SavedLocation)

    @Delete
    suspend fun deleteLocation(location: SavedLocation)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Long)

    // ✅ FIXED: Use column name 'is_favorite' instead of 'isFavorite'
    @Query("UPDATE saved_locations SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    // ✅ ADD: Synchronous version for ViewModel
    @Query("SELECT * FROM saved_locations ORDER BY is_favorite DESC, created_at DESC")
    suspend fun getAllLocationsSync(): List<SavedLocation>
}
