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
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.googlemap.R
import com.example.googlemap.databinding.FragmentMapsBinding
import com.example.googlemap.model.GeoPoint
import com.example.googlemap.model.PolyLineModel
import com.example.googlemap.model.UserLocation
import com.example.googlemap.ui.main.MainActivityViewModel
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.maps.android.PolyUtil
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MapsFragment : Fragment(), GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

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
    private lateinit var slideDownAnimation: Animation
    private lateinit var slideUpAnimation: Animation
    private val polyList = mutableListOf<PolyLineModel>()
    private val PATTERN_GAP_LENGTH_PX = 20
    private val DOT: PatternItem = Dot()
    private  val GAP: PatternItem = Gap(PATTERN_GAP_LENGTH_PX.toFloat())
    private val PATTERN_POLYLINE_DOTTED = listOf(GAP, DOT)
    private lateinit var placesClient: PlacesClient
    private lateinit var currentPlace : GeoPoint

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

        map.setOnMapClickListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
        slideUpAnimation = AnimationUtils.loadAnimation(requireContext(),R.anim.slide_up)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mapFragment.getMapAsync(callback)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        locationRequest = createLocationRequest()
        placesClient = Places.createClient(requireContext())
        checkGps()

        viewModel.destinationLiveData.observe(viewLifecycleOwner){ data ->
            val latlng = data.latLng
            val latitude = latlng?.latitude
            val longitude = latlng?.longitude
            val position = LatLng(latitude!!, longitude!!)
            val currentLocation = viewModel.currentLocation.value
            val startLongitude = currentLocation?.longitude
            val startLatitude = currentLocation?.latitude
            val origin = com.google.maps.model.LatLng(startLatitude!!,startLongitude!!)
            val destination = com.google.maps.model.LatLng(latitude,longitude)
            val route = GeoPoint()
            route.setCoordinate(startLongitude,startLatitude,longitude,latitude)
            viewModel.getRoute(origin,destination)
            currentPlace = GeoPoint(latitude,longitude)
            Log.d("routes",route.toString())
            googleMap?.addMarker(MarkerOptions().position(position).title(data.address))
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position,zoomLevel)
            googleMap?.animateCamera(cameraUpdate,duration,null)
        }

        viewModel.routeLiveData.observe(viewLifecycleOwner){directionResponse ->
            clearPolyLine()
            polyList.clear()

            directionResponse?.forEach{ r ->
                val route = r.routes[0]
                route?.let {
                    val polylinePoints = route.overviewPolyline.encodedPath
                    Log.d("places", polylinePoints)
                    lifecycleScope.launch(Dispatchers.IO) {
                        polylinePoints.let { points ->
                            val decodedPoints = PolyUtil.decode(points)
                            val polylineOptions = PolylineOptions()
                                .startCap(RoundCap())
                                .endCap(RoundCap())
                                .width(POLYLINE_STROKE_WIDTH_PX.toFloat())
                                .color(requireContext().getColor(R.color.colorPrimary))
                                .geodesic(true)
                                .jointType(JointType.ROUND)
                                .addAll(decodedPoints.map { LatLng(it.latitude, it.longitude) })
                            withContext(Dispatchers.Main){
                                val mode = route.legs[0].steps[0].travelMode
                                if (mode == TravelMode.DRIVING){
                                    val distanceValue = route.legs[0].distance
                                    val durationText = route.legs[0].duration
                                    binding.lytBottomsheetLocomotion.txtTime.text = durationText.toString()
                                    binding.lytBottomsheetLocomotion.txtDistance.text = "($distanceValue)"
                                    binding.lytBottomsheetLocomotion.chipCar.text = distanceValue.toString()
                                    val model = PolyLineModel(travelMode = TravelMode.DRIVING,polyline = polylineOptions, distance = distanceValue, duration = durationText)
                                    polyList.add(model)
                                    if (binding.lytBottomsheetLocomotion.chipCar.isChecked){
                                        polyline =  googleMap?.addPolyline(polylineOptions)
                                    }
                                }else if (mode == TravelMode.WALKING){
                                    val distanceValue = route.legs[0].distance
                                    val durationText = route.legs[0].duration
                                    binding.lytBottomsheetLocomotion.txtTime.text = durationText.toString()
                                    binding.lytBottomsheetLocomotion.txtDistance.text = "($distanceValue)"
                                    binding.lytBottomsheetLocomotion.chipWalk.text = distanceValue.toString()
                                    polylineOptions.pattern(PATTERN_POLYLINE_DOTTED)
                                    val model = PolyLineModel(travelMode = TravelMode.WALKING,polyline = polylineOptions ,distance = distanceValue, duration = durationText)
                                    polyList.add(model)
                                    if (binding.lytBottomsheetLocomotion.chipWalk.isChecked){
                                        polyline =  googleMap?.addPolyline(polylineOptions)
                                    }
                                }
                            }
                        }
                    }
                }

            }
            startShowAnimation()

        }

        binding.lytBottomsheetLocomotion.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            clearPolyLine()
            polyline = if (binding.lytBottomsheetLocomotion.chipCar.isChecked){
                val polylineOption =  polyList.find { it.travelMode == TravelMode.DRIVING }
                polylineOption?.apply {
                    binding.lytBottomsheetLocomotion.txtTime.text = duration.toString()
                    binding.lytBottomsheetLocomotion.txtDistance.text = "($distance)"
                }
                googleMap?.addPolyline(polylineOption?.polyline!!)
            }else{
                val polylineOption =  polyList.find { it.travelMode == TravelMode.WALKING }
                polylineOption?.apply {
                    binding.lytBottomsheetLocomotion.txtTime.text = duration.toString()
                    binding.lytBottomsheetLocomotion.txtDistance.text = "($distance)"
                }
                googleMap?.addPolyline(polylineOption?.polyline!!)
            }
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
            currentPlace = GeoPoint(latitude,longitude)
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


        slideDownAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Hide the layout after animation

            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })


        slideUpAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                // Show the layout before animation
                binding.lytBottomsheetLocomotion.root.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        binding.cardHospital.setOnClickListener {
            viewModel.findHospitals(currentPlace)
        }

        viewModel.hospitals.observe(viewLifecycleOwner){hospitals ->
            googleMap?.clear()
            val latlng = LatLng(currentPlace.latitude,currentPlace.longitude)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latlng,13f)
            googleMap?.animateCamera(cameraUpdate,duration,null)

            for (hospital in hospitals) {
                val latLng = LatLng(hospital.geometry.location.lat,hospital.geometry.location.lng)
                googleMap?.addMarker(MarkerOptions().position(latLng).title(hospital.name).snippet(hospital.vicinity))
            }
        }

        binding.imgReceiver.setOnClickListener {
           val location = viewModel.friendLocation.value
            location?.let {
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location,zoomLevel)
                googleMap?.moveCamera(cameraUpdate)
            }
        }

        populateMarkers()
    }

    private fun startHideAnimation() {
        binding.lytBottomsheetLocomotion.root.clearAnimation()
        binding.lytBottomsheetLocomotion.root.startAnimation(slideDownAnimation)
    }

    private fun startShowAnimation(){
        binding.lytBottomsheetLocomotion.root.clearAnimation()
        binding.lytBottomsheetLocomotion.root.startAnimation(slideUpAnimation)
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
                                viewModel.setFriendLocation(LatLng(lat,long))
                                loadIconWithGlide(
                                    iconUrl = pic,
                                    latitude = lat,
                                    longitude = long,
                                    name = name.toString()
                                )
                                loadTrackIcon(
                                    iconUrl = pic
                                )
                            }
                        }
                    }
                }else{
                    binding.imgReceiver.visibility = View.GONE
                    if (friendMarkerPosition!=null){
                        friendMarkerPosition?.remove()
                        friendMarkerPosition = null
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }
        markerReference = database.getReference("location").child(auth.uid!!)
        markerReference.addValueEventListener(markerListener)
    }

    private fun loadTrackIcon(iconUrl: String) {
        if (!binding.imgReceiver.isVisible){
            binding.imgReceiver.visibility = View.VISIBLE
            Glide.with(this).asBitmap()
                .load(iconUrl)
                .placeholder(R.drawable.img)
                .into(binding.imgReceiver)
        }
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
                    if (friendMarkerPosition == null){
                        Log.d("myMarker","bitmap null")
                        val markerOptions = MarkerOptions()
                            .position(LatLng(latitude!!, longitude!!))
                            .title(name)
                        friendMarkerPosition = googleMap?.addMarker(markerOptions)
                        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
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
                    Log.d("myMarker","bitmap failed")
                    if (friendMarkerPosition == null){
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
                    Firebase.crashlytics.recordException(e)
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
        val origin = com.google.maps.model.LatLng(startLatitude!!,startLongitude!!)
        val destination = com.google.maps.model.LatLng(markerLatitude,markerLongitude)
        viewModel.getRoute(origin,destination)
        return false
    }

    override fun onMapClick(p0: LatLng) {
        if (polyline !=null){
            if (binding.lytBottomsheetLocomotion.root.visibility == View.GONE){
                startShowAnimation()
            }else{
                startHideAnimation()
                binding.lytBottomsheetLocomotion.root.visibility = View.GONE
            }
        }

    }

}