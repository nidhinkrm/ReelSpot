package com.reelspot.app.ui.map

import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.reelspot.app.data.SavedPlace
import com.reelspot.app.location.LocationManager
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    savedPlaces: List<SavedPlace>,
    currentLocation: Location?,
    onOpenPlaceDetails: (SavedPlace) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedMarkerPlace by remember { mutableStateOf<SavedPlace?>(null) }

    val initialLatLng = when {
        currentLocation != null -> LatLng(currentLocation.latitude, currentLocation.longitude)
        savedPlaces.isNotEmpty() -> LatLng(savedPlaces.first().latitude, savedPlaces.first().longitude)
        else -> LatLng(37.7749, -122.4194)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 12f)
    }

    var hasCenteredOnUser by remember { mutableStateOf(false) }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null && !hasCenteredOnUser) {
            hasCenteredOnUser = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLocation.latitude, currentLocation.longitude),
                    14f
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = currentLocation != null),
            uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false),
            onMapClick = {
                selectedMarkerPlace = null
            }
        ) {
            savedPlaces.forEach { place ->
                val latLng = LatLng(place.latitude, place.longitude)
                Marker(
                    state = MarkerState(position = latLng),
                    title = place.name,
                    snippet = "${place.category} • ${LocationManager.formatDistance(place.radius)} radius",
                    onClick = {
                        selectedMarkerPlace = place
                        true
                    }
                )
                Circle(
                    center = latLng,
                    radius = place.radius.toDouble(),
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeColor = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2f
                )
            }
        }

        // Empty state hint card when no places saved yet
        if (savedPlaces.isEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "No saved spots yet. Tap '+' to pin and save your favorite places!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // My Location Floating Action Button
        if (currentLocation != null) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(currentLocation.latitude, currentLocation.longitude),
                                15f
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My Location")
            }
        }

        // Selected Marker Place Info Bottom Card
        selectedMarkerPlace?.let { place ->
            val dist = if (currentLocation != null) {
                LocationManager.calculateDistanceMeters(
                    currentLocation.latitude, currentLocation.longitude,
                    place.latitude, place.longitude
                )
            } else null

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(place.category) }
                                )
                                if (dist != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${LocationManager.formatDistance(dist)} away",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (place.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = place.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onOpenPlaceDetails(place) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Details")
                    }
                }
            }
        }
    }
}
