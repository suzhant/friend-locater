package com.example.googlemap.listener

import com.example.googlemap.model.LocationResult

interface PlaceListener {

    fun onPlaceClicked(place: LocationResult)
}