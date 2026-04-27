package com.example.tripexplorer.data.remote

import com.google.gson.annotations.SerializedName

data class GeonameResponse(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double,
    @SerializedName("name")
    val name: String,
    @SerializedName("country")
    val country: String?
)

data class PlacesResponse(
    @SerializedName("features")
    val features: List<PlaceFeature>
)

data class PlaceFeature(
    @SerializedName("properties")
    val properties: PlaceProperties
)

data class PlaceProperties(
    @SerializedName("xid")
    val xid: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("kinds")
    val kinds: String,
    @SerializedName("rate")
    val rate: Double?
)

data class PlaceDetailsResponse(
    val xid: String,
    val name: String,
    val wikipedia_extracts: WikipediaExtracts?,
    val preview: PlacePreview? = null
)

data class WikipediaExtracts(
    val text: String
)

data class PlacePreview(
    val source: String?
)
