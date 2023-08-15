package com.example.googlemap.ui.main.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.googlemap.adapter.LocateFriendAdapter
import com.example.googlemap.databinding.FragmentBottomSheetFriendBinding
import com.example.googlemap.model.Friend
import com.example.googlemap.model.NotificationModel
import com.example.googlemap.utils.FcmNotification
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FriendBottomSheet : BottomSheetDialogFragment()  {


    private val binding : FragmentBottomSheetFriendBinding by lazy {
        FragmentBottomSheetFriendBinding.inflate(layoutInflater)
    }
    private val friendList = mutableListOf<Friend>()
    private lateinit var friendAdapter: LocateFriendAdapter
    private lateinit var auth : FirebaseAuth
    private lateinit var database: FirebaseDatabase

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
                            friendList.add(user)
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
                sendNotification(friend)
            })
            adapter = friendAdapter
            addItemDecoration(DividerItemDecoration(requireContext(),DividerItemDecoration.VERTICAL))
        }
    }

    private fun sendNotification(friend: Friend) {
        friend.userData?.run {
            database.getReference("Token").child(userId).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val token = snapshot.child("token").getValue(String::class.java)
                        val notification = NotificationModel(
                            title = userName,
                            body = "hi $userName",
                            avatar = profilePicUrl,
                            senderId = auth.uid,
                            receiverId = userId,
                            msgType = "text"
                        )
                        val notificationSender = FcmNotification(requireContext(),token,notification)
                        notificationSender.sendNotifications()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }

}