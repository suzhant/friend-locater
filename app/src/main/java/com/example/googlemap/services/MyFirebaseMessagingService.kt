package com.example.googlemap.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.googlemap.R
import com.example.googlemap.receiver.NotificationAcceptReceiver
import com.example.googlemap.receiver.NotificationRemovedReceiver
import com.example.googlemap.ui.main.MainActivity
import com.example.googlemap.utils.BitmapUtils
import com.example.googlemap.utils.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var notificationManager : NotificationManager

    override fun onNewToken(token: String) {
        Log.d("firebaseMessage", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)


        val data = message.data
        Log.d("firebaseMessage", data.toString())

        //use data object for consistent notification.
        // using notification object will cause issue if app is in background
        val title = data["title"]
        val body = data["body"]
        val imgUrl = data["profilePic"]
        val senderId = data["senderId"]
        val receiverId = data["receiverId"]
        val bitmap = BitmapUtils(this).getBitmapFromUrl(imgUrl)
        val soundPath = "android.resource://" + packageName + "/" + R.raw.incoming_sound

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            notificationIntent,
           PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(applicationContext, NotificationAcceptReceiver::class.java)
        acceptIntent.putExtra(Constants.SENDER_ID,senderId)
        acceptIntent.putExtra(Constants.RECEIVER_ID,receiverId)
        acceptIntent.action = "ACTION_ACCEPT"
        val grantPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            2,
            acceptIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(applicationContext, NotificationRemovedReceiver::class.java)
        rejectIntent.putExtra(Constants.SENDER_ID,senderId)
        rejectIntent.putExtra(Constants.RECEIVER_ID,receiverId)
        val denyPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            3,
            rejectIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, FCM_CHANNEL_ID)
            .apply {
            setContentTitle(title)
            setContentText(body)
            setContentIntent(pendingIntent)
            setLargeIcon(bitmap)
            setSmallIcon(R.drawable.baseline_notifications_24)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            setOngoing(true)
            setTimeoutAfter(60000)
            addAction(
                R.drawable.ic_accept,
                "Accept",
                grantPendingIntent
            )
            addAction(
                    R.drawable.ic_reject,
                    "Reject",
                     denyPendingIntent
                )
            priority = NotificationCompat.PRIORITY_MAX
            setCategory(NotificationCompat.CATEGORY_CALL)
            setSmallIcon(R.drawable.baseline_notifications_24)
            setLights(Color.BLUE,500,500)
        }


        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FCM_CHANNEL_ID,
                "Fcm Notification channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val att = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            channel.setSound(Uri.parse(soundPath),att)
            notificationManager.createNotificationChannel(channel)
        }
        val note = builder.build()
        note.flags = Notification.FLAG_INSISTENT
        notificationManager.notify(NOTIFICATION_ID, note)
    }

    companion object{
        const val FCM_CHANNEL_ID = "FCM_CHANNEL_ID"
        const val NOTIFICATION_ID = 100
    }

}