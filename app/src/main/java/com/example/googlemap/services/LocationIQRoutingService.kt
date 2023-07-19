package com.example.googlemap.services

import com.example.googlemap.modal.DirectionResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap


interface LocationIQRoutingService {
    @GET("{service}/{profile}/{coordinates}")
    fun getRoute(
        @Path("service") service: String,
        @Path("profile") profile: String,
        @Path("coordinates") coordinates: String,
        @Query("key") apiKey: String
    ): Call<DirectionResponse>
}