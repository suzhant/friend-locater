package com.example.googlemap.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.googlemap.utils.Constants

class LocationUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Start the foreground service to get and update the location
        val senderId = inputData.getString(Constants.SENDER_ID)
        val receiverId = inputData.getString(Constants.RECEIVER_ID)
        val intent =  Intent(applicationContext, LocationUpdateService::class.java)
        intent.putExtra(Constants.SENDER_ID,senderId)
        intent.putExtra(Constants.RECEIVER_ID, receiverId)
        ContextCompat.startForegroundService(
            applicationContext,
            intent
        )

        return Result.success()
    }
}

