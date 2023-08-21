package com.example.googlemap.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
  //  private const val BASE_URL = "https://us1.locationiq.com/v1/"
    private const val BASE_URL =  "https://maps.googleapis.com/maps/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}