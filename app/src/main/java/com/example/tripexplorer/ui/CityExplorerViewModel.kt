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
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
                _searchResults.value = ResultState.Success(placesResponse.features)
            } catch (e: Exception) {
                _searchResults.value = ResultState.Error(
                    e.message ?: appContext.getString(R.string.generic_unexpected_error)
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
                _searchResults.value = ResultState.Success(placesResponse.features)
            } catch (e: Exception) {
                _searchResults.value = ResultState.Error(
                    e.message ?: appContext.getString(R.string.generic_unexpected_error)
                )
            }
        }
    }

    fun saveToFavorites(place: PlaceEntity) {
        viewModelScope.launch {
            repository.insertPlace(place)
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
                    e.message ?: appContext.getString(R.string.generic_unexpected_error)
                )
            }
        }
    }

    suspend fun fetchPlaceImageUrl(xid: String): String? {
        imageUrlCacheMutex.withLock {
            if (imageUrlCache.containsKey(xid)) {
                return imageUrlCache[xid]
            }
        }

        return try {
            val details = repository.getPlaceDetails(xid, OPEN_TRIP_MAP_API_KEY)
            val url = details.preview?.source
            Log.d("TripExplorer_Image", "Fetched URL for $xid: $url")
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
}
