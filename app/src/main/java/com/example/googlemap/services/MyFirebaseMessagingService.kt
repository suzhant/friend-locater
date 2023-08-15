package com.example.googlemap.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.googlemap.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private var title = ""
    private var body = ""

    override fun onNewToken(token: String) {
        Log.d("firebaseMessage", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)


        val data = message.data
        val notification = message.notification


        title = notification?.title ?: "Enpty title"
        body = notification?.body ?: "Empty body"
        Log.d("firebaseMessage","$title $body")

         val builder = NotificationCompat.Builder(this, FCM_CHANNEL_ID)
        builder.setContentTitle(title)
        builder.setAutoCancel(true)
        builder.setOnlyAlertOnce(true)
        builder.setSmallIcon(R.drawable.baseline_notifications_24)
        builder.setLights(-0xffff01, 200, 200)
        builder.setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = FCM_CHANNEL_ID
            val channel = NotificationChannel(
                channelId,
                "Fcm Notification channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(100, builder.build())
    }

    companion object{
        const val FCM_CHANNEL_ID = "FCM_CHANNEL_ID"
    }
}