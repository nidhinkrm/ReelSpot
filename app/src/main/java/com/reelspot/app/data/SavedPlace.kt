package com.reelspot.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SavedPlace model for Reelspot.
 * Version 3 extensions:
 * - [tags]: Comma-separated string of custom tags (e.g. "Kerala Trip,Must Watch")
 * - [folderId]: Nullable ID of user-created PlaceFolder. Null indicates "No Folder" (Uncategorized)
 */
@Entity(tableName = "saved_places")
data class SavedPlace(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 500f, // 500m, 1000m (1km), 2000m (2km)
    val reelUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: String = "",
    val folderId: Long? = null
) {
    /**
     * Splits tags string into distinct non-empty list of tags.
     */
    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /**
     * Returns a copy with the given list of tags serialized as a comma-separated string.
     */
    fun withTags(newTags: List<String>): SavedPlace {
        val distinctTrimmed = newTags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return copy(tags = distinctTrimmed.joinToString(","))
    }

    /**
     * Adds a custom tag if not already present (case-insensitive check).
     */
    fun addTag(tag: String): SavedPlace {
        val clean = tag.trim()
        if (clean.isEmpty()) return this
        val current = tagList
        if (current.any { it.equals(clean, ignoreCase = true) }) return this
        return copy(tags = (current + clean).joinToString(","))
    }

    /**
     * Removes a tag (case-insensitive match).
     */
    fun removeTag(tag: String): SavedPlace {
        val clean = tag.trim()
        val updated = tagList.filterNot { it.equals(clean, ignoreCase = true) }
        return copy(tags = updated.joinToString(","))
    }
}
