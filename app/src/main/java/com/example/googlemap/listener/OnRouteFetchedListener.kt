package com.example.googlemap.listener

import com.example.googlemap.model.DirectionResponse

interface OnRouteFetchedListener {

    fun onRouteFetched(response: DirectionResponse)
    fun onFailure(errorMessage: String)
}