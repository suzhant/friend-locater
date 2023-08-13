package com.example.googlemap.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.googlemap.model.UserLocation
import com.example.googlemap.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Date


class LocationUploadWorker (
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        //get Data out from input
        val longitude = inputData.getDouble(Constants.KEY_LONGITUDE, 0.0)
        val latitude = inputData.getDouble(Constants.KEY_LATITUDE, 0.0)
        //construct our report for server format
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        val userLocation = UserLocation(
            id = auth.uid!!,
            latitude = latitude,
            longitude =  longitude,
            timestamp =  Date().time
        )
        val myRef = database.getReference("location").child(auth.uid!!)
        myRef.setValue(userLocation)
            .addOnSuccessListener {
                Log.d("result","success")
            }.addOnFailureListener {
                Log.d("result","failed")
            }
        return Result.success()
    }


}