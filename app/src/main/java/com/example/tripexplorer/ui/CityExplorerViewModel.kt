package com.example.tripexplorer.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripexplorer.data.local.PlaceEntity
import com.example.tripexplorer.data.remote.PlaceFeature
import com.example.tripexplorer.data.remote.PlaceDetailsResponse
import com.example.tripexplorer.data.repository.CityExplorerRepository
import com.example.tripexplorer.utils.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CityExplorerViewModel @Inject constructor(
    private val repository: CityExplorerRepository
) : ViewModel() {

    companion object {
        private const val OPEN_TRIP_MAP_API_KEY = "YOUR_API_KEY_HERE"
    }

    private val _searchResults = MutableLiveData<ResultState<List<PlaceFeature>>>()
    val searchResults: LiveData<ResultState<List<PlaceFeature>>> = _searchResults

    private val _placeDetails = MutableLiveData<ResultState<PlaceDetailsResponse>>()
    val placeDetails: LiveData<ResultState<PlaceDetailsResponse>> = _placeDetails

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
                val cityCoordinates = repository.getCityCoordinates(cityName, OPEN_TRIP_MAP_API_KEY)
                val placesResponse = repository.getPlacesInRadius(
                    radius = 5000,
                    lon = cityCoordinates.lon,
                    lat = cityCoordinates.lat,
                    apiKey = OPEN_TRIP_MAP_API_KEY
                )
                _searchResults.value = ResultState.Success(placesResponse.features)
            } catch (e: Exception) {
                _searchResults.value = ResultState.Error(
                    e.message ?: "An unexpected error occurred."
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
                    e.message ?: "An unexpected error occurred."
                )
            }
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
}
