package com.example.autoflow.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.autoflow.data.AppDatabase
import com.example.autoflow.data.SavedLocationDao
import com.example.autoflow.integrations.LocationManager
import com.example.autoflow.data.SavedLocation
import com.example.autoflow.domain.usecase.location.SaveLocationUseCase
import com.example.autoflow.domain.usecase.location.DeleteLocationUseCase
import com.example.autoflow.domain.usecase.location.UpdateLocationUseCase
import com.example.autoflow.domain.usecase.location.GetLocationsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ✅ Enhanced LocationViewModel with Use Cases for business logic
 * Follows Clean Architecture - delegates business logic to domain layer
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val locationDao: SavedLocationDao = database.savedLocationDao()
    private val locationManager = LocationManager(application)
    
    // Use Cases - encapsulate business logic
    private val saveLocationUseCase = SaveLocationUseCase(locationDao)
    private val deleteLocationUseCase = DeleteLocationUseCase(locationDao)
    private val updateLocationUseCase = UpdateLocationUseCase(locationDao)
    private val getLocationsUseCase = GetLocationsUseCase(locationDao)

    companion object {
        private const val TAG = "LocationViewModel"
    }

    // ✅ LiveData for saved locations - BOTH properties for compatibility
    private val _savedLocations = MutableLiveData<List<SavedLocation>>()
    val savedLocations: LiveData<List<SavedLocation>> = _savedLocations

    // ✅ Add allLocations property that UI expects
    val allLocations: LiveData<List<SavedLocation>> = locationDao.getAllLocations()

    // Current location
    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation

    // Status messages
    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadSavedLocations()
    }

    /**
     * ✅ Load all saved locations from database using Use Case
     */
    fun loadSavedLocations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = getLocationsUseCase.execute()
                
                result.onSuccess { locations ->
                    _savedLocations.value = locations
                    Log.d(TAG, "✅ Loaded ${locations.size} saved locations")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error loading locations", error)
                    _statusMessage.value = "Failed to load locations: ${error.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Get current location
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        if (!hasLocationPermissions()) {
            _statusMessage.value = "Location permissions not granted"
            return
        }

        _isLoading.value = true
        _statusMessage.value = "Getting current location..."

        locationManager.getLastLocation(object : LocationManager.Callback {
            override fun onLocationReceived(location: Location) {
                _currentLocation.value = location
                _isLoading.value = false
                _statusMessage.value = "✅ Location found: ${location.latitude}, ${location.longitude}"
                Log.d(TAG, "✅ Current location: ${location.latitude}, ${location.longitude}")
            }

            override fun onLocationError(errorMessage: String) {
                _isLoading.value = false
                _statusMessage.value = "❌ Location error: $errorMessage"
                Log.e(TAG, "❌ Location error: $errorMessage")
            }

            override fun onPermissionDenied() {
                _isLoading.value = false
                _statusMessage.value = "❌ Location permission denied"
                Log.w(TAG, "❌ Location permission denied")
            }
        })
    }

    /**
     * ✅ FIXED: Save location with name - Using Use Case
     */
    fun saveLocation(
        name: String,
        latitude: Double,
        longitude: Double,
        radius: Double = 100.0,
        address: String = ""
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val result = saveLocationUseCase.execute(name, latitude, longitude, radius, address)
                
                result.onSuccess { id ->
                    _statusMessage.value = "✅ Location '$name' saved successfully"
                    Log.d(TAG, "✅ Location saved: $name at $latitude, $longitude with ID $id")
                    loadSavedLocations()
                }.onFailure { error ->
                    _statusMessage.value = "❌ ${error.message}"
                    Log.e(TAG, "❌ Error saving location", error)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * ✅ Save current location with name
     */
    fun saveCurrentLocation(name: String) {
        val location = _currentLocation.value
        if (location == null) {
            _statusMessage.value = "❌ No current location available. Please get location first."
            return
        }

        saveLocation(name, location.latitude, location.longitude)
    }

    /**
     * ✅ Delete saved location - Using Use Case
     */
    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            try {
                val result = deleteLocationUseCase.execute(location)
                
                result.onSuccess {
                    _statusMessage.value = "✅ Location '${location.name}' deleted"
                    loadSavedLocations()
                    Log.d(TAG, "✅ Location deleted: ${location.name}")
                }.onFailure { error ->
                    _statusMessage.value = "❌ Failed to delete location: ${error.message}"
                    Log.e(TAG, "❌ Error deleting location", error)
                }
            } catch (e: Exception) {
                _statusMessage.value = "❌ Failed to delete location: ${e.message}"
                Log.e(TAG, "❌ Error deleting location", e)
            }
        }
    }

    /**
     * ✅ Delete by ID - Using Use Case
     */
    fun deleteLocationById(locationId: Long) {
        viewModelScope.launch {
            try {
                val result = deleteLocationUseCase.executeById(locationId)
                
                result.onSuccess {
                    _statusMessage.value = "✅ Location deleted"
                    loadSavedLocations()
                    Log.d(TAG, "✅ Location deleted: ID $locationId")
                }.onFailure { error ->
                    _statusMessage.value = "❌ Failed to delete location: ${error.message}"
                    Log.e(TAG, "❌ Error deleting location", error)
                }
            } catch (e: Exception) {
                _statusMessage.value = "❌ Failed to delete location: ${e.message}"
                Log.e(TAG, "❌ Error deleting location", e)
            }
        }
    }

    /**
     * ✅ Toggle favorite status - Using Use Case
     */
    fun toggleFavorite(locationId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                val result = updateLocationUseCase.updateFavorite(locationId, isFavorite)
                
                result.onSuccess {
                    loadSavedLocations()
                    Log.d(TAG, "✅ Location favorite toggled: ID $locationId, favorite: $isFavorite")
                }.onFailure { error ->
                    _statusMessage.value = "❌ Failed to update favorite: ${error.message}"
                    Log.e(TAG, "❌ Error updating favorite", error)
                }
            } catch (e: Exception) {
                _statusMessage.value = "❌ Failed to update favorite: ${e.message}"
                Log.e(TAG, "❌ Error updating favorite", e)
            }
        }
    }

    /**
     * ✅ Update location - Using Use Case
     */
    fun updateLocation(location: SavedLocation) {
        viewModelScope.launch {
            try {
                val result = updateLocationUseCase.execute(location)
                
                result.onSuccess {
                    _statusMessage.value = "✅ Location '${location.name}' updated"
                    loadSavedLocations()
                    Log.d(TAG, "✅ Location updated: ${location.name}")
                }.onFailure { error ->
                    _statusMessage.value = "❌ Failed to update location: ${error.message}"
                    Log.e(TAG, "❌ Error updating location", error)
                }
            } catch (e: Exception) {
                _statusMessage.value = "❌ Failed to update location: ${e.message}"
                Log.e(TAG, "❌ Error updating location", e)
            }
        }
    }

    /**
     * ✅ Check location permissions
     */
    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation && coarseLocation
    }

    /**
     * ✅ Clear status message
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.cleanup()
        Log.d(TAG, "🧹 LocationViewModel cleaned up")
    }
}
