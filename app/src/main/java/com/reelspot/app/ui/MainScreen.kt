package com.reelspot.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.reelspot.app.ui.components.AddEditPlaceSheet
import com.reelspot.app.ui.components.PermissionHandler
import com.reelspot.app.ui.components.PlaceDetailSheet
import com.reelspot.app.ui.home.HomeScreen
import com.reelspot.app.ui.map.MapScreen
import com.reelspot.app.ui.saved.SavedPlacesScreen

enum class ScreenRoute(val title: String) {
    HOME("Home"),
    MAP("Map"),
    SAVED("Saved")
}

@Composable
fun MainScreen(viewModel: ReelSpotViewModel) {
    var currentRoute by remember { mutableStateOf(ScreenRoute.HOME) }

    val savedPlaces by viewModel.savedPlaces.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val nearbyPlaces by viewModel.nearbyPlaces.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPlaceForDetails by viewModel.selectedPlaceForDetails.collectAsState()
    val isAddEditSheetOpen by viewModel.isAddEditSheetOpen.collectAsState()
    val editingPlace by viewModel.editingPlace.collectAsState()
    val pastedReelUrl by viewModel.pastedReelUrl.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchingAddress by viewModel.isSearchingAddress.collectAsState()

    PermissionHandler(
        onPermissionsGranted = {
            viewModel.startLocationUpdates()
        }
    ) {
        Scaffold(
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
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openAddPlaceSheet() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Place")
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
                            onPlaceClick = { place -> viewModel.selectPlaceDetails(place) }
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
                            currentLocation = currentLocation,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onCategorySelect = { viewModel.setSelectedCategory(it) },
                            onPlaceClick = { place -> viewModel.selectPlaceDetails(place) },
                            onEditPlace = { place -> viewModel.openEditPlaceSheet(place) },
                            onDeletePlace = { place -> viewModel.deletePlace(place) }
                        )
                    }
                }
            }
        }

        // Add/Edit Place Bottom Sheet
        if (isAddEditSheetOpen) {
            AddEditPlaceSheet(
                editingPlace = editingPlace,
                initialReelUrl = pastedReelUrl,
                onDismiss = { viewModel.closeAddEditSheet() },
                onSave = { id, name, description, category, lat, lng, radius, url ->
                    viewModel.savePlace(id, name, description, category, lat, lng, radius, url)
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
                currentDistanceMeters = dist,
                onDismiss = { viewModel.selectPlaceDetails(null) },
                onEdit = { p -> viewModel.openEditPlaceSheet(p) },
                onDelete = { p -> viewModel.deletePlace(p) }
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
