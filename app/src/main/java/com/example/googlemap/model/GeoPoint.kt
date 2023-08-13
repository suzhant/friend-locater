package com.example.googlemap.model

data class GeoPoint(
    val latitude : Double,
    val longitude: Double
){
    constructor() : this(0.0,0.0)

    private var coordinates : String = ""

    fun setCoordinate(startLong: Double, startLat : Double, endLong : Double, endLat : Double ){
         coordinates = "${startLong},${startLat};${endLong},${endLat}"
    }

    fun getCoordinate() = coordinates
}