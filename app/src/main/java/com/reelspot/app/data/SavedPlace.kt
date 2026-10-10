package com.reelspot.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val createdAt: Long = System.currentTimeMillis()
)
