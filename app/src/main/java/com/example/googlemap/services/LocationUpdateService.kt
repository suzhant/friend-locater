package com.example.googlemap.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.googlemap.R
import com.example.googlemap.model.UserLocation
import com.example.googlemap.ui.main.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Date


class LocationUpdateService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(LocationRequest.Builder.IMPLICIT_MIN_UPDATE_INTERVAL)
            .setMaxUpdateAgeMillis(20000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { updateLocationInDatabase(it) }
            }
        }
    }

    private fun createNotification() {
        // Create a notification channel if targeting Android Oreo (API 26) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CHANNEL_ID
            val channel = NotificationChannel(
                channelId,
                "Location Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            11,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, StopServiceReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Update")
            .setContentText("Fetching location updates")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.baseline_close_24,"Stop Service",stopPendingIntent)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the foreground service with a notification to keep it alive
        createNotification()
        requestLocationUpdates()
        return START_NOT_STICKY
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle case when location permission is not granted
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun updateLocationInDatabase(location: Location) {
        // Replace "users" with the appropriate node in your Firebase Realtime Database
        // Replace "userId" with the unique identifier of the user (e.g., user's ID or email)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser!=null){
            val databaseReference = FirebaseDatabase.getInstance().reference.child("location").child(auth.uid!!)
            val userLocation = UserLocation(
                id = auth.uid!!,
                latitude = location.latitude,
                longitude =  location.longitude,
                timestamp =  Date().time
            )
            databaseReference.setValue(userLocation)
        }else{
            fusedLocationClient.removeLocationUpdates(locationCallback)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "location_update"
    }
}
