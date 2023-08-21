package com.example.googlemap.listener

import com.google.maps.model.DirectionsResult

interface OnRouteFetchedListener {

    fun onRouteFetched(response: DirectionsResult)
    fun onFailure(errorMessage: String)
}