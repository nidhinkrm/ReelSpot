package com.reelspot.app.ui.components

import android.location.Address
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.reelspot.app.data.ReelProcessor
import com.reelspot.app.data.SavedPlace
import kotlinx.coroutines.launch

val CATEGORIES = listOf("Food", "Travel", "Cafe", "Shopping", "Other")
val RADIUS_OPTIONS = listOf(500f, 1000f, 2000f) // meters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPlaceSheet(
    editingPlace: SavedPlace?,
    initialReelUrl: String,
    currentLocation: Location? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        reelUrl: String?
    ) -> Unit,
    onSearchAddress: (String) -> Unit,
    searchResults: List<Address>,
    isSearchingAddress: Boolean,
    onGetAddressFromLatLng: suspend (Double, Double) -> String
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Form fields preserved against accidental state losses
    var name by rememberSaveable { mutableStateOf(editingPlace?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(editingPlace?.description ?: "") }
    var selectedCategory by rememberSaveable { mutableStateOf(editingPlace?.category ?: "Food") }
    var selectedRadius by rememberSaveable { mutableFloatStateOf(editingPlace?.radius ?: 500f) }
    var reelUrl by rememberSaveable { mutableStateOf(editingPlace?.reelUrl ?: initialReelUrl) }

    val defaultLat = editingPlace?.latitude
        ?: currentLocation?.latitude
        ?: 37.7749
    val defaultLng = editingPlace?.longitude
        ?: currentLocation?.longitude
        ?: -122.4194

    var selectedLat by rememberSaveable { mutableDoubleStateOf(defaultLat) }
    var selectedLng by rememberSaveable { mutableDoubleStateOf(defaultLng) }

    val selectedLatLng = LatLng(selectedLat, selectedLng)

    var addressText by rememberSaveable { mutableStateOf("") }
    var addressSearchQuery by rememberSaveable { mutableStateOf("") }

    var isFullscreenMapOpen by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = name.isNotBlank() || description.isNotBlank() || (editingPlace == null && reelUrl.isNotBlank())

    val attemptDismiss = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }

    // Sheet state with confirmValueChange to prevent accidental downward swipe dismissal while scrolling
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newState ->
            if (newState == SheetValue.Hidden && hasUnsavedChanges) {
                showDiscardDialog = true
                false // Blocks accidental drag-down dismissal!
            } else {
                true
            }
        }
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLatLng, 15f)
    }

    // Auto-center to user location if starting empty and user position is obtained
    LaunchedEffect(currentLocation) {
        if (editingPlace == null && currentLocation != null && selectedLat == 37.7749 && selectedLng == -122.4194) {
            selectedLat = currentLocation.latitude
            selectedLng = currentLocation.longitude
            val userLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
        }
    }

    // Reverse geocode whenever pin changes
    LaunchedEffect(selectedLat, selectedLng) {
        addressText = onGetAddressFromLatLng(selectedLat, selectedLng)
    }

    ModalBottomSheet(
        onDismissRequest = attemptDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingPlace == null) "Save a Place" else "Edit Place",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = attemptDismiss) {
                    Text("Cancel")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Instagram Reel URL Input
            OutlinedTextField(
                value = reelUrl,
                onValueChange = { reelUrl = it },
                label = { Text("Instagram Reel URL (Optional)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Link, contentDescription = "Reel Link")
                },
                trailingIcon = {
                    Row {
                        if (reelUrl.isNotEmpty()) {
                            IconButton(onClick = { reelUrl = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { text ->
                                if (ReelProcessor.isValidInstagramUrl(text)) {
                                    reelUrl = ReelProcessor.cleanUrl(text)
                                } else {
                                    reelUrl = text
                                }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Place Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Place Name *") },
                placeholder = { Text("e.g. Best Ramen Spot, Sky Bar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description / Notes Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
                placeholder = { Text("Must-try dishes, tips, or what to order...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Radius Selection (500m, 1km, 2km)
            Text(
                text = "Nearby Reminder Radius",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                RADIUS_OPTIONS.forEachIndexed { index, radiusValue ->
                    SegmentedButton(
                        selected = selectedRadius == radiusValue,
                        onClick = { selectedRadius = radiusValue },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = RADIUS_OPTIONS.size)
                    ) {
                        Text(
                            when (radiusValue) {
                                500f -> "500 m"
                                1000f -> "1 km"
                                else -> "2 km"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Location Search & Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pin Spot on Map",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { isFullscreenMapOpen = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Maximize Map",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Maximize Map", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Address Search Text Field
            OutlinedTextField(
                value = addressSearchQuery,
                onValueChange = {
                    addressSearchQuery = it
                    onSearchAddress(it)
                },
                label = { Text("Search location or address...") },
                placeholder = { Text("Search city, street, or spot") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Address")
                },
                trailingIcon = {
                    if (isSearchingAddress) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else if (addressSearchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            addressSearchQuery = ""
                            onSearchAddress("")
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Geocoding search results list (Fix: Auto-select and populate place name)
            if (searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        searchResults.take(5).forEach { address ->
                            val line = address.getAddressLine(0) ?: "${address.latitude}, ${address.longitude}"
                            val placeTitle = address.featureName?.takeIf { it.isNotBlank() && !it.all { c -> c.isDigit() } }
                                ?: address.locality
                                ?: address.subLocality
                                ?: line.split(",").firstOrNull()?.trim()
                                ?: "Selected Spot"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLat = address.latitude
                                        selectedLng = address.longitude
                                        name = placeTitle
                                        addressSearchQuery = line
                                        addressText = line
                                        onSearchAddress("")
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(address.latitude, address.longitude),
                                                    16f
                                                )
                                            )
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = placeTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (addressText.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = addressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Inline Map Picker Preview with Draggable Pin
            val inlineMarkerState = remember(selectedLatLng) { MarkerState(position = selectedLatLng) }
            LaunchedEffect(inlineMarkerState.position) {
                if (inlineMarkerState.position != selectedLatLng) {
                    selectedLat = inlineMarkerState.position.latitude
                    selectedLng = inlineMarkerState.position.longitude
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = currentLocation != null),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = currentLocation != null
                    ),
                    onMapClick = { latLng ->
                        selectedLat = latLng.latitude
                        selectedLng = latLng.longitude
                        inlineMarkerState.position = latLng
                    }
                ) {
                    Marker(
                        state = inlineMarkerState,
                        title = name.ifBlank { "Selected Spot" },
                        snippet = "Tap map or drag pin to adjust",
                        draggable = true
                    )
                    Circle(
                        center = selectedLatLng,
                        radius = selectedRadius.toDouble(),
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3f
                    )
                }

                // Top-right Maximize Button
                FloatingActionButton(
                    onClick = { isFullscreenMapOpen = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(42.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Maximize Map",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Bottom helpful hint pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "Tap map or drag marker to reposition pin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            editingPlace?.id ?: 0L,
                            name.trim(),
                            description.trim(),
                            selectedCategory,
                            selectedLat,
                            selectedLng,
                            selectedRadius,
                            reelUrl.ifBlank { null }
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (editingPlace == null) "Save Place" else "Update Place",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Confirmation Dialog before discarding unsaved edits
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Unsaved Place?") },
            text = { Text("You have entered details for this place. Closing now will discard your progress.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    // Full-Screen Map Picker Dialog for precise, easy pin positioning
    if (isFullscreenMapOpen) {
        FullscreenMapPicker(
            initialLatLng = selectedLatLng,
            radius = selectedRadius,
            placeName = name,
            currentLocation = currentLocation,
            onDismiss = { isFullscreenMapOpen = false },
            onConfirm = { confirmedLatLng, confirmedAddress, autoName ->
                selectedLat = confirmedLatLng.latitude
                selectedLng = confirmedLatLng.longitude
                addressText = confirmedAddress
                if (name.isBlank() && autoName.isNotBlank()) {
                    name = autoName
                }
                isFullscreenMapOpen = false
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(confirmedLatLng, 16f)
                    )
                }
            },
            onSearchAddress = onSearchAddress,
            searchResults = searchResults,
            isSearchingAddress = isSearchingAddress,
            onGetAddressFromLatLng = onGetAddressFromLatLng
        )
    }
}

/**
 * Fullscreen Interactive Map Picker Dialog
 * Gives the user full freedom to explore, pan, zoom, drag the pin, search addresses,
 * and confirm exact pin coordinates without any bottom sheet gesture conflicts.
 */
@Composable
fun FullscreenMapPicker(
    initialLatLng: LatLng,
    radius: Float,
    placeName: String,
    currentLocation: Location?,
    onDismiss: () -> Unit,
    onConfirm: (LatLng, String, String) -> Unit,
    onSearchAddress: (String) -> Unit,
    searchResults: List<Address>,
    isSearchingAddress: Boolean,
    onGetAddressFromLatLng: suspend (Double, Double) -> String
) {
    val scope = rememberCoroutineScope()
    var pickedLatLng by remember { mutableStateOf(initialLatLng) }
    var currentAddressText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAutoName by remember { mutableStateOf("") }

    val fullCameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 16f)
    }

    val markerState = remember(pickedLatLng) { MarkerState(position = pickedLatLng) }
    LaunchedEffect(markerState.position) {
        if (markerState.position != pickedLatLng) {
            pickedLatLng = markerState.position
        }
    }

    LaunchedEffect(pickedLatLng) {
        currentAddressText = onGetAddressFromLatLng(pickedLatLng.latitude, pickedLatLng.longitude)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Full Edge-to-Edge Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = fullCameraPositionState,
                properties = MapProperties(isMyLocationEnabled = currentLocation != null),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                ),
                onMapClick = { latLng ->
                    pickedLatLng = latLng
                    markerState.position = latLng
                }
            ) {
                Marker(
                    state = markerState,
                    title = placeName.ifBlank { "Selected Pin Spot" },
                    snippet = "Drag or tap anywhere to reposition pin",
                    draggable = true
                )
                Circle(
                    center = pickedLatLng,
                    radius = radius.toDouble(),
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    strokeColor = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3f
                )
            }

            // Top Search & Navigation Bar Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onSearchAddress(it)
                            },
                            placeholder = { Text("Search address or place...") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (isSearchingAddress) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                } else if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        onSearchAddress("")
                                    }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    }
                }

                // Fullscreen Geocoding Search Dropdown Results
                if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column {
                            searchResults.take(5).forEach { address ->
                                val line = address.getAddressLine(0) ?: "${address.latitude}, ${address.longitude}"
                                val placeTitle = address.featureName?.takeIf { it.isNotBlank() && !it.all { c -> c.isDigit() } }
                                    ?: address.locality
                                    ?: address.subLocality
                                    ?: line.split(",").firstOrNull()?.trim()
                                    ?: "Selected Spot"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newLatLng = LatLng(address.latitude, address.longitude)
                                            pickedLatLng = newLatLng
                                            markerState.position = newLatLng
                                            selectedAutoName = placeTitle
                                            searchQuery = line
                                            currentAddressText = line
                                            onSearchAddress("")
                                            scope.launch {
                                                fullCameraPositionState.animate(
                                                    CameraUpdateFactory.newLatLngZoom(newLatLng, 16f)
                                                )
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = placeTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // My Location Floating Button
            if (currentLocation != null) {
                FloatingActionButton(
                    onClick = {
                        val userLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                        pickedLatLng = userLatLng
                        markerState.position = userLatLng
                        scope.launch {
                            fullCameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(userLatLng, 16f)
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }

            // Bottom Confirmation Floating Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentAddressText.isNotBlank()) currentAddressText else "Tap map or drag marker to set pin",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onConfirm(pickedLatLng, currentAddressText, selectedAutoName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Pin Location", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
