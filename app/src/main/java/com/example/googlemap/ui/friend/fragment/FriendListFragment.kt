package com.example.googlemap.ui.friend.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.googlemap.R
import com.example.googlemap.adapter.FriendListAdapter
import com.example.googlemap.adapter.FriendRequestAdapter
import com.example.googlemap.databinding.FragmentFriendListBinding
import com.example.googlemap.model.Friend
import com.example.googlemap.model.enums.FriendStatus
import com.example.googlemap.model.enums.RequestAction
import com.google.android.material.transition.MaterialElevationScale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FriendListFragment : Fragment() {


    private val binding : FragmentFriendListBinding by lazy {
        FragmentFriendListBinding.inflate(layoutInflater)
    }
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private val friendList = mutableListOf<Friend>()
    private lateinit var friendListAdapter: FriendListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
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
                        if (user?.status == FriendStatus.ACCEPTED){
                            friendList.add(user)
                        }
                    }
                    friendListAdapter.differ.submitList(friendList.sortedByDescending { it.timestamp })
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        initRecycler()
    }

    private fun initRecycler() {
        binding.recyclerFriendList.apply {
            val manager = LinearLayoutManager(requireContext())
            layoutManager = manager
            friendListAdapter = FriendListAdapter(onClickMore = {

            })
            adapter = friendListAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(),
                    DividerItemDecoration.VERTICAL)
            )
        }
    }

}