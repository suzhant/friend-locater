package com.example.googlemap.ui.main.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.googlemap.R
import com.example.googlemap.databinding.FragmentMapsBinding
import com.example.googlemap.model.GeoPoint
import com.example.googlemap.model.UserLocation
import com.example.googlemap.services.LocationUploadWorker
import com.example.googlemap.ui.main.MainActivityViewModel
import com.example.googlemap.utils.Constants
import com.example.googlemap.utils.Constants.KEY_LATITUDE
import com.example.googlemap.utils.Constants.KEY_LONGITUDE
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.maps.android.PolyUtil


class MapsFragment : Fragment(), GoogleMap.OnMarkerClickListener {

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
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var friendMarkerPosition : Marker ?= null
    private lateinit var markerListener: ValueEventListener
    private lateinit var markerReference: DatabaseReference
    private var bitmapDescriptor: BitmapDescriptor ?= null

    @SuppressLint("PotentialBehaviorOverride")
    private val callback = OnMapReadyCallback { map ->
        googleMap = map
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false
        googleMap?.uiSettings?.isCompassEnabled = false
        googleMap?.setOnMarkerClickListener(this)
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

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        locationRequest = createLocationRequest()
        checkGps()

        viewModel.destinationLiveData.observe(viewLifecycleOwner){ data ->
            val latitude = data.latitude.toDouble()
            val longitude = data.longitude.toDouble()
            val position = LatLng(latitude, longitude)
            val currentLocation = viewModel.currentLocation.value
            val startLongitude = currentLocation?.longitude
            val startLatitude = currentLocation?.latitude
            val route = GeoPoint()
            route.setCoordinate(startLongitude!!,startLatitude!!,longitude,latitude)
            viewModel.getRoute(route.getCoordinate())
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
         //   uploadLocation(position)
        }

        binding.fabTile.setOnClickListener {
            findNavController().navigate(R.id.action_mapsFragment_to_mapTypeBottomSheet)
        }

        binding.fabFriend.setOnClickListener{
            findNavController().navigate(R.id.action_mapsFragment_to_friendBottomSheet)
        }

        populateMarkers()
    }

    private fun populateMarkers(){
        markerListener = object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    for (dataSnap in snapshot.children){
                        Log.d("myMarker","snap called")
                        val location = dataSnap.getValue(UserLocation::class.java)
                        val lat = location?.latitude
                        val long = location?.longitude
                        val userData = location?.userData
                        userData?.let {
                            val pic = userData.profilePicUrl ?: ""
                            val name = userData.userName
                            if (lat != null && long != null){
                                loadIconWithGlide(
                                    iconUrl = pic,
                                    latitude = lat,
                                    longitude = long,
                                    name = name.toString()
                                )
                            }
                        }
                    }
                }else{
                    if (friendMarkerPosition!=null){
                        friendMarkerPosition?.remove()
                        bitmapDescriptor = null
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }
        markerReference = database.getReference("location").child(auth.uid!!)
        markerReference.addValueEventListener(markerListener)
    }

    private fun loadIconWithGlide(iconUrl: String?,latitude : Double?, longitude : Double?,name: String?) {
        Log.d("myMarker","function called")
        Glide.with(this)
            .asBitmap()
            .load(iconUrl)
            .placeholder(R.drawable.img)
            .circleCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d("myMarker","bitmap success")
                    if (bitmapDescriptor == null){
                        Log.d("myMarker","bitmap null")
                        val markerOptions = MarkerOptions()
                            .position(LatLng(latitude!!, longitude!!))
                            .title(name)
                        friendMarkerPosition = googleMap?.addMarker(markerOptions)
                        bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
                        friendMarkerPosition?.setIcon(bitmapDescriptor)
                        return
                    }
                    friendMarkerPosition?.position = LatLng(latitude!!,longitude!!)

                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Called when the resource is cleared.
                    // You can optionally handle this case if needed.
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    // Called when the image failed to load.
                    // You can handle this case if needed.
                    Log.d("myMarker","bitmap failed")
                    if (bitmapDescriptor == null){
                        val markerOptions = MarkerOptions()
                            .position(LatLng(latitude!!, longitude!!))
                            .title(name)
                        friendMarkerPosition = googleMap?.addMarker(markerOptions)
                        val drawable = ContextCompat.getDrawable(requireContext(),R.drawable.img)
                        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(drawable!!.toBitmap(100 ,100))
                        friendMarkerPosition?.setIcon(bitmapDescriptor)
                        return
                    }
                    friendMarkerPosition?.position = LatLng(latitude!!,longitude!!)
                }

            })
    }

    private fun uploadLocation(position: LatLng) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()


        val inputData: Data = Data.Builder()
            .putDouble(KEY_LATITUDE, position.latitude)
            .putDouble(KEY_LONGITUDE, position.longitude)
            .build()

        val uploadWork: OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(LocationUploadWorker::class.java).setConstraints(constraints)
                .setInputData(inputData).build()

        WorkManager.getInstance(requireContext().applicationContext).enqueue(uploadWork)

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
        markerReference.removeEventListener(markerListener)
        super.onDestroy()
        mapFragment.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapFragment.onLowMemory()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Get the latitude and longitude of the clicked marker
        val markerLatLng = marker.position
        val markerLatitude = markerLatLng.latitude
        val markerLongitude = markerLatLng.longitude

        val currentLocation = viewModel.currentLocation.value
        val startLongitude = currentLocation?.longitude
        val startLatitude = currentLocation?.latitude
        val coordinates = "${startLongitude},${startLatitude};${markerLongitude},${markerLatitude}"
        viewModel.getRoute(coordinates)
        // Return false to allow the default behavior (opening the info window, if available)
        // Return true to prevent the default behavior from happening
        return false
    }

}