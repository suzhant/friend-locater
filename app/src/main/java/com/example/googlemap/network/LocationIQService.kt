package com.example.googlemap.network

import com.example.googlemap.model.LocationResult
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationIQService {
    @GET("search")
    fun searchLocation(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("countrycodes") countryCode : String = "np"
    ): Call<List<LocationResult>>
}

