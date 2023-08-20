package com.example.googlemap.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.googlemap.services.MyFirebaseMessagingService
import com.example.googlemap.utils.Constants
import com.google.firebase.database.FirebaseDatabase


class NotificationRemovedReceiver : BroadcastReceiver() {
    private var senderId = ""
    private var receiverId = ""
    override fun onReceive(context: Context?, intent: Intent?) {
        // Stop the NotificationSoundService when the notification is removed
        if (intent!=null && intent.extras!=null){
            senderId = intent.getStringExtra(Constants.SENDER_ID)!!
            receiverId = intent.getStringExtra(Constants.RECEIVER_ID)!!
            removeLocation()
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(MyFirebaseMessagingService.NOTIFICATION_ID)
        }
    }

    private fun removeLocation(){
        val databaseReference = FirebaseDatabase.getInstance().reference.child("location")
        databaseReference.child(senderId).child(receiverId).removeValue()
    }
}
