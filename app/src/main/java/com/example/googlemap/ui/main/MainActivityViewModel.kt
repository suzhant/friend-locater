package com.example.googlemap.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.googlemap.listener.OnPlaceFetchedListener
import com.example.googlemap.listener.OnRouteFetchedListener
import com.example.googlemap.modal.DirectionResponse
import com.example.googlemap.modal.GeoPoint
import com.example.googlemap.modal.LocationResult
import com.example.googlemap.repository.LocationRepository
import com.example.googlemap.repository.MapRepository

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val mapRepository : MapRepository = MapRepository()
    val locationRepository : LocationRepository = LocationRepository(application)

    private val _placesListLiveData : MutableLiveData<MutableList<LocationResult>> = MutableLiveData()
    val placesLiveData: LiveData<MutableList<LocationResult>> get() = _placesListLiveData

    private val _routeLiveData : MutableLiveData<DirectionResponse> = MutableLiveData()
    val routeLiveData : LiveData<DirectionResponse> get() = _routeLiveData

    private val _destinationLivedata: MutableLiveData<LocationResult> = MutableLiveData<LocationResult>()
    val destinationLiveData : LiveData<LocationResult> = _destinationLivedata

    private val _currentLocation : MutableLiveData<GeoPoint> = MutableLiveData()
    val currentLocation :LiveData<GeoPoint> get() = _currentLocation

    private val _gpsEnabled = MutableLiveData(false)
    val gpsEnabled = _gpsEnabled

    fun setDestinationData(location: LocationResult) {
        _destinationLivedata.value = location
    }

    fun setGps(enabled : Boolean){
        _gpsEnabled.value = enabled
    }

    fun fetchPlaces(query: String) {
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

    fun getRoute(coordinates : String){
        mapRepository.getRoute(coordinates,object : OnRouteFetchedListener{
            override fun onRouteFetched(response: DirectionResponse) {
                _routeLiveData.postValue(response)
            }

            override fun onFailure(errorMessage: String) {
                Log.d("error",errorMessage)
            }

        })
    }

    fun getDeviceLocation(){
        locationRepository.getDeviceLocation(object : LocationRepository.OnLocationFetchedListener{
            override fun onLocationFetched(location: GeoPoint) {
                _currentLocation.postValue(location)
            }

            override fun onFailure(errorMessage: String) {
                locationRepository.startLocationUpdates()
            }

        })
    }


}