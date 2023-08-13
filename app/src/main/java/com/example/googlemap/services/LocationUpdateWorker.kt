package com.example.googlemap.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class LocationUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Start the foreground service to get and update the location
        ContextCompat.startForegroundService(
            applicationContext,
            Intent(applicationContext, LocationUpdateService::class.java)
        )

        return Result.success()
    }
}

