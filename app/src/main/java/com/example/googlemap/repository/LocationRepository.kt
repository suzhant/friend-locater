package com.example.googlemap.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.googlemap.model.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationRepository(private val context:Context, private val listener: OnLocationFetchedListener) {

    private var fusedLocationProviderClient : FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationRequest: LocationRequest = createLocationRequest()

    interface OnLocationFetchedListener{
        fun onLocationFetched(location : GeoPoint)
        fun onFailure(errorMessage: String)
    }

    private var locationCallback: LocationCallback  = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                // Use the location data here
                val latitude = location.latitude
                val longitude = location.longitude
                val currentLocation = GeoPoint(latitude, longitude)
                listener.onLocationFetched(currentLocation)
                stopLocationUpdates()
            }
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            // This callback is triggered when the availability of location updates changes.
            // You can handle it as needed.
        }
    }

     fun getDeviceLocation() {
        try {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        // Use the location data here
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val currentLocation = GeoPoint(latitude,longitude)
                        listener.onLocationFetched(currentLocation)
                    } else {
                        // Location is null (no last known location available)
                        // You might want to request location updates instead
                        startLocationUpdates()
                        listener.onFailure("Failed to fetch data")
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors that occurred while getting the location
                    listener.onFailure("Failed to fetch data")
                }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

     private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(LocationRequest.Builder.IMPLICIT_MIN_UPDATE_INTERVAL)
            .setMaxUpdateAgeMillis(20000)
            .build()
    }

     fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}