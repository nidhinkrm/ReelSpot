package com.reelspot.app.ui.saved

import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reelspot.app.data.PlaceFolder
import com.reelspot.app.data.SavedPlace
import com.reelspot.app.location.LocationManager
import com.reelspot.app.ui.SavedSortOption
import com.reelspot.app.ui.components.CATEGORIES
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedPlacesScreen(
    savedPlaces: List<SavedPlace>,
    folders: List<PlaceFolder> = emptyList(),
    currentLocation: Location?,
    searchQuery: String,
    selectedCategory: String?,
    selectedFolderId: Long? = null,
    selectedTag: String? = null,
    sortOption: SavedSortOption = SavedSortOption.NEWEST,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onFolderSelect: (Long?) -> Unit = {},
    onTagSelect: (String?) -> Unit = {},
    onSortSelect: (SavedSortOption) -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    onRenameFolder: (Long, String) -> Unit = { _, _ -> },
    onDeleteFolder: (Long) -> Unit = {},
    onPlaceClick: (SavedPlace) -> Unit,
    onEditPlace: (SavedPlace) -> Unit,
    onDeletePlace: (SavedPlace) -> Unit
) {
    val context = LocalContext.current
    var isCreateFolderDialogOpen by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<PlaceFolder?>(null) }
    var folderToDelete by remember { mutableStateOf<PlaceFolder?>(null) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    // Map folderId to Folder for quick lookup
    val folderMap = remember(folders) { folders.associateBy { it.id } }

    // Gather all distinct custom tags across all saved places
    val allTags = remember(savedPlaces) {
        savedPlaces.flatMap { it.tagList }.distinct().sorted()
    }

    // 1. Filter places based on search, category, folder, and tag
    val filteredPlaces = savedPlaces.filter { place ->
        val matchesQuery = searchQuery.isBlank() ||
                place.name.contains(searchQuery, ignoreCase = true) ||
                place.description.contains(searchQuery, ignoreCase = true) ||
                place.tags.contains(searchQuery, ignoreCase = true) ||
                (place.reelUrl?.contains(searchQuery, ignoreCase = true) == true)

        val matchesCategory = selectedCategory == null || place.category == selectedCategory

        val matchesFolder = when (selectedFolderId) {
            null -> true // All folders
            -1L -> place.folderId == null // Uncategorized / No folder
            else -> place.folderId == selectedFolderId
        }

        val matchesTag = selectedTag == null || place.tagList.any { it.equals(selectedTag, ignoreCase = true) }

        matchesQuery && matchesCategory && matchesFolder && matchesTag
    }

    // 2. Sort filtered places
    val sortedPlaces = when (sortOption) {
        SavedSortOption.NEWEST -> filteredPlaces.sortedByDescending { it.createdAt }
        SavedSortOption.OLDEST -> filteredPlaces.sortedBy { it.createdAt }
        SavedSortOption.NAME -> filteredPlaces.sortedBy { it.name.lowercase(Locale.getDefault()) }
        SavedSortOption.NEAREST -> {
            if (currentLocation != null) {
                filteredPlaces.sortedBy { place ->
                    LocationManager.calculateDistanceMeters(
                        currentLocation.latitude, currentLocation.longitude,
                        place.latitude, place.longitude
                    )
                }
            } else {
                filteredPlaces.sortedByDescending { it.createdAt }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header with Title, Count & Sort button ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saved Spots",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${savedPlaces.size} saved spots & reels",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sort Dropdown Button
                Box {
                    OutlinedButton(
                        onClick = { isSortMenuExpanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = sortOption.displayName, style = MaterialTheme.typography.labelMedium)
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false }
                    ) {
                        SavedSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    onSortSelect(option)
                                    isSortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Search Bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by title, notes, tags or reel...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Folders Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Folders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = { isCreateFolderDialogOpen = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Folder")
                }
            }

            // Folder Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // All Folders
                item {
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { onFolderSelect(null) },
                        label = { Text("All (${savedPlaces.size})") }
                    )
                }

                // Custom Folders
                items(folders) { folder ->
                    val count = savedPlaces.count { it.folderId == folder.id }
                    var folderMenuOpen by remember { mutableStateOf(false) }

                    Box {
                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = { onFolderSelect(folder.id) },
                            label = { Text("${folder.name} ($count)") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { folderMenuOpen = true },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Folder options",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = folderMenuOpen,
                            onDismissRequest = { folderMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Folder") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    folderMenuOpen = false
                                    folderToRename = folder
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    folderMenuOpen = false
                                    folderToDelete = folder
                                }
                            )
                        }
                    }
                }

                // Uncategorized
                val uncategorizedCount = savedPlaces.count { it.folderId == null }
                if (uncategorizedCount > 0 && folders.isNotEmpty()) {
                    item {
                        FilterChip(
                            selected = selectedFolderId == -1L,
                            onClick = { onFolderSelect(-1L) },
                            label = { Text("Uncategorized ($uncategorizedCount)") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Categories & Tags Filter Chips ---
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All Categories") },
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

            // Custom Tags bar if any tags exist
            if (allTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filter by Tag:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { onTagSelect(null) },
                            label = { Text("All Tags") }
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { onTagSelect(tag) },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
        }

        // --- Content List or Empty State ---
        if (sortedPlaces.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (savedPlaces.isEmpty()) "No saved spots yet" else "No matching spots found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (savedPlaces.isEmpty())
                                "Share an Instagram reel to Reelspot or tap + to save your first spot!"
                            else
                                "Try adjusting your search query, folder, or category filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (savedPlaces.isNotEmpty() && (searchQuery.isNotEmpty() || selectedCategory != null || selectedFolderId != null || selectedTag != null)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                onSearchQueryChange("")
                                onCategorySelect(null)
                                onFolderSelect(null)
                                onTagSelect(null)
                            }) {
                                Text("Reset All Filters")
                            }
                        }
                    }
                }
            }
        } else {
            items(sortedPlaces, key = { it.id }) { place ->
                val distance = if (currentLocation != null) {
                    LocationManager.calculateDistanceMeters(
                        currentLocation.latitude, currentLocation.longitude,
                        place.latitude, place.longitude
                    )
                } else null

                val folderName = place.folderId?.let { folderMap[it]?.name }

                SavedPlaceCardV3(
                    place = place,
                    folderName = folderName,
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

    // --- Create Folder Dialog ---
    if (isCreateFolderDialogOpen) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isCreateFolderDialogOpen = false },
            title = { Text("Create Folder") },
            text = {
                Column {
                    Text(
                        text = "Group your saved reels into categories like Travel, Food, Study, or Kerala.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        placeholder = { Text("e.g. Kerala Trip, Food Spots") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            isCreateFolderDialogOpen = false
                        }
                    },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { isCreateFolderDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Rename Folder Dialog ---
    folderToRename?.let { folder ->
        var renameName by remember { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameName.isNotBlank()) {
                            onRenameFolder(folder.id, renameName.trim())
                            folderToRename = null
                        }
                    },
                    enabled = renameName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Delete Folder Dialog (Safe deletion confirmation) ---
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder \"${folder.name}\"?") },
            text = {
                Text(
                    text = "Saved spots inside this folder will NOT be deleted. They will simply be moved to Uncategorized.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolder(folder.id)
                        folderToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * V3 Saved Place Card with thumbnail preview, folder label, custom tags,
 * reel action button, and edit/delete actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedPlaceCardV3(
    place: SavedPlace,
    folderName: String?,
    distanceMeters: Float?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenReel: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(place.createdAt))
    val hasReel = !place.reelUrl.isNullOrBlank()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Reel Thumbnail / Icon Badge + Title + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Visual Preview Badge for Reel/Place
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (hasReel) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF833AB4),
                                            Color(0xFFFD1D1D),
                                            Color(0xFFFCB045)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasReel) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Reel",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Category chip
                            AssistChip(
                                onClick = {},
                                label = { Text(place.category, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp)
                            )

                            // Folder badge if present
                            if (folderName != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = folderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Edit & Delete actions
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Spot")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Spot",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Description / notes
            if (place.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = place.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Custom Tags Display
            val tags = place.tagList
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: Saved date / Distance + Open Reel Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saved $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (distanceMeters != null) {
                        Text(
                            text = "${LocationManager.formatDistance(distanceMeters)} away",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (hasReel) {
                    OutlinedButton(
                        onClick = { onOpenReel(place.reelUrl!!) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Watch Reel", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
