package com.example.autoflow.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.autoflow.model.SavedLocation

@Dao
interface SavedLocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY is_favorite DESC, created_at DESC")
    fun getAllLocations(): LiveData<List<SavedLocation>>

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
}
