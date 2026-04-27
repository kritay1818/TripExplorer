package com.example.tripexplorer.data.repository

import com.example.tripexplorer.data.local.PlaceDao
import com.example.tripexplorer.data.local.PlaceEntity
import com.example.tripexplorer.data.remote.OpenTripMapApiService
import javax.inject.Inject

class CityExplorerRepository @Inject constructor(
    private val apiService: OpenTripMapApiService,
    private val placeDao: PlaceDao
) {

    suspend fun getCityCoordinates(cityName: String, apiKey: String) =
        apiService.getCityCoordinates(cityName = cityName, apiKey = apiKey)

    suspend fun getPlacesInRadius(
        radius: Int,
        lon: Double,
        lat: Double,
        limit: Int,
        apiKey: String
    ) = apiService.getPlacesInRadius(
        radius = radius,
        lon = lon,
        lat = lat,
        limit = limit,
        apiKey = apiKey
    )

    suspend fun getPlaceDetails(xid: String, apiKey: String) =
        apiService.getPlaceDetails(xid = xid, apiKey = apiKey)

    fun getAllFavoritePlaces() = placeDao.getAllFavoritePlaces()

    suspend fun insertPlace(place: PlaceEntity) = placeDao.insertPlace(place)

    suspend fun deletePlace(place: PlaceEntity) = placeDao.deletePlace(place)

    suspend fun updatePlace(place: PlaceEntity) = placeDao.updatePlace(place)

    suspend fun clearAllFavoritePlaces() = placeDao.clearAllFavoritePlaces()
}
