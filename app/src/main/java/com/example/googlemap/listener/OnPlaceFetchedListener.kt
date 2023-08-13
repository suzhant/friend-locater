package com.example.googlemap.listener

import com.example.googlemap.model.LocationResult

interface OnPlaceFetchedListener {
    fun onPlacesFetched(locations : MutableList<LocationResult>)
    fun onFailure(errorMessage : String)
}