package com.reelspot.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {

    @Query("SELECT * FROM saved_places ORDER BY createdAt DESC")
    fun getAllPlaces(): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places WHERE id = :id")
    suspend fun getPlaceById(id: Long): SavedPlace?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlace): Long

    @Update
    suspend fun updatePlace(place: SavedPlace)

    @Delete
    suspend fun deletePlace(place: SavedPlace)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deletePlaceById(id: Long)
}
