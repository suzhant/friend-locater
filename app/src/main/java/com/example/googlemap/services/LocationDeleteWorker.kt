package com.example.googlemap.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.googlemap.model.UserLocation
import com.example.googlemap.model.enums.TrackStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LocationDeleteWorker(context: Context,workerParameters: WorkerParameters)
    : CoroutineWorker(context,workerParameters) {

    override suspend fun doWork(): Result {
        val ref = FirebaseDatabase.getInstance().getReference("location").child(FirebaseAuth.getInstance().uid!!)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    for (dataSnap in snapshot.children){
                        val user = dataSnap.getValue(UserLocation::class.java)
                        user?.run {
                            if (status == TrackStatus.PENDING.name){
                                ref.removeValue()
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        return Result.success()
    }
}