package com.example.googlemap.ui.splash

import android.Manifest
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.googlemap.R
import com.example.googlemap.ui.LoginActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class StartScreenActivity : AppCompatActivity() {

    private val REQUEST_CHECK_SETTINGS = 102
   private var permissions = emptyArray<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
         permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        checkGps()
    }

    private fun goToNextActivity(){
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAfterTransition()
        },2500)
    }

    private fun checkPermission() {
        val notificationPermission = ContextCompat.checkSelfPermission(
            this,
            permissions[2]
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (!notificationPermission){
                requestNotificationPermissionLauncher.launch(
                    permissions[2]
                )
                return
            }
        }

        if (!checkLocationPermission()) {
            requestPermissionLauncher.launch(
                permissions
            )
            return
        }

        goToNextActivity()
    }

    private fun checkLocationPermission() : Boolean{
        val finePermission = ContextCompat.checkSelfPermission(
            this,
            permissions[0]
        ) == PackageManager.PERMISSION_GRANTED
        val coarsePermission = ContextCompat.checkSelfPermission(
            this,
            permissions[1]
        ) == PackageManager.PERMISSION_GRANTED

        if (!finePermission || !coarsePermission) {
            return false
        }
        return true
    }

    private val requestNotificationPermissionLauncher= registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted){
            showDialog()
        }else{
            if (!checkLocationPermission()) {
                requestPermissionLauncher.launch(
                    permissions
                )
            }
        }
    }
    private val requestPermissionLauncher= registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var locationGranted = false
        permissions.entries.forEach {
            val isGranted = it.value
            val permissionName = it.key
            if (permissionName == Manifest.permission.ACCESS_FINE_LOCATION || permissionName == Manifest.permission.ACCESS_COARSE_LOCATION) {
               locationGranted = isGranted
            }
        }
        if (!locationGranted) {
            showDialog()
        }else{
            goToNextActivity()
        }
    }

    private fun showDialog() {
        var message = ""
        message = getString(R.string.location_permission_message)

        val builder = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
        builder.setTitle(R.string.permission_required)
        builder.setMessage(message)
        builder.setPositiveButton(R.string.settings) { dialog, which ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
        builder.setNegativeButton(R.string.cancel) { _, _ ->
            finishAndRemoveTask()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun checkGps() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsEnabled){
            displayLocationSettingsRequest()
        }else{
            checkPermission()
        }
    }

    private fun displayLocationSettingsRequest() {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(LocationRequest.Builder.IMPLICIT_MIN_UPDATE_INTERVAL)
                .setMaxUpdateAgeMillis(20000)
                .build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
        result.addOnFailureListener {
            if (it is ResolvableApiException) {
                try {
                    it.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (e: SendIntentException) {
                    // Ignore the error.
                } catch (e: ClassCastException) {
                    // Ignore, should be an impossible error.
                }
            }
        }
    }

    override fun onRestart() {
        checkGps()
        super.onRestart()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS){
            if (resultCode == RESULT_OK){
                checkPermission()
            }else if (resultCode == RESULT_CANCELED){
                finishAndRemoveTask()
            }
        }
    }


}