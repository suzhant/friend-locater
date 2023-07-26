package com.example.googlemap.listener

import com.example.googlemap.modal.LocationResult

interface OnPlaceFetchedListener {
    fun onPlacesFetched(locations : MutableList<LocationResult>)
    fun onFailure(errorMessage : String)
}