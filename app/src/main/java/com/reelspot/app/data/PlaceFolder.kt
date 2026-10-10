package com.reelspot.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Version 3: Folder/Category for organizing saved spots and reels.
 * Safe deletion: Deleting a folder unlinks places (sets folderId = null)
 * without deleting the places themselves.
 */
@Entity(tableName = "place_folders")
data class PlaceFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
