package com.example.googlemap.utils

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import com.example.googlemap.model.GeoPoint

class LocationUtils {

    // Function to generate a location link using Google Maps
    fun generateLocationLink(latitude: Double, longitude: Double, label: String): String {
        return "https://www.google.com/maps?q=$latitude,$longitude($label)"
    }

    // Function to open the generated location link in Google Maps app
    fun openLocationInGoogleMaps(locationLink: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(locationLink))
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Handle case where Google Maps app is not installed
            // You can also open the link in a web browser as a fallback
            openLinkInBrowser(locationLink,context)
        }
    }

    private fun openLinkInBrowser(link: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Handle case where no web browser app is available
            // Inside your onMapReady method

        }
    }

     fun calculateDistance(currLoc : GeoPoint, targetLoc : GeoPoint) : Float {
        val currentLocation = Location("CurrentLocation")
        currentLocation.latitude = currLoc.latitude
        currentLocation.longitude = currLoc.longitude

        val targetLocation = Location("TargetLocation")
        targetLocation.latitude = targetLoc.latitude
        targetLocation.longitude = targetLoc.longitude

        // Calculate the distance between the two locations
        return currentLocation.distanceTo(targetLocation)
    }


}