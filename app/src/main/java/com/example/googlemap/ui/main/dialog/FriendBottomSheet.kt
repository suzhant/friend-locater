package com.example.googlemap.ui.main.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.googlemap.adapter.LocateFriendAdapter
import com.example.googlemap.databinding.FragmentBottomSheetFriendBinding
import com.example.googlemap.model.Friend
import com.example.googlemap.model.NotificationModel
import com.example.googlemap.model.UserLocation
import com.example.googlemap.model.enums.FriendStatus
import com.example.googlemap.model.enums.Presence
import com.example.googlemap.model.enums.TrackStatus
import com.example.googlemap.services.LocationDeleteWorker
import com.example.googlemap.ui.friend.SearchViewModel
import com.example.googlemap.utils.FcmNotification
import com.example.googlemap.utils.ProgressHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date
import java.util.concurrent.TimeUnit

class FriendBottomSheet : BottomSheetDialogFragment()  {


    private val binding : FragmentBottomSheetFriendBinding by lazy {
        FragmentBottomSheetFriendBinding.inflate(layoutInflater)
    }
    private val searchViewModel : SearchViewModel by activityViewModels()
    private val friendList = mutableListOf<Friend>()
    private lateinit var friendAdapter: LocateFriendAdapter
    private lateinit var auth : FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var dialog : Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        val bottomSheetBehavior = BottomSheetBehavior<View>()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        binding.root.minimumHeight = resources.displayMetrics.heightPixels
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        database.getReference("friends").child(auth.uid!!).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    friendList.clear()
                    for (dataSnap in snapshot.children){
                        val user = dataSnap.getValue(Friend::class.java)
                        user?.let {
                            if (user.status == FriendStatus.ACCEPTED){
                                friendList.add(user)
                            }
                        }
                    }
                    friendAdapter.setData(friendList)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })



        initRecycler()
    }

    private fun initRecycler() {
        binding.recyclerTrackFriend.apply {
            layoutManager = LinearLayoutManager(requireContext())
            friendAdapter = LocateFriendAdapter(onClick = {friend ->
                dialog = ProgressHelper.buildProgressDialog(requireContext())
                dialog.show()
                locateUser(friend)
            })
            adapter = friendAdapter
            addItemDecoration(DividerItemDecoration(requireContext(),DividerItemDecoration.VERTICAL))
        }
    }

    private fun locateUser(friend: Friend) {
        val senderId = auth.uid
        val receiverId = friend.userData?.userId
        val receiverObj =  UserLocation(
           longitude = null,
           latitude = null,
           status = TrackStatus.PENDING.name,
           timestamp = Date().time,
           userData = friend.userData
        )
        val locationRef = database.getReference("location")
        locationRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val data = snapshot.value as? Map<*, *>
                    if (data?.containsKey(senderId) == true) {
                        dialog.dismiss()
                        Toast.makeText(requireContext(),"You can track only one user at a time",Toast.LENGTH_SHORT).show()
                    } else {
                        for (dataSnap in snapshot.children){
                            for (child in dataSnap.children){
                                val user = child.getValue(UserLocation::class.java)
                                user?.let {
                                    if (user.userData?.userId == receiverId){
                                        dialog.dismiss()
                                        Toast.makeText(requireContext(),"The user is busy. Try again sometime",Toast.LENGTH_SHORT).show()
                                    }else{
                                        sendNotification(friend)
                                        locationRef.child(senderId!!).child(receiverId!!).setValue(receiverObj)
                                    }
                                }
                            }
                        }
                    }
                }else{
                    sendNotification(friend)
                    locationRef.child(senderId!!).child(receiverId!!).setValue(receiverObj)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

    }

    private fun sendNotification(friend: Friend){
        val userData = searchViewModel.userData.value
        database.getReference("Token").child(friend.userData?.userId!!).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    dialog.dismiss()
                    val token = snapshot.child("token").getValue(String::class.java)
                    val notification = userData?.run {
                        NotificationModel(
                            title = "Location Request",
                            body = "$userName is requesting to track your location",
                            avatar = profilePicUrl,
                            senderId = userId,
                            receiverId = friend.userData.userId
                        )
                    }
                    context?.let {
                        val notificationSender = FcmNotification(it,token,notification!!)
                        notificationSender.sendNotifications()
                        // Start a timer to delete the node after 60 seconds
                        scheduleLocationDelete()
                    }
                }else{
                    dialog.dismiss()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun scheduleLocationDelete() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a OneTimeWorkRequest to trigger the Worker
        val locationUpdateWorkRequest = OneTimeWorkRequest.Builder(LocationDeleteWorker::class.java)
            .setConstraints(constraints)
            .setInitialDelay(62L,TimeUnit.SECONDS)
            .build()

        val workManager = WorkManager.getInstance(requireContext())
        workManager.enqueueUniqueWork(locationUpdateWorkRequest.stringId,ExistingWorkPolicy.APPEND,locationUpdateWorkRequest)
    }
}