package com.reelspot.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceFolderDao {

    @Query("SELECT * FROM place_folders ORDER BY name COLLATE NOCASE ASC")
    fun getAllFolders(): Flow<List<PlaceFolder>>

    @Query("SELECT * FROM place_folders WHERE id = :id")
    suspend fun getFolderById(id: Long): PlaceFolder?

    @Query("SELECT * FROM place_folders WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getFolderByName(name: String): PlaceFolder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: PlaceFolder): Long

    @Update
    suspend fun updateFolder(folder: PlaceFolder)

    @Delete
    suspend fun deleteFolder(folder: PlaceFolder)

    /**
     * V3 Safety Guarantee:
     * When a folder is deleted, unlink all places previously in this folder
     * so that NO saved places are ever lost.
     */
    @Query("UPDATE saved_places SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderFromPlaces(folderId: Long)
}
