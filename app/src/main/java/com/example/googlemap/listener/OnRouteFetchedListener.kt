package com.example.googlemap.listener

import com.example.googlemap.modal.DirectionResponse

interface OnRouteFetchedListener {

    fun onRouteFetched(response: DirectionResponse)
    fun onFailure(errorMessage: String)
}