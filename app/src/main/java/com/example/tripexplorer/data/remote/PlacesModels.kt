package com.example.tripexplorer.data.remote

data class CityCoordinates(
    val lat: Double,
    val lon: Double,
    val name: String,
    val country: String?
)

data class PlacesResponse(
    val features: List<PlaceFeature>
)

data class PlaceFeature(
    val properties: PlaceProperties
)

data class PlaceProperties(
    val xid: String,
    val name: String,
    val rate: Int?,
    val kinds: String
)

data class PlaceDetailsResponse(
    val xid: String,
    val name: String,
    val wikipedia_extracts: WikipediaExtracts?,
    val preview: ImagePreview?
)

data class WikipediaExtracts(
    val text: String
)

data class ImagePreview(
    val source: String
)
