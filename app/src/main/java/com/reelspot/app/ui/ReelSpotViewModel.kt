package com.reelspot.app.ui

import android.app.Application
import android.location.Address
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reelspot.app.data.AppDatabase
import com.reelspot.app.data.PlaceFolder
import com.reelspot.app.data.PlaceRepository
import com.reelspot.app.data.ReelProcessor
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

enum class SavedSortOption(val displayName: String) {
    NEWEST("Newest Saved"),
    OLDEST("Oldest Saved"),
    NAME("Name (A–Z)"),
    NEAREST("Nearest to Me")
}

class ReelSpotViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlaceRepository
    val locationManager: LocationManager
    private val geofenceManager: GeofenceManager

    val savedPlaces: StateFlow<List<SavedPlace>>
    val folders: StateFlow<List<PlaceFolder>>
    val currentLocation: StateFlow<Location?>

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _sortOption = MutableStateFlow(SavedSortOption.NEWEST)
    val sortOption: StateFlow<SavedSortOption> = _sortOption.asStateFlow()

    private val _selectedPlaceForDetails = MutableStateFlow<SavedPlace?>(null)
    val selectedPlaceForDetails: StateFlow<SavedPlace?> = _selectedPlaceForDetails.asStateFlow()

    private val _isAddEditSheetOpen = MutableStateFlow(false)
    val isAddEditSheetOpen: StateFlow<Boolean> = _isAddEditSheetOpen.asStateFlow()

    private val _editingPlace = MutableStateFlow<SavedPlace?>(null)
    val editingPlace: StateFlow<SavedPlace?> = _editingPlace.asStateFlow()

    private val _pastedReelUrl = MutableStateFlow("")
    val pastedReelUrl: StateFlow<String> = _pastedReelUrl.asStateFlow()

    // Holds a URL received via Android share intent – shown in ShareReelConfirmSheet
    private val _sharedReelUrl = MutableStateFlow<String?>(null)
    val sharedReelUrl: StateFlow<String?> = _sharedReelUrl.asStateFlow()

    // Duplicate Reel warning state
    private val _duplicateWarning = MutableStateFlow<SavedPlace?>(null)
    val duplicateWarning: StateFlow<SavedPlace?> = _duplicateWarning.asStateFlow()

    // Temporary feedback toast / snackbar message
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Address>>(emptyList())
    val searchResults: StateFlow<List<Address>> = _searchResults.asStateFlow()

    private val _isSearchingAddress = MutableStateFlow(false)
    val isSearchingAddress: StateFlow<Boolean> = _isSearchingAddress.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PlaceRepository(database.savedPlaceDao(), database.placeFolderDao())
        locationManager = LocationManager(application)
        geofenceManager = GeofenceManager(application)

        savedPlaces = repository.allPlaces.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        folders = repository.allFolders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        currentLocation = locationManager.currentLocation

        // Keep geofences synced when places change
        viewModelScope.launch {
            savedPlaces.collect { places ->
                geofenceManager.updateGeofences(places)
                // Keep selected detail place updated if it was modified
                _selectedPlaceForDetails.value?.let { currentDetail ->
                    val updated = places.find { it.id == currentDetail.id }
                    if (updated != null) {
                        _selectedPlaceForDetails.value = updated
                    }
                }
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

    fun setSelectedFolder(folderId: Long?) {
        _selectedFolderId.value = if (_selectedFolderId.value == folderId) null else folderId
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun setSortOption(sort: SavedSortOption) {
        _sortOption.value = sort
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    fun clearDuplicateWarning() {
        _duplicateWarning.value = null
    }

    fun openAddPlaceSheet(initialReelUrl: String = "") {
        _pastedReelUrl.value = initialReelUrl
        _editingPlace.value = null
        _duplicateWarning.value = null
        _isAddEditSheetOpen.value = true
        if (initialReelUrl.isNotBlank()) {
            checkDuplicateReelUrl(initialReelUrl)
        }
    }

    fun openEditPlaceSheet(place: SavedPlace) {
        _editingPlace.value = place
        _pastedReelUrl.value = place.reelUrl ?: ""
        _duplicateWarning.value = null
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditSheetOpen.value = false
        _editingPlace.value = null
        _pastedReelUrl.value = ""
        _searchResults.value = emptyList()
        _duplicateWarning.value = null
    }

    fun selectPlaceDetails(place: SavedPlace?) {
        _selectedPlaceForDetails.value = place
    }

    /**
     * Checks if a reel URL is already saved in another spot.
     */
    fun checkDuplicateReelUrl(url: String, currentPlaceId: Long = 0L) {
        if (url.isBlank()) {
            _duplicateWarning.value = null
            return
        }
        viewModelScope.launch {
            val existing = repository.findPlaceByReelUrl(url)
            if (existing != null && existing.id != currentPlaceId) {
                _duplicateWarning.value = existing
            } else {
                _duplicateWarning.value = null
            }
        }
    }

    /**
     * V3 save place supporting custom tags and folder assignment,
     * fully backward-compatible with V2 parameters.
     */
    fun savePlace(
        id: Long = 0,
        name: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        reelUrl: String?,
        tags: String = "",
        folderId: Long? = null
    ) {
        viewModelScope.launch {
            val trimmedUrl = if (reelUrl.isNullOrBlank()) null else reelUrl.trim()

            // Reliable saving check: if adding new, verify no duplicate reel URL
            if (id == 0L && trimmedUrl != null) {
                val existing = repository.findPlaceByReelUrl(trimmedUrl)
                if (existing != null) {
                    _userFeedbackMessage.value = "Note: Reel is already saved as \"${existing.name}\""
                }
            }

            val place = SavedPlace(
                id = id,
                name = name.trim(),
                description = description.trim(),
                category = category,
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                reelUrl = trimmedUrl,
                tags = tags.trim(),
                folderId = folderId
            )
            val savedId = repository.savePlace(place)
            geofenceManager.addGeofenceForPlace(place.copy(id = savedId))
            closeAddEditSheet()
            _userFeedbackMessage.value = if (id == 0L) "Spot saved successfully!" else "Spot updated!"
        }
    }

    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch {
            repository.deletePlace(place)
            geofenceManager.removeGeofenceForPlace(place.id)
            if (_selectedPlaceForDetails.value?.id == place.id) {
                _selectedPlaceForDetails.value = null
            }
            _userFeedbackMessage.value = "Spot removed from saved items"
        }
    }

    // --- Folder Management ---

    fun createFolder(name: String, onCreated: ((Long) -> Unit)? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val newId = repository.createFolder(trimmed)
            _userFeedbackMessage.value = "Folder \"$trimmed\" created"
            onCreated?.invoke(newId)
        }
    }

    fun renameFolder(folderId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.renameFolder(folderId, trimmed)
            _userFeedbackMessage.value = "Folder renamed to \"$trimmed\""
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
            _userFeedbackMessage.value = "Folder deleted. Saved spots were kept safe!"
        }
    }

    fun movePlaceToFolder(placeId: Long, folderId: Long?) {
        viewModelScope.launch {
            repository.updatePlaceFolder(placeId, folderId)
            _userFeedbackMessage.value = if (folderId != null) "Moved to folder" else "Removed from folder"
        }
    }

    // --- Tag Management ---

    fun addTagToPlace(placeId: Long, tag: String) {
        val clean = tag.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val place = repository.getPlaceById(placeId) ?: return@launch
            val updated = place.addTag(clean)
            repository.updatePlaceTags(placeId, updated.tags)
            _userFeedbackMessage.value = "Tag \"$clean\" added"
        }
    }

    fun removeTagFromPlace(placeId: Long, tag: String) {
        val clean = tag.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val place = repository.getPlaceById(placeId) ?: return@launch
            val updated = place.removeTag(clean)
            repository.updatePlaceTags(placeId, updated.tags)
            _userFeedbackMessage.value = "Tag \"$clean\" removed"
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

    /**
     * Called from MainActivity when the app receives an ACTION_SEND intent.
     * Extracts and stores the shared text/URL for confirmation in the UI.
     */
    fun handleSharedIntent(sharedText: String?) {
        if (sharedText.isNullOrBlank()) return
        val cleaned = ReelProcessor.cleanUrl(sharedText)
        val url = if (cleaned.isNotBlank()) cleaned else sharedText.trim()
        _sharedReelUrl.value = url
        checkDuplicateReelUrl(url)
    }

    /** Dismiss the ShareReel confirmation screen without saving. */
    fun clearSharedReelUrl() {
        _sharedReelUrl.value = null
        _duplicateWarning.value = null
    }

    /**
     * User confirmed the shared URL – open the AddEditPlaceSheet with the URL pre-filled
     * and clear the shared URL state so the confirm screen closes.
     */
    fun confirmSharedReelUrl() {
        val url = _sharedReelUrl.value ?: return
        clearSharedReelUrl()
        openAddPlaceSheet(initialReelUrl = url)
    }
}
