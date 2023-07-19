package com.example.googlemap.listener

import com.example.googlemap.modal.LocationResult

interface PlaceListener {

    fun onPlaceClicked(place: LocationResult)
}