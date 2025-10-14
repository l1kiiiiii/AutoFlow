package com.example.autoflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.model.SavedLocation
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).savedLocationDao()

    val allLocations: LiveData<List<SavedLocation>> = dao.getAllLocations()
    val favoriteLocations: LiveData<List<SavedLocation>> = dao.getFavoriteLocations()

    fun saveLocation(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        radius: Double = 100.0,
        isFavorite: Boolean = false
    ) {
        viewModelScope.launch {
            dao.insertLocation(
                SavedLocation(
                    name = name,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    isFavorite = isFavorite
                )
            )
        }
    }

    fun updateLocation(location: SavedLocation) {
        viewModelScope.launch {
            dao.updateLocation(location)
        }
    }

    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            dao.deleteLocation(location)
        }
    }

    fun toggleFavorite(location: SavedLocation) {
        viewModelScope.launch {
            dao.updateLocation(location.copy(isFavorite = !location.isFavorite))
        }
    }
}
