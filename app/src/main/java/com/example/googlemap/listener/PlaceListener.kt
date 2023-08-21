package com.example.googlemap.listener

import com.example.googlemap.model.LocationResult
import com.google.android.libraries.places.api.model.AutocompletePrediction

interface PlaceListener {

    fun onPlaceClicked(place: AutocompletePrediction)
}