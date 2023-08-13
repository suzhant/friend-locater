package com.example.googlemap.ui.friend.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.example.googlemap.R
import com.example.googlemap.adapter.FriendRequestAdapter
import com.example.googlemap.databinding.FragmentFriendRequestBinding
import com.example.googlemap.model.Friend
import com.example.googlemap.model.enums.FriendStatus
import com.example.googlemap.model.enums.RequestAction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FriendRequestFragment : Fragment() {


    private val binding : FragmentFriendRequestBinding by lazy {
        FragmentFriendRequestBinding.inflate(layoutInflater)
    }
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private lateinit var requestAdapter: FriendRequestAdapter
    private var requestList = mutableListOf<Friend>()

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

        database.getReference("friends").child(auth.uid!!).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    requestList.clear()
                    for (dataSnap in snapshot.children){
                        val requests = dataSnap.getValue(Friend::class.java)
                        if (requests?.status == FriendStatus.INCOMING){
                            requestList.add(requests)
                        }
                    }
                    if (requestList.isEmpty()){
                        binding.txtMesssage.visibility = View.VISIBLE
                    }else{
                        binding.txtMesssage.visibility = View.GONE
                    }
                    requestAdapter.differ.submitList(requestList.sortedByDescending { it.timestamp})
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        initRecycler()
    }

    private fun initRecycler() {
        binding.recyclerFriendRequest.apply {
            val manager = LinearLayoutManager(requireContext())
            layoutManager = manager
            requestAdapter = FriendRequestAdapter(onConfirm =
            {
                processRequest(it,RequestAction.ACCEPT)
            }, onDelete = {
                processRequest(it,RequestAction.REJECT)
            })
            adapter = requestAdapter
            addItemDecoration(DividerItemDecoration(requireContext(),DividerItemDecoration.VERTICAL))
        }
    }

    private fun processRequest(friend: Friend, action: RequestAction) {
        when(action){
            RequestAction.ACCEPT -> {
                val updates = hashMapOf<String, Any>(
                    "status" to FriendStatus.ACCEPTED.name
                )
                friend.userData?.run {
                    database.getReference("friends").child(auth.uid!!).child(userId).updateChildren(updates)
                        .addOnSuccessListener {
                            database.getReference("friends").child(userId).child(auth.uid!!).updateChildren(updates)
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(),"$userName has been accepted",Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener {
                                    Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                                }
                        }.addOnFailureListener {
                            Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        }
                }
            }

            RequestAction.REJECT -> {
                friend.userData?.run{
                    database.getReference("friends").child(auth.uid!!).child(userId).removeValue()
                        .addOnSuccessListener {
                            database.getReference("friends").child(userId).child(auth.uid!!).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(),"$userName has been rejected",Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener {
                                    Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                                }
                        }.addOnFailureListener {
                            Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

}