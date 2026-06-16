package com.example.tripexplorer.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexplorer.R
import com.example.tripexplorer.data.local.PlaceEntity
import com.example.tripexplorer.data.remote.PlaceFeature
import com.example.tripexplorer.data.remote.PlaceDetailsResponse
import com.example.tripexplorer.data.repository.CityExplorerRepository
import com.example.tripexplorer.utils.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class CityExplorerViewModel @Inject constructor(
    private val repository: CityExplorerRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val OPEN_TRIP_MAP_API_KEY = "5ae2e3f221c38a28845f05b67350bfdf4e0ecdcaba757251fba3d8f8"
        private const val PREFS_NAME = "trip_explorer_prefs"
        private const val PREF_SEARCH_RADIUS_KM = "search_radius_km"
        private const val DEFAULT_RADIUS_KM = 10
    }

    private val _searchResults = MutableLiveData<ResultState<List<PlaceFeature>>>()
    val searchResults: LiveData<ResultState<List<PlaceFeature>>> = _searchResults

    private var originalPlacesList: List<PlaceFeature> = emptyList()

    private val _placeDetails = MutableLiveData<ResultState<PlaceDetailsResponse>>()
    val placeDetails: LiveData<ResultState<PlaceDetailsResponse>> = _placeDetails

    private val imageUrlCache = mutableMapOf<String, String?>()
    private val imageUrlCacheMutex = Mutex()

    val favoritePlaces: StateFlow<List<PlaceEntity>> = repository.getAllFavoritePlaces()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun searchPlaces(cityName: String) {
        viewModelScope.launch {
            _searchResults.value = ResultState.Loading
            try {
                val radiusMeters = getSavedSearchRadiusKm() * 1000
                val cityCoordinates = repository.getCityCoordinates(cityName, OPEN_TRIP_MAP_API_KEY)
                if (!isLikelySameCity(cityName, cityCoordinates.name)) {
                    _searchResults.value = ResultState.Error(
                        appContext.getString(R.string.city_not_found_error)
                    )
                    return@launch
                }
                Log.d(
                    "TripExplorer",
                    "Coordinates found: lat=${cityCoordinates.lat}, lon=${cityCoordinates.lon}"
                )
                val placesResponse = repository.getPlacesInRadius(
                    radius = radiusMeters,
                    lon = cityCoordinates.lon,
                    lat = cityCoordinates.lat,
                    limit = 30,
                    apiKey = OPEN_TRIP_MAP_API_KEY
                )
                Log.d("TripExplorer", "Parsed features count: ${placesResponse.features.size}")
                if (placesResponse.features.isEmpty()) {
                    _searchResults.value = ResultState.Error(
                        appContext.getString(R.string.no_places_found_error)
                    )
                    return@launch
                }
                originalPlacesList = placesResponse.features
                _searchResults.value = ResultState.Success(placesResponse.features)
            } catch (e: Exception) {
                _searchResults.value = ResultState.Error(
                    mapCitySearchErrorToMessage(e)
                )
            }
        }
    }

    fun searchPlacesByLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            _searchResults.value = ResultState.Loading
            try {
                val radiusMeters = getSavedSearchRadiusKm() * 1000
                val placesResponse = repository.getPlacesInRadius(
                    radius = radiusMeters,
                    lon = lon,
                    lat = lat,
                    limit = 30,
                    apiKey = OPEN_TRIP_MAP_API_KEY
                )
                if (placesResponse.features.isEmpty()) {
                    _searchResults.value = ResultState.Error(
                        appContext.getString(R.string.no_places_found_near_you_error)
                    )
                    return@launch
                }
                originalPlacesList = placesResponse.features
                _searchResults.value = ResultState.Success(placesResponse.features)
            } catch (e: Exception) {
                _searchResults.value = ResultState.Error(
                    mapSearchErrorToMessage(e)
                )
            }
        }
    }

    fun filterPlacesByCategory(category: String) {
        val filteredPlaces = when (category) {
            appContext.getString(R.string.chip_all) -> originalPlacesList
            appContext.getString(R.string.chip_museums) -> originalPlacesList.filter { place ->
                place.properties.kinds.contains("museum", ignoreCase = true)
            }
            appContext.getString(R.string.chip_nature) -> originalPlacesList.filter { place ->
                val kinds = place.properties.kinds
                kinds.contains("nature", ignoreCase = true) ||
                    kinds.contains("natural", ignoreCase = true) ||
                    kinds.contains("park", ignoreCase = true) ||
                    kinds.contains("garden", ignoreCase = true) ||
                    kinds.contains("water", ignoreCase = true) ||
                    kinds.contains("beach", ignoreCase = true)
            }
            appContext.getString(R.string.chip_food) -> originalPlacesList.filter { place ->
                val kinds = place.properties.kinds
                kinds.contains("food", ignoreCase = true) ||
                    kinds.contains("cafe", ignoreCase = true) ||
                    kinds.contains("restaurant", ignoreCase = true)
            }
            appContext.getString(R.string.chip_historic) -> originalPlacesList.filter { place ->
                val kinds = place.properties.kinds
                kinds.contains("historic", ignoreCase = true) ||
                    kinds.contains("monument", ignoreCase = true)
            }
            else -> originalPlacesList
        }

        _searchResults.value = ResultState.Success(filteredPlaces)
    }

    fun saveToFavorites(place: PlaceEntity, onCompleted: (isSaved: Boolean) -> Unit) {
        viewModelScope.launch {
            if (repository.isPlaceFavorite(place.xid)) {
                onCompleted(false)
                return@launch
            }
            repository.insertPlace(place)
            onCompleted(true)
        }
    }

    fun getPlaceDetails(xid: String) {
        viewModelScope.launch {
            _placeDetails.value = ResultState.Loading
            try {
                val details = repository.getPlaceDetails(xid, OPEN_TRIP_MAP_API_KEY)
                _placeDetails.value = ResultState.Success(details)
            } catch (e: Exception) {
                _placeDetails.value = ResultState.Error(
                    mapSearchErrorToMessage(e)
                )
            }
        }
    }

    suspend fun fetchPlaceImageUrl(xid: String): String? {
        imageUrlCacheMutex.withLock {
            if (imageUrlCache.containsKey(xid)) {
                val cachedUrl = imageUrlCache[xid]
                Log.d("TripExplorer_Image", "Using cached image URL for $xid: $cachedUrl")
                return cachedUrl
            }
        }

        return try {
            val details = repository.getPlaceDetails(xid, OPEN_TRIP_MAP_API_KEY)
            val url = details.preview?.source?.takeIf { it.isNotBlank() }
            if (url == null) {
                Log.w(
                    "TripExplorer_Image",
                    "No preview.source returned for $xid. preview=${details.preview}"
                )
            } else {
                Log.d("TripExplorer_Image", "Fetched URL for $xid: $url")
            }
            imageUrlCacheMutex.withLock {
                imageUrlCache[xid] = url
            }
            url
        } catch (e: Exception) {
            Log.e("TripExplorer_Image", "Error fetching image for $xid", e)
            imageUrlCacheMutex.withLock {
                imageUrlCache[xid] = null
            }
            null
        }
    }

    fun removeFromFavorites(place: PlaceEntity) {
        viewModelScope.launch {
            repository.deletePlace(place)
        }
    }

    fun updatePlace(place: PlaceEntity) {
        viewModelScope.launch {
            repository.updatePlace(place)
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            repository.clearAllFavoritePlaces()
        }
    }

    fun saveSearchRadiusKm(radiusKm: Int) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(PREF_SEARCH_RADIUS_KM, radiusKm)
            .apply()
    }

    fun getSavedSearchRadiusKm(): Int {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(PREF_SEARCH_RADIUS_KM, DEFAULT_RADIUS_KM)
    }

    private fun isLikelySameCity(userInput: String, resolvedCityName: String): Boolean {
        val normalizedInput = normalizeCityText(userInput)
        val normalizedResolved = normalizeCityText(resolvedCityName)
        if (normalizedInput.isBlank() || normalizedResolved.isBlank()) return false
        return normalizedResolved.contains(normalizedInput) || normalizedInput.contains(normalizedResolved)
    }

    private fun normalizeCityText(text: String): String {
        val withoutDiacritics = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return withoutDiacritics
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun mapSearchErrorToMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> appContext.getString(R.string.error_no_internet)
            is SocketTimeoutException -> appContext.getString(R.string.error_request_timeout)
            is IOException -> appContext.getString(R.string.error_network_generic)
            is HttpException -> {
                when (error.code()) {
                    400, 404 -> appContext.getString(R.string.city_not_found_error)
                    401, 403 -> appContext.getString(R.string.error_service_unavailable)
                    in 500..599 -> appContext.getString(R.string.error_server_generic)
                    else -> appContext.getString(R.string.generic_unexpected_error)
                }
            }
            else -> appContext.getString(R.string.generic_unexpected_error)
        }
    }

    private fun mapCitySearchErrorToMessage(error: Throwable): String {
        val networkOrServerMessage = mapSearchErrorToMessage(error)
        val genericMessage = appContext.getString(R.string.generic_unexpected_error)
        return if (networkOrServerMessage == genericMessage) {
            appContext.getString(R.string.city_not_found_error)
        } else {
            networkOrServerMessage
        }
    }
}
