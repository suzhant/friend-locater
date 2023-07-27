package com.example.googlemap.ui.main.fragments

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.googlemap.R
import com.example.googlemap.databinding.FragmentMapsBinding
import com.example.googlemap.ui.main.MainActivityViewModel
import com.example.googlemap.ui.main.MapTypeBottomSheet
import com.example.googlemap.utils.Constants
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
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

class MapsFragment : Fragment() {

    private val REQUEST_CHECK_SETTINGS: Int = 123
    private val binding: FragmentMapsBinding by lazy {
        FragmentMapsBinding.inflate(
            layoutInflater
        )
    }

    private val viewModel : MainActivityViewModel by activityViewModels()
    private val zoomLevel = 15f
    private val duration = 1500
    private var googleMap: GoogleMap ?= null
    private lateinit var mapFragment: SupportMapFragment
    private var polyline: Polyline? = null
    private val POLYLINE_STROKE_WIDTH_PX = 12
    private lateinit var locationRequest: LocationRequest

    private val callback = OnMapReadyCallback { map ->
        googleMap = map
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
        googleMap?.uiSettings?.isCompassEnabled = false

        viewModel.mapType.observe(viewLifecycleOwner){mapType ->
            when(mapType){
                Constants.MAP_NORMAL -> {
                    googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                }
                Constants.MAP_SATELLITE -> {
                    googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                }
                Constants.MAP_HYBRID -> {
                    googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
                }
            }
        }
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

        locationRequest = createLocationRequest()
        checkGps()

        viewModel.destinationLiveData.observe(viewLifecycleOwner){ data ->
            val latitude = data.latitude.toDouble()
            val longitude = data.longitude.toDouble()
            val position = LatLng(latitude, longitude)
            val currentLocation = viewModel.currentLocation.value
            val startLongitude = currentLocation?.longitude
            val startLatitude = currentLocation?.latitude
            val coordinates = "${startLongitude},${startLatitude};${longitude},${latitude}"
            viewModel.getRoute(coordinates)
            googleMap?.addMarker(MarkerOptions().position(position).title(data.displayName))
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position,zoomLevel)
            googleMap?.animateCamera(cameraUpdate,duration,null)
        }

        viewModel.routeLiveData.observe(viewLifecycleOwner){directionResponse ->
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
            polyline = googleMap?.addPolyline(polylineOptions)
        }

        viewModel.gpsEnabled.observe(viewLifecycleOwner){enabled ->
            if (enabled){
                viewModel.getDeviceLocation()
            }
        }

        binding.fabCurrent.setOnClickListener {
            checkGps()
        }

        viewModel.currentLocation.observe(viewLifecycleOwner){location ->
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@observe
            }

            googleMap?.isMyLocationEnabled = true
            // Use the location data here
            val latitude = location.latitude
            val longitude = location.longitude
            val position = LatLng(latitude, longitude)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position,zoomLevel)
            googleMap?.moveCamera(cameraUpdate)
        }

        binding.fabTile.setOnClickListener {
            val modalBottomSheet = MapTypeBottomSheet()
            modalBottomSheet.show(childFragmentManager, MapTypeBottomSheet.TAG)
        }

    }

    private fun checkGps() {
        val locationManager = context?.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGPSEnabled) {
            // GPS is disabled
            displayLocationSettingsRequest()
        }else{
            viewModel.getDeviceLocation()
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

    override fun onStop() {
        super.onStop()
        // Stop location updates when the activity is not visible
        viewModel.locationRepository.stopLocationUpdates()
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
        viewModel.locationRepository.stopLocationUpdates()
        super.onDestroy()
        mapFragment.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapFragment.onLowMemory()
    }

}