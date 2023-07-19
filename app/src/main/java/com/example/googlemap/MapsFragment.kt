package com.example.googlemap

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.googlemap.databinding.FragmentMapsBinding
import com.example.googlemap.modal.DirectionResponse
import com.example.googlemap.services.ApiClient
import com.example.googlemap.services.LocationIQRoutingService
import com.example.googlemap.utils.Constants.API_KEY
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.PolyUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsFragment : Fragment() {

    private val binding: FragmentMapsBinding by lazy {
        FragmentMapsBinding.inflate(
            layoutInflater
        )
    }

    private val viewModel : MapViewModel by activityViewModels()
    private var curLatitude : Double= 0.0
    private var curLongitude : Double = 0.0
    private val zoomLevel = 15f
    private val duration = 1500
    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private lateinit var locationIQRoutingService: LocationIQRoutingService
    private var polyline: Polyline? = null
    private val POLYLINE_STROKE_WIDTH_PX = 12

    private val callback = OnMapReadyCallback { map ->
        googleMap = map
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isCompassEnabled = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mapFragment.getMapAsync(callback)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationIQRoutingService = ApiClient.retrofit.create(LocationIQRoutingService::class.java)

        viewModel.data.observe(viewLifecycleOwner){data ->
            val latitude = data.latitude.toDouble()
            val longitude = data.longitude.toDouble()
            val position = LatLng(latitude, longitude)
            getRoute(longitude,latitude)
            googleMap.addMarker(MarkerOptions().position(position).title(data.displayName))
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position,zoomLevel)
            googleMap.animateCamera(cameraUpdate,duration,null)
        }


        binding.fabCurrent.setOnClickListener {
            val position = LatLng(curLatitude,curLongitude)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position,zoomLevel)
            googleMap.animateCamera(cameraUpdate,duration,null)
        }

        viewModel.locationPermissionGranted.observe(viewLifecycleOwner){granted ->
            if (granted){
                getDeviceLocation()
            }
        }

    }


    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    val lastKnownLocation = task.result
                    googleMap.isMyLocationEnabled = true
                    curLatitude = lastKnownLocation.latitude
                    curLongitude = lastKnownLocation.longitude
                    val position = LatLng(curLatitude, curLongitude)
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position,zoomLevel)
                    googleMap.animateCamera(cameraUpdate,duration,null)
                } else {
                    Log.d("location", "Current location is null. Using defaults.")
                    Log.e("location", "Exception: %s", task.exception)
                    googleMap.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(LatLng(0.0,0.0), zoomLevel))
                    googleMap.isMyLocationEnabled = false
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getRoute(endLongitude: Double, endLatitude : Double) {
        val service = "directions"
        val profile = "driving"
        val startLongitude = curLongitude
        val startLatitude = curLatitude
        val coordinates = "${startLongitude},${startLatitude};${endLongitude},${endLatitude}"

        val call = locationIQRoutingService.getRoute(service, profile, coordinates, API_KEY)
        call.enqueue(object : Callback<DirectionResponse> {
            override fun onResponse(
                call: Call<DirectionResponse>,
                response: Response<DirectionResponse>
            ) {
                if (response.isSuccessful) {
                    val directionResponse = response.body()
                    Log.d("routes",directionResponse.toString())

                    clearPolyLine()
                    val route = directionResponse?.routes?.firstOrNull() // Get the first route
                    val routeGeometry = route?.geometry // Get the encoded polyline geometry of the route
                    val decodedRoutePoints = PolyUtil.decode(routeGeometry)
                    val polylineOptions = PolylineOptions()
                        .addAll(decodedRoutePoints.map { LatLng(it.latitude, it.longitude) })
                    polyline = googleMap.addPolyline(polylineOptions)
                    stylePolyLine()

                    // Handle the direction response
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: Call<DirectionResponse>, t: Throwable) {
                // Handle failure
            }
        })
    }

    private fun clearPolyLine(){
        polyline?.remove()
        polyline = null
    }

    private fun stylePolyLine(){
        polyline?.startCap = RoundCap()
        polyline?.endCap = RoundCap()
        polyline?.width = POLYLINE_STROKE_WIDTH_PX.toFloat()
        polyline?.color = requireContext().getColor(R.color.colorPrimary)
        polyline?.jointType = JointType.ROUND
    }


    override fun onResume() {
        super.onResume()
        mapFragment.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapFragment.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapFragment.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapFragment.onLowMemory()
    }

}