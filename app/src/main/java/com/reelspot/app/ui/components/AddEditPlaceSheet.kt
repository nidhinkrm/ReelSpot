package com.reelspot.app.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
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
    searchResults: List<android.location.Address>,
    isSearchingAddress: Boolean,
    onGetAddressFromLatLng: suspend (Double, Double) -> String
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(editingPlace?.name ?: "") }
    var description by remember { mutableStateOf(editingPlace?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(editingPlace?.category ?: "Food") }
    var selectedRadius by remember { mutableStateOf(editingPlace?.radius ?: 500f) }
    var reelUrl by remember { mutableStateOf(editingPlace?.reelUrl ?: initialReelUrl) }

    // Map & Location picking state
    var selectedLatLng by remember {
        mutableStateOf(
            if (editingPlace != null) LatLng(editingPlace.latitude, editingPlace.longitude)
            else LatLng(37.7749, -122.4194) // Default fallback (e.g. San Francisco)
        )
    }
    var addressText by remember { mutableStateOf("") }
    var addressSearchQuery by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLatLng, 15f)
    }

    LaunchedEffect(selectedLatLng) {
        addressText = onGetAddressFromLatLng(selectedLatLng.latitude, selectedLatLng.longitude)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(onClick = onDismiss) {
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description / Notes Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes") },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Location Search & Selection
            Text(
                text = "Select Location on Map",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = addressSearchQuery,
                onValueChange = {
                    addressSearchQuery = it
                    onSearchAddress(it)
                },
                label = { Text("Search location address...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Address")
                },
                trailingIcon = {
                    if (isSearchingAddress) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                    } else if (addressSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { addressSearchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Geocoding search results list
            if (searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        searchResults.take(4).forEach { address ->
                            val line = address.getAddressLine(0) ?: "${address.latitude}, ${address.longitude}"
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newLatLng = LatLng(address.latitude, address.longitude)
                                        selectedLatLng = newLatLng
                                        addressSearchQuery = ""
                                        if (name.isBlank() && address.featureName != null) {
                                            name = address.featureName
                                        }
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(newLatLng, 16f)
                                            )
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (addressText.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Interactive Google Map Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        selectedLatLng = latLng
                    }
                ) {
                    Marker(
                        state = MarkerState(position = selectedLatLng),
                        title = name.ifBlank { "Selected Spot" }
                    )
                    Circle(
                        center = selectedLatLng,
                        radius = selectedRadius.toDouble(),
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3f
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
                            selectedLatLng.latitude,
                            selectedLatLng.longitude,
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
}
