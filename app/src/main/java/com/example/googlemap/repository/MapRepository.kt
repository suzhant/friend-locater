package com.example.googlemap.repository

import com.example.googlemap.BuildConfig
import com.example.googlemap.listener.OnPlaceFetchedListener
import com.example.googlemap.model.LocationResult
import com.example.googlemap.network.ApiClient
import com.example.googlemap.network.LocationIQService
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.NearbySearchRequest
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng
import com.google.maps.model.PlaceType
import com.google.maps.model.PlacesSearchResult
import com.google.maps.model.RankBy
import com.google.maps.model.TravelMode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MapRepository {

    private var locationIQService = ApiClient.retrofit.create(LocationIQService::class.java)
   // private val locationIQRoutingService = ApiClient.retrofit.create(LocationIQRoutingService::class.java)
    private var geoApiContext : GeoApiContext ?= null



    init {
        geoApiContext = GeoApiContext.Builder()
            .apiKey(BuildConfig.MAP_API_KEY)
            .build()
    }

    fun searchLocation(query: String, listener: OnPlaceFetchedListener) {
        val apiKey = com.example.googlemap.BuildConfig.LOCATIONIQ_API_KEY
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

//     fun getRoute(coordinates : String, listener: OnRouteFetchedListener) {
//        val service = "directions"
//        val profile = "driving"
//
//        val call = locationIQRoutingService.getRoute(service, profile, coordinates,
//            Constants.API_KEY
//        )
//        call.enqueue(object : Callback<DirectionResponse> {
//            override fun onResponse(
//                call: Call<DirectionResponse>,
//                response: Response<DirectionResponse>,
//            ) {
//                if (response.isSuccessful) {
//                    // Handle the direction response
//                    val directionResponse = response.body()
//                    Log.d("routes",directionResponse.toString())
//
//                    directionResponse?.let {
//                        listener.onRouteFetched(it)
//                    }
//                } else {
//                    // Handle error response
//                    listener.onFailure("Failed to route")
//                }
//            }
//
//            override fun onFailure(call: Call<DirectionResponse>, t: Throwable) {
//                // Handle failure
//                listener.onFailure("Failed to route")
//            }
//        })
//    }

    fun calculateDirection(origin: LatLng, destination: LatLng) : List<DirectionsResult>?{
        val results = mutableListOf<DirectionsResult>()

        val drivingRequest = DirectionsApiRequest(geoApiContext).alternatives(false)
            .mode(TravelMode.DRIVING)
            .origin(origin)
            .optimizeWaypoints(true)

            .destination(destination)

        val walkingRequest = DirectionsApiRequest(geoApiContext).alternatives(false)
                .mode(TravelMode.WALKING)
                .origin(origin)
                .optimizeWaypoints(true)
                .destination(destination)

        // Make the directions requests asynchronously
        val walkingResult = walkingRequest.await()
        val drivingResult = drivingRequest.await()

        results.add(drivingResult)
        results.add(walkingResult)

        return results
    }

    fun findHospitals(latitude: Double, longitude: Double): List<PlacesSearchResult> {

        // Create a NearbySearchRequest object.
        val hospitals = NearbySearchRequest(geoApiContext)
            .location(LatLng(latitude, longitude))
            .radius(1500) // 2 kilometers
            .type(PlaceType.HOSPITAL)
            .rankby(RankBy.PROMINENCE)
            .name("hospital")
            .keyword("hospital")
            .await()

        return hospitals.results.toList()
    }

}