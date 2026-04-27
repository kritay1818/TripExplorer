package com.example.tripexplorer.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenTripMapApiService {

    @GET("en/places/geoname")
    suspend fun getCityCoordinates(
        @Query("name") cityName: String,
        @Query("apikey") apiKey: String
    ): GeonameResponse

    @GET("en/places/radius")
    suspend fun getPlacesInRadius(
        @Query("radius") radius: Int,
        @Query("lon") lon: Double,
        @Query("lat") lat: Double,
        @Query("limit") limit: Int = 30,
        @Query("format") format: String = "geojson",
        @Query("apikey") apiKey: String
    ): PlacesResponse

    @GET("en/places/xid/{xid}")
    suspend fun getPlaceDetails(
        @Path("xid") xid: String,
        @Query("apikey") apiKey: String
    ): PlaceDetailsResponse
}
