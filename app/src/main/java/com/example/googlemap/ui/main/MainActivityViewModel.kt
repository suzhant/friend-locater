package com.example.googlemap.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.googlemap.listener.OnPlaceFetchedListener
import com.example.googlemap.model.GeoPoint
import com.example.googlemap.model.LocationResult
import com.example.googlemap.repository.LocationRepository
import com.example.googlemap.repository.MapRepository
import com.example.googlemap.utils.Constants
import com.google.android.libraries.places.api.model.Place
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val mapRepository : MapRepository = MapRepository()
    val locationRepository : LocationRepository = LocationRepository(application, object : LocationRepository.OnLocationFetchedListener{
        override fun onLocationFetched(location: GeoPoint) {
            _currentLocation.postValue(location)
        }

        override fun onFailure(errorMessage: String) {
            Log.d("error",errorMessage)
        }

    })

    private val _placesListLiveData : MutableLiveData<MutableList<LocationResult>> = MutableLiveData()
    val placesLiveData: LiveData<MutableList<LocationResult>> get() = _placesListLiveData

    private val _routeLiveData : MutableLiveData<List<DirectionsResult>?> = MutableLiveData()
    val routeLiveData : LiveData<List<DirectionsResult>?> get() = _routeLiveData

    private val _destinationLivedata: MutableLiveData<Place> = MutableLiveData<Place>()
    val destinationLiveData : LiveData<Place> = _destinationLivedata

    private val _currentLocation : MutableLiveData<GeoPoint> = MutableLiveData()
    val currentLocation :LiveData<GeoPoint> get() = _currentLocation

    private val _gpsEnabled = MutableLiveData(false)
    val gpsEnabled = _gpsEnabled

    private val _linkEnabled = MutableLiveData(false)
    val linkEnabled : LiveData<Boolean> get() = _linkEnabled

    private val _mapType = MutableLiveData(Constants.MAP_NORMAL)
    val mapType get() = _mapType

    fun setMapType(mapType : Int){
        _mapType.value = mapType
    }

    fun setLinkEnabled(enabled: Boolean){
        _linkEnabled.value = enabled
    }

    fun setDestinationData(location: Place) {
        _destinationLivedata.postValue(location)
    }

    fun setGps(enabled : Boolean){
        _gpsEnabled.value = enabled
    }

     fun fetchPlaces(query: String) {
         viewModelScope.launch(Dispatchers.IO) {
             mapRepository.searchLocation(query, object : OnPlaceFetchedListener{
                 override fun onPlacesFetched(locations: MutableList<LocationResult>) {
                     // setValue() should be used when updating the LiveData from the main/UI thread
                     // postValue() should be used when updating the LiveData from a background thread
                     _placesListLiveData.postValue(locations)
                 }

                 override fun onFailure(errorMessage: String) {
                     Log.d("error",errorMessage)
                 }
             })
         }
    }


    fun getRoute(origin: LatLng,destination : LatLng){
        viewModelScope.launch(Dispatchers.IO) {
             val results =  mapRepository.calculateDirection(
                origin = origin,
                destination = destination)
            _routeLiveData.postValue(results)
        }
    }

    fun getDeviceLocation(){
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.getDeviceLocation()
        }
    }


}