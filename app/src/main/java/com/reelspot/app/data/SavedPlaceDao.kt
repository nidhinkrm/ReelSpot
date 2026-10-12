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

    /**
     * V3 Duplicate Detection:
     * Check if a reel URL has already been saved (matching either full or cleaned URL).
     */
    @Query("SELECT * FROM saved_places WHERE reelUrl = :url OR reelUrl = :cleanedUrl LIMIT 1")
    suspend fun findPlaceByReelUrl(url: String, cleanedUrl: String): SavedPlace?

    @Query("SELECT * FROM saved_places WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getPlacesByFolder(folderId: Long): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places WHERE folderId IS NULL ORDER BY createdAt DESC")
    fun getPlacesWithoutFolder(): Flow<List<SavedPlace>>

    @Query("UPDATE saved_places SET folderId = :folderId WHERE id = :placeId")
    suspend fun updatePlaceFolder(placeId: Long, folderId: Long?)

    @Query("UPDATE saved_places SET tags = :tags WHERE id = :placeId")
    suspend fun updatePlaceTags(placeId: Long, tags: String)

    @Query("SELECT * FROM saved_places ORDER BY createdAt DESC")
    suspend fun getAllPlacesList(): List<SavedPlace>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlace): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<SavedPlace>): List<Long>

    @Update
    suspend fun updatePlace(place: SavedPlace)

    @Delete
    suspend fun deletePlace(place: SavedPlace)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deletePlaceById(id: Long)

    @Query("DELETE FROM saved_places")
    suspend fun deleteAllPlaces()
}
