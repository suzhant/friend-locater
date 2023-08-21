package com.example.googlemap.model

import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.model.Distance
import com.google.maps.model.Duration
import com.google.maps.model.TravelMode

data class PolyLineModel (
    val travelMode : TravelMode,
    val polyline: PolylineOptions,
    val distance: Distance,
    val duration : Duration
)