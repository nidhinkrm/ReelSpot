package com.reelspot.app.data

import kotlinx.coroutines.flow.Flow

class PlaceRepository(
    private val placeDao: SavedPlaceDao
) {
    val allPlaces: Flow<List<SavedPlace>> = placeDao.getAllPlaces()

    suspend fun getPlaceById(id: Long): SavedPlace? {
        return placeDao.getPlaceById(id)
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
}
