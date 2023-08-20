package com.example.googlemap.services

import android.content.Context
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.googlemap.model.Friend
import com.example.googlemap.model.NotificationModel
import com.example.googlemap.model.UserData
import com.example.googlemap.model.UserLocation
import com.example.googlemap.model.enums.TrackStatus
import com.example.googlemap.utils.FcmNotification
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import java.util.Date


class LocationUploadWorker (
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        //get Data out from input
        val data1 = inputData.getString("friend")
        val data2 = inputData.getString("userData")
        val friend = Gson().fromJson(data1, Friend::class.java)
        val userData = Gson().fromJson(data2, UserData::class.java)
        locateUser(friend, userData)
        return Result.success()
    }
    private fun locateUser(friend: Friend,userData: UserData) {
        val senderId = userData.userId
        val receiverId = friend.userData?.userId
        val receiverObj =  UserLocation(
            longitude = null,
            latitude = null,
            status = TrackStatus.PENDING.name,
            timestamp = Date().time,
            userData = userData
        )
        val locationRef = FirebaseDatabase.getInstance().getReference("location")
        locationRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val data = snapshot.value as? Map<*, *>
                    if (data?.containsKey(senderId) == true) {
                        Toast.makeText(applicationContext,"You can track only one user at a time",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        sendNotification(friend,userData)
                        locationRef.child(senderId).child(receiverId!!).setValue(receiverObj)
                    }
                }else{
                    sendNotification(friend,userData)
                    locationRef.child(senderId).child(receiverId!!).setValue(receiverObj)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

    }


    private fun sendNotification(friend: Friend,userData: UserData){
        FirebaseDatabase.getInstance().getReference("Token").child(friend.userData?.userId!!).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val token = snapshot.child("token").getValue(String::class.java)
                    val notification = userData.run {
                        NotificationModel(
                            title = "Location Request",
                            body = "$userName is requesting to track your location",
                            avatar = profilePicUrl,
                            senderId = userId,
                            receiverId = friend.userData.userId
                        )
                    }

                    val notificationSender = FcmNotification(applicationContext,token,notification!!)
                    notificationSender.sendNotifications()

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }
}