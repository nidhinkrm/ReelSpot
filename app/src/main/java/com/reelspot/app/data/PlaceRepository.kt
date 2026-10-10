package com.reelspot.app.data

import kotlinx.coroutines.flow.Flow

class PlaceRepository(
    private val placeDao: SavedPlaceDao,
    private val folderDao: PlaceFolderDao
) {
    val allPlaces: Flow<List<SavedPlace>> = placeDao.getAllPlaces()
    val allFolders: Flow<List<PlaceFolder>> = folderDao.getAllFolders()

    suspend fun getPlaceById(id: Long): SavedPlace? {
        return placeDao.getPlaceById(id)
    }

    /**
     * Finds if an Instagram reel URL has already been saved.
     * Uses both raw and cleaned URL for reliable matching.
     */
    suspend fun findPlaceByReelUrl(url: String): SavedPlace? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val cleaned = ReelProcessor.cleanUrl(trimmed)
        return placeDao.findPlaceByReelUrl(trimmed, cleaned)
    }

    suspend fun savePlace(place: SavedPlace): Long {
        return if (place.id == 0L) {
            placeDao.insertPlace(place)
        } else {
            placeDao.updatePlace(place)
            place.id
        }
    }

    suspend fun deletePlace(place: SavedPlace) {
        placeDao.deletePlace(place)
    }

    suspend fun deletePlaceById(id: Long) {
        placeDao.deletePlaceById(id)
    }

    suspend fun updatePlaceFolder(placeId: Long, folderId: Long?) {
        placeDao.updatePlaceFolder(placeId, folderId)
    }

    suspend fun updatePlaceTags(placeId: Long, tags: String) {
        placeDao.updatePlaceTags(placeId, tags)
    }

    // --- Folder Operations ---

    suspend fun createFolder(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val existing = folderDao.getFolderByName(trimmed)
        if (existing != null) return existing.id
        return folderDao.insertFolder(PlaceFolder(name = trimmed))
    }

    suspend fun renameFolder(folderId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val folder = folderDao.getFolderById(folderId) ?: return
        folderDao.updateFolder(folder.copy(name = trimmed))
    }

    /**
     * Safe folder deletion:
     * Unlinks all places from this folder (setting folderId = null)
     * so that deleting a folder NEVER deletes the saved places inside it.
     */
    suspend fun deleteFolder(folderId: Long) {
        folderDao.clearFolderFromPlaces(folderId)
        val folder = folderDao.getFolderById(folderId)
        if (folder != null) {
            folderDao.deleteFolder(folder)
        }
    }
}
