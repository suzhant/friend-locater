package com.example.googlemap

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class PlaceSearch {

    private val NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
    private val client: OkHttpClient = OkHttpClient()
    val gson: Gson = Gson()

    fun searchPlace(query: String, callback: Callback) {
        val url = NOMINATIM_URL.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("q", query)
            ?.build()

        val request = url?.let {
            Request.Builder()
                .url(it)
                .build()
        }

        if (request != null) {
            client.newCall(request).enqueue(callback)
        }
    }

    data class Place(
        @SerializedName("lat") val latitude: Double,
        @SerializedName("lon") val longitude: Double,
        @SerializedName("display_name") val displayName: String
    )
}
