package com.example.googlemap.repository

import android.util.Log
import com.example.googlemap.listener.OnPlaceFetchedListener
import com.example.googlemap.listener.OnRouteFetchedListener
import com.example.googlemap.model.DirectionResponse
import com.example.googlemap.model.LocationResult
import com.example.googlemap.network.ApiClient
import com.example.googlemap.network.LocationIQRoutingService
import com.example.googlemap.network.LocationIQService
import com.example.googlemap.utils.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapRepository {

    private var locationIQService = ApiClient.retrofit.create(LocationIQService::class.java)
    private val locationIQRoutingService = ApiClient.retrofit.create(LocationIQRoutingService::class.java)



    fun searchLocation(query: String, listener: OnPlaceFetchedListener) {
        val apiKey = Constants.API_KEY
        val call = locationIQService.searchLocation(apiKey, query)
        call.enqueue(object : Callback<List<LocationResult>> {
            override fun onResponse(
                call: Call<List<LocationResult>>,
                response: Response<List<LocationResult>>,
            ) {
                if (response.isSuccessful) {
                    val locations = response.body()
                    listener.onPlacesFetched(locations = locations as MutableList<LocationResult>)
                    // Handle the list of locations returned
                } else {
                    // Handle error response
                    listener.onFailure(response.errorBody().toString())
                }
            }

            override fun onFailure(call: Call<List<LocationResult>>, t: Throwable) {
                // Handle failure
                listener.onFailure("Failed to fetch data")
            }
        })
    }

     fun getRoute(coordinates : String, listener: OnRouteFetchedListener) {
        val service = "directions"
        val profile = "driving"

        val call = locationIQRoutingService.getRoute(service, profile, coordinates,
            Constants.API_KEY
        )
        call.enqueue(object : Callback<DirectionResponse> {
            override fun onResponse(
                call: Call<DirectionResponse>,
                response: Response<DirectionResponse>,
            ) {
                if (response.isSuccessful) {
                    // Handle the direction response
                    val directionResponse = response.body()
                    Log.d("routes",directionResponse.toString())

                    directionResponse?.let {
                        listener.onRouteFetched(it)
                    }
                } else {
                    // Handle error response
                    listener.onFailure("Failed to route")
                }
            }

            override fun onFailure(call: Call<DirectionResponse>, t: Throwable) {
                // Handle failure
                listener.onFailure("Failed to route")
            }
        })
    }


}