package com.reelspot.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.reelspot.app.ui.components.AddEditPlaceSheet
import com.reelspot.app.ui.components.PermissionHandler
import com.reelspot.app.ui.components.PlaceDetailSheet
import com.reelspot.app.ui.components.ShareReelConfirmSheet
import com.reelspot.app.ui.home.HomeScreen
import com.reelspot.app.ui.map.MapScreen
import com.reelspot.app.ui.saved.SavedPlacesScreen
import com.reelspot.app.ui.settings.SettingsScreen

enum class ScreenRoute(val title: String) {
    HOME("Home"),
    MAP("Map"),
    SAVED("Saved"),
    SETTINGS("Settings")
}

@Composable
fun MainScreen(viewModel: ReelSpotViewModel) {
    var currentRoute by remember { mutableStateOf(ScreenRoute.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    val savedPlaces by viewModel.savedPlaces.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val nearbyPlaces by viewModel.nearbyPlaces.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val selectedPlaceForDetails by viewModel.selectedPlaceForDetails.collectAsState()
    val isAddEditSheetOpen by viewModel.isAddEditSheetOpen.collectAsState()
    val editingPlace by viewModel.editingPlace.collectAsState()
    val pastedReelUrl by viewModel.pastedReelUrl.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchingAddress by viewModel.isSearchingAddress.collectAsState()
    val sharedReelUrl by viewModel.sharedReelUrl.collectAsState()
    val duplicateWarning by viewModel.duplicateWarning.collectAsState()
    val userFeedbackMessage by viewModel.userFeedbackMessage.collectAsState()

    // Display user feedback messages (saving, tag, folder changes)
    LaunchedEffect(userFeedbackMessage) {
        userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    PermissionHandler(
        onPermissionsGranted = {
            viewModel.startLocationUpdates()
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.HOME,
                        onClick = { currentRoute = ScreenRoute.HOME },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.MAP,
                        onClick = { currentRoute = ScreenRoute.MAP },
                        icon = { Icon(imageVector = Icons.Default.Map, contentDescription = "Map") },
                        label = { Text("Map") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.SAVED,
                        onClick = { currentRoute = ScreenRoute.SAVED },
                        icon = { Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Saved") },
                        label = { Text("Saved") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == ScreenRoute.SETTINGS,
                        onClick = { currentRoute = ScreenRoute.SETTINGS },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            },
            floatingActionButton = {
                if (currentRoute != ScreenRoute.SETTINGS) {
                    FloatingActionButton(
                        onClick = { viewModel.openAddPlaceSheet() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Place")
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentRoute) {
                    ScreenRoute.HOME -> {
                        HomeScreen(
                            savedPlacesCount = savedPlaces.size,
                            nearbyPlaces = nearbyPlaces,
                            onAddPlaceClick = { viewModel.openAddPlaceSheet() },
                            onViewMapClick = { currentRoute = ScreenRoute.MAP },
                            onSaveFromReelUrl = { url -> viewModel.openAddPlaceSheet(url) },
                            onPlaceClick = { place -> viewModel.selectPlaceDetails(place) },
                            onOpenSettingsClick = { currentRoute = ScreenRoute.SETTINGS }
                        )
                    }

                    ScreenRoute.MAP -> {
                        MapScreen(
                            savedPlaces = savedPlaces,
                            currentLocation = currentLocation,
                            onOpenPlaceDetails = { place -> viewModel.selectPlaceDetails(place) }
                        )
                    }

                    ScreenRoute.SAVED -> {
                        SavedPlacesScreen(
                            savedPlaces = savedPlaces,
                            folders = folders,
                            currentLocation = currentLocation,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            selectedFolderId = selectedFolderId,
                            selectedTag = selectedTag,
                            sortOption = sortOption,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onCategorySelect = { viewModel.setSelectedCategory(it) },
                            onFolderSelect = { viewModel.setSelectedFolder(it) },
                            onTagSelect = { viewModel.setSelectedTag(it) },
                            onSortSelect = { viewModel.setSortOption(it) },
                            onCreateFolder = { viewModel.createFolder(it) },
                            onRenameFolder = { id, name -> viewModel.renameFolder(id, name) },
                            onDeleteFolder = { viewModel.deleteFolder(it) },
                            onPlaceClick = { place -> viewModel.selectPlaceDetails(place) },
                            onEditPlace = { place -> viewModel.openEditPlaceSheet(place) },
                            onDeletePlace = { place -> viewModel.deletePlace(place) }
                        )
                    }

                    ScreenRoute.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentRoute = ScreenRoute.HOME }
                        )
                    }
                }
            }
        }

        // Share Reel Confirmation Sheet (shown when URL arrives via Android share intent)
        sharedReelUrl?.let { url ->
            ShareReelConfirmSheet(
                sharedUrl = url,
                duplicatePlace = duplicateWarning,
                onConfirm = { confirmedUrl ->
                    viewModel.clearSharedReelUrl()
                    viewModel.openAddPlaceSheet(initialReelUrl = confirmedUrl)
                },
                onViewExisting = { existing ->
                    viewModel.clearSharedReelUrl()
                    viewModel.selectPlaceDetails(existing)
                },
                onDismiss = { viewModel.clearSharedReelUrl() }
            )
        }

        // Add/Edit Place Bottom Sheet
        if (isAddEditSheetOpen) {
            AddEditPlaceSheet(
                editingPlace = editingPlace,
                initialReelUrl = pastedReelUrl,
                currentLocation = currentLocation,
                folders = folders,
                duplicateWarning = duplicateWarning,
                onCheckDuplicateUrl = { url, id -> viewModel.checkDuplicateReelUrl(url, id) },
                onCreateFolder = { name -> viewModel.createFolder(name) },
                onDismiss = { viewModel.closeAddEditSheet() },
                onSave = { id, name, description, category, lat, lng, radius, url, tags, folderId ->
                    viewModel.savePlace(id, name, description, category, lat, lng, radius, url, tags, folderId)
                },
                onSearchAddress = { query -> viewModel.searchAddressQuery(query) },
                searchResults = searchResults,
                isSearchingAddress = isSearchingAddress,
                onGetAddressFromLatLng = { lat, lng -> viewModel.getAddressFromLatLng(lat, lng) }
            )
        }

        // Place Detail Sheet
        selectedPlaceForDetails?.let { place ->
            val dist = if (currentLocation != null) {
                com.reelspot.app.location.LocationManager.calculateDistanceMeters(
                    currentLocation!!.latitude, currentLocation!!.longitude,
                    place.latitude, place.longitude
                )
            } else null

            PlaceDetailSheet(
                place = place,
                folders = folders,
                currentDistanceMeters = dist,
                onDismiss = { viewModel.selectPlaceDetails(null) },
                onEdit = { p -> viewModel.openEditPlaceSheet(p) },
                onDelete = { p -> viewModel.deletePlace(p) },
                onMoveToFolder = { placeId, folderId -> viewModel.movePlaceToFolder(placeId, folderId) },
                onAddTag = { placeId, tag -> viewModel.addTagToPlace(placeId, tag) },
                onRemoveTag = { placeId, tag -> viewModel.removeTagFromPlace(placeId, tag) },
                onCreateFolder = { name -> viewModel.createFolder(name) }
            )
        }
    }
}

@Composable
private fun Box(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}
