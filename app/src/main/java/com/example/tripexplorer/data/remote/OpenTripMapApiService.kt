package com.example.tripexplorer.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenTripMapApiService {

    @GET("en/places/geoname")
    suspend fun getCityCoordinates(
        @Query("name") cityName: String,
        @Query("apikey") apiKey: String
    ): CityCoordinates

    @GET("en/places/radius")
    suspend fun getPlacesInRadius(
        @Query("radius") radius: Int,
        @Query("lon") lon: Double,
        @Query("lat") lat: Double,
        @Query("kinds") kinds: String = "interesting_places",
        @Query("apikey") apiKey: String
    ): PlacesResponse

    @GET("en/places/xid/{xid}")
    suspend fun getPlaceDetails(
        @Path("xid") xid: String,
        @Query("apikey") apiKey: String
    ): PlaceDetailsResponse
}
