package com.reelspot.app.ui

import android.app.Application
import android.location.Address
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reelspot.app.data.AppDatabase
import com.reelspot.app.data.PlaceRepository
import com.reelspot.app.data.SavedPlace
import com.reelspot.app.location.GeofenceManager
import com.reelspot.app.location.LocationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReelSpotViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlaceRepository
    val locationManager: LocationManager
    private val geofenceManager: GeofenceManager

    val savedPlaces: StateFlow<List<SavedPlace>>
    val currentLocation: StateFlow<Location?>

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedPlaceForDetails = MutableStateFlow<SavedPlace?>(null)
    val selectedPlaceForDetails: StateFlow<SavedPlace?> = _selectedPlaceForDetails.asStateFlow()

    private val _isAddEditSheetOpen = MutableStateFlow(false)
    val isAddEditSheetOpen: StateFlow<Boolean> = _isAddEditSheetOpen.asStateFlow()

    private val _editingPlace = MutableStateFlow<SavedPlace?>(null)
    val editingPlace: StateFlow<SavedPlace?> = _editingPlace.asStateFlow()

    private val _pastedReelUrl = MutableStateFlow("")
    val pastedReelUrl: StateFlow<String> = _pastedReelUrl.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Address>>(emptyList())
    val searchResults: StateFlow<List<Address>> = _searchResults.asStateFlow()

    private val _isSearchingAddress = MutableStateFlow(false)
    val isSearchingAddress: StateFlow<Boolean> = _isSearchingAddress.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PlaceRepository(database.savedPlaceDao())
        locationManager = LocationManager(application)
        geofenceManager = GeofenceManager(application)

        savedPlaces = repository.allPlaces.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        currentLocation = locationManager.currentLocation

        // Keep geofences synced when places change
        viewModelScope.launch {
            savedPlaces.collect { places ->
                geofenceManager.updateGeofences(places)
            }
        }
    }

    // Nearby places stream (places within distance, sorted by proximity)
    val nearbyPlaces: StateFlow<List<Pair<SavedPlace, Float>>> = combine(
        savedPlaces,
        currentLocation
    ) { places, location ->
        if (location == null) {
            places.map { it to -1f }
        } else {
            places.map { place ->
                val dist = LocationManager.calculateDistanceMeters(
                    location.latitude, location.longitude,
                    place.latitude, place.longitude
                )
                place to dist
            }.sortedBy { it.second }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun startLocationUpdates() {
        locationManager.startLocationUpdates()
    }

    fun stopLocationUpdates() {
        locationManager.stopLocationUpdates()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun openAddPlaceSheet(initialReelUrl: String = "") {
        _pastedReelUrl.value = initialReelUrl
        _editingPlace.value = null
        _isAddEditSheetOpen.value = true
    }

    fun openEditPlaceSheet(place: SavedPlace) {
        _editingPlace.value = place
        _pastedReelUrl.value = place.reelUrl ?: ""
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditSheetOpen.value = false
        _editingPlace.value = null
        _pastedReelUrl.value = ""
        _searchResults.value = emptyList()
    }

    fun selectPlaceDetails(place: SavedPlace?) {
        _selectedPlaceForDetails.value = place
    }

    fun savePlace(
        id: Long = 0,
        name: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        reelUrl: String?
    ) {
        viewModelScope.launch {
            val place = SavedPlace(
                id = id,
                name = name,
                description = description,
                category = category,
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                reelUrl = if (reelUrl.isNullOrBlank()) null else reelUrl.trim()
            )
            val savedId = repository.savePlace(place)
            geofenceManager.addGeofenceForPlace(place.copy(id = savedId))
            closeAddEditSheet()
        }
    }

    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch {
            repository.deletePlace(place)
            geofenceManager.removeGeofenceForPlace(place.id)
            if (_selectedPlaceForDetails.value?.id == place.id) {
                _selectedPlaceForDetails.value = null
            }
        }
    }

    fun searchAddressQuery(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearchingAddress.value = true
            val results = locationManager.searchAddress(query)
            _searchResults.value = results
            _isSearchingAddress.value = false
        }
    }

    fun clearAddressSearchResults() {
        _searchResults.value = emptyList()
    }

    suspend fun getAddressFromLatLng(latitude: Double, longitude: Double): String {
        return locationManager.getAddressFromCoordinates(latitude, longitude)
    }

    fun selectPlaceById(id: Long) {
        viewModelScope.launch {
            val place = repository.getPlaceById(id)
            if (place != null) {
                _selectedPlaceForDetails.value = place
            }
        }
    }
}
