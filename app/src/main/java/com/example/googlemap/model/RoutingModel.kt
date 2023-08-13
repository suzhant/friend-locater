package com.example.googlemap.model

import com.google.gson.annotations.SerializedName

data class DirectionResponse(
    @SerializedName("code") val code: String,
    @SerializedName("routes") val routes: List<Route>,
    @SerializedName("waypoints") val waypoints: List<Waypoint>
)

data class Route(
    @SerializedName("geometry") val geometry: String,
    @SerializedName("legs") val legs: List<Leg>,
    @SerializedName("weight_name") val weightName: String,
    @SerializedName("weight") val weight: Double,
    @SerializedName("duration") val duration: Double,
    @SerializedName("distance") val distance: Double
)

data class Leg(
    @SerializedName("steps") val steps: List<Step>,
    @SerializedName("summary") val summary: String,
    @SerializedName("weight") val weight: Double,
    @SerializedName("duration") val duration: Double,
    @SerializedName("distance") val distance: Double
)

data class Step(
    @SerializedName("geometry") val geometry: String,
    @SerializedName("maneuver") val maneuver: Maneuver,
    @SerializedName("mode") val mode: String,
    @SerializedName("driving_side") val drivingSide: String,
    @SerializedName("name") val name: String,
    @SerializedName("intersections") val intersections: List<Intersection>,
    @SerializedName("weight") val weight: Double,
    @SerializedName("duration") val duration: Double,
    @SerializedName("distance") val distance: Double
)

data class Maneuver(
    @SerializedName("bearing_after") val bearingAfter: Int,
    @SerializedName("bearing_before") val bearingBefore: Int,
    @SerializedName("location") val location: List<Double>,
    @SerializedName("modifier") val modifier: String,
    @SerializedName("type") val type: String
)

data class Intersection(
    @SerializedName("out") val out: Int,
    @SerializedName("in") val `in`: Int? = null,
    @SerializedName("entry") val entry: List<Boolean>,
    @SerializedName("bearings") val bearings: List<Int>,
    @SerializedName("location") val location: List<Double>
)

data class Waypoint(
    @SerializedName("hint") val hint: String,
    @SerializedName("distance") val distance: Double,
    @SerializedName("name") val name: String,
    @SerializedName("location") val location: List<Double>
)
