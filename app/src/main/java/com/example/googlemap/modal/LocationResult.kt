package com.example.googlemap.modal

import com.google.gson.annotations.SerializedName

data class LocationResult(
    @SerializedName("place_id") val placeId: String,
    @SerializedName("licence") val licence: String,
    @SerializedName("osm_type") val osmType: String,
    @SerializedName("osm_id") val osmId: String,
    @SerializedName("boundingbox") val boundingBox: List<String>,
    @SerializedName("lat") val latitude: String,
    @SerializedName("lon") val longitude: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("class") val classType: String,
    @SerializedName("type") val type: String,
    @SerializedName("importance") val importance: Double,
    @SerializedName("icon") val icon: String
)