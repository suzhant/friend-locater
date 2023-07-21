package com.example.googlemap.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.googlemap.MapViewModel
import com.example.googlemap.R
import com.example.googlemap.databinding.FragmentMapsBinding
import com.example.googlemap.modal.DirectionResponse
import com.example.googlemap.services.ApiClient
import com.example.googlemap.services.LocationIQRoutingService
import com.example.googlemap.utils.Constants.API_KEY
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
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

    private val REQUEST_CHECK_SETTINGS: Int = 123
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
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

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
        locationRequest = createLocationRequest()
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

        viewModel.gpsEnabled.observe(viewLifecycleOwner){enabled ->
            if (enabled){
                getDeviceLocation()
            }
        }

        binding.fabCurrent.setOnClickListener {
            checkGps()
        }

        getDeviceLocation()
        createLocationCallback()
    }

    private fun checkGps() {
        val locationManager = context?.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGPSEnabled) {
            // GPS is disabled
            displayLocationSettingsRequest()
        }else{
            getDeviceLocation()
        }
    }

    private fun displayLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result = LocationServices.getSettingsClient(requireActivity()).checkLocationSettings(builder.build())
        result.addOnFailureListener {
            if (it is ResolvableApiException) {
                try {
                    it.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                } catch (e: IntentSender.SendIntentException) {
                    // Ignore the error.
                } catch (e: ClassCastException) {
                    // Ignore, should be an impossible error.
                }
            }
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(LocationRequest.Builder.IMPLICIT_MIN_UPDATE_INTERVAL)
            .setMaxUpdateAgeMillis(20000)
            .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CHECK_SETTINGS){
            if (resultCode == RESULT_OK){
                getDeviceLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        try {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        googleMap.isMyLocationEnabled = true
                        // Use the location data here
                        val latitude = location.latitude
                        val longitude = location.longitude
                        curLatitude = latitude
                        curLongitude = longitude
                        val position = LatLng(curLatitude, curLongitude)
                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position,zoomLevel)
                        googleMap.animateCamera(cameraUpdate,duration,null)
                    } else {
                        // Location is null (no last known location available)
                        // You might want to request location updates instead
                        startLocationUpdates()
                        googleMap.isMyLocationEnabled = false
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors that occurred while getting the location
                }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Use the location data here
                    val latitude = location.latitude
                    val longitude = location.longitude
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    googleMap.isMyLocationEnabled = true
                    curLatitude = latitude
                    curLongitude = longitude
                    val position = LatLng(curLatitude, curLongitude)
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position,zoomLevel)
                    googleMap.animateCamera(cameraUpdate,duration,null)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                // This callback is triggered when the availability of location updates changes.
                // You can handle it as needed.
            }
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop location updates when the activity is not visible
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
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
                response: Response<DirectionResponse>,
            ) {
                if (response.isSuccessful) {
                    val directionResponse = response.body()
                    Log.d("routes",directionResponse.toString())

                    clearPolyLine()
                    val route = directionResponse?.routes?.firstOrNull() // Get the first route
                    val routeGeometry = route?.geometry // Get the encoded polyline geometry of the route
                    val decodedRoutePoints = PolyUtil.decode(routeGeometry)
                    val polylineOptions = PolylineOptions()
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                        .width(POLYLINE_STROKE_WIDTH_PX.toFloat())
                        .color(requireContext().getColor(R.color.colorPrimary))
                        .jointType(JointType.ROUND)
                        .addAll(decodedRoutePoints.map { LatLng(it.latitude, it.longitude) })
                    polyline = googleMap.addPolyline(polylineOptions)

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


    override fun onResume() {
        super.onResume()
        mapFragment.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapFragment.onPause()
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
        mapFragment.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapFragment.onLowMemory()
    }

}