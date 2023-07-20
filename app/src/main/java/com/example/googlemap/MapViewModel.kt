package com.example.googlemap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.googlemap.modal.LocationResult

class MapViewModel : ViewModel() {

    private val _data: MutableLiveData<LocationResult> = MutableLiveData<LocationResult>()
    val data : LiveData<LocationResult> = _data

    private val _gpsEnabled = MutableLiveData(false)
    val gpsEnabled = _gpsEnabled

    fun setData(location: LocationResult) {
        _data.value = location
    }

    fun setGps(enabled : Boolean){
        _gpsEnabled.value = enabled
    }

}