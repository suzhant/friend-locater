package com.example.googlemap.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.googlemap.services.LocationUpdateWorker
import com.example.googlemap.utils.Constants

class NotificationAcceptReceiver : BroadcastReceiver() {

    private var senderId : String = ""
    private var receiverId : String = ""

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.extras!=null){
            senderId = intent.getStringExtra(Constants.SENDER_ID)!!
            receiverId = intent.getStringExtra(Constants.RECEIVER_ID)!!
            scheduleLocationUpdates(context)
        }
    }

    private fun scheduleLocationUpdates(context: Context?) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a OneTimeWorkRequest to trigger the Worker
        val locationUpdateWorkRequest = OneTimeWorkRequest.Builder(LocationUpdateWorker::class.java)
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                Constants.SENDER_ID to senderId,
                Constants.RECEIVER_ID to receiverId)
            )
            .build()

        val workManager = context?.let { WorkManager.getInstance(it) }
        workManager?.enqueue(locationUpdateWorkRequest)
    }
}