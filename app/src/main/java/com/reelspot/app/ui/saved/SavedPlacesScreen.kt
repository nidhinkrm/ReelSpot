package com.reelspot.app.ui.saved

import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reelspot.app.data.SavedPlace
import com.reelspot.app.location.LocationManager
import com.reelspot.app.ui.components.CATEGORIES
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedPlacesScreen(
    savedPlaces: List<SavedPlace>,
    currentLocation: Location?,
    searchQuery: String,
    selectedCategory: String?,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onPlaceClick: (SavedPlace) -> Unit,
    onEditPlace: (SavedPlace) -> Unit,
    onDeletePlace: (SavedPlace) -> Unit
) {
    val context = LocalContext.current

    val filteredPlaces = savedPlaces.filter { place ->
        val matchesQuery = searchQuery.isBlank() ||
                place.name.contains(searchQuery, ignoreCase = true) ||
                place.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || place.category == selectedCategory
        matchesQuery && matchesCategory
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Saved Spots",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All your bookmarked recommendations and places",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search places or notes...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All") },
                    leadingIcon = { Icon(imageVector = Icons.Default.FilterList, contentDescription = null) }
                )
                CATEGORIES.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category) }
                    )
                }
            }
        }

        if (filteredPlaces.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (savedPlaces.isEmpty()) "No places saved yet" else "No matching places found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredPlaces) { place ->
                val distance = if (currentLocation != null) {
                    LocationManager.calculateDistanceMeters(
                        currentLocation.latitude, currentLocation.longitude,
                        place.latitude, place.longitude
                    )
                } else null

                SavedPlaceCard(
                    place = place,
                    distanceMeters = distance,
                    onClick = { onPlaceClick(place) },
                    onEdit = { onEditPlace(place) },
                    onDelete = { onDeletePlace(place) },
                    onOpenReel = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun SavedPlaceCard(
    place: SavedPlace,
    distanceMeters: Float?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenReel: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(place.createdAt))

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = {},
                            label = { Text(place.category) }
                        )
                        if (distanceMeters != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${LocationManager.formatDistance(distanceMeters)} away",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (place.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!place.reelUrl.isNull_or_blank()) {
                    OutlinedButton(onClick = { onOpenReel(place.reelUrl!!) }) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reel Link")
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
