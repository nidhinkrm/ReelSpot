package com.reelspot.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.reelspot.app.data.SavedPlace
import com.reelspot.app.location.LocationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailSheet(
    place: SavedPlace,
    currentDistanceMeters: Float?,
    onDismiss: () -> Unit,
    onEdit: (SavedPlace) -> Unit,
    onDelete: (SavedPlace) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    val placeLatLng = LatLng(place.latitude, place.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(placeLatLng, 15f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = {},
                            label = { Text(place.category) }
                        )
                        if (currentDistanceMeters != null && currentDistanceMeters >= 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${LocationManager.formatDistance(currentDistanceMeters)} away",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = {
                        onDismiss()
                        onEdit(place)
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Place")
                    }
                    IconButton(onClick = {
                        onDismiss()
                        onDelete(place)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Place",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (place.description.isNotBlank()) {
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = "Reminder Radius",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Nearby Notification Radius: ${LocationManager.formatDistance(place.radius)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Map Card Preview with Geofence Radius Circle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxWidth(),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = placeLatLng),
                        title = place.name
                    )
                    Circle(
                        center = placeLatLng,
                        radius = place.radius.toDouble(),
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3f
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                // Open in External Google Maps
                OutlinedButton(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:${place.latitude},${place.longitude}?q=${Uri.encode(place.name)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=${place.latitude},${place.longitude}")
                            )
                            context.startActivity(browserIntent)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Directions")
                }

                if (!place.reelUrl.isNull_or_blank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(place.reelUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Reel")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
