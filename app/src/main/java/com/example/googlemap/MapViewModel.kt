package com.example.googlemap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.googlemap.modal.GeoPoint
import com.example.googlemap.modal.LocationResult
import com.example.googlemap.modal.MyLocation

class MapViewModel : ViewModel() {

    private val _data: MutableLiveData<LocationResult> = MutableLiveData<LocationResult>()
    val data : LiveData<LocationResult> = _data

    private val _locationPermissionGranted = MutableLiveData(false)
    val locationPermissionGranted = _locationPermissionGranted

    fun setData(location: LocationResult) {
        _data.value = location
    }

    fun setLocationPermissionGranted(permission : Boolean){
        _locationPermissionGranted.value = permission
    }

}