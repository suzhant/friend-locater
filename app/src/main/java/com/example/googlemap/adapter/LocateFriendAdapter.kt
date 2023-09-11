package com.example.googlemap.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.googlemap.R
import com.example.googlemap.databinding.WidgetTrackFriendBinding
import com.example.googlemap.model.Friend
import com.example.googlemap.model.enums.Presence
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LocateFriendAdapter(val onClick : (Friend) -> Unit) : RecyclerView.Adapter<LocateFriendAdapter.LocateViewHolder>() {

    private var friendList = mutableListOf<Friend>()
    private lateinit var context: Context
    private val database = FirebaseDatabase.getInstance()

    inner class LocateViewHolder(val binding : WidgetTrackFriendBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocateViewHolder {
        context = parent.context
        val view = WidgetTrackFriendBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return LocateViewHolder(view)
    }

    override fun getItemCount(): Int {
       return friendList.size
    }

    override fun onBindViewHolder(holder: LocateViewHolder, position: Int) {
        val friend = friendList[position]
        with(holder.binding){
            txtEmail.text = friend.userData?.email
            txtName.text = friend.userData?.userName
            Glide.with(context).load(friend.userData?.profilePicUrl).placeholder(R.drawable.img).into(imgProfile)
            btnLocate.setOnClickListener {
                onClick(friend)
            }


            friend.userData?.userId?.let {
                database.getReference("Connection").child(it).addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()){
                            val status = snapshot.child("status").getValue(String::class.java)
                            status?.let {
                                if (status == "online"){
                                    imgProfile.strokeWidth = 10f
                                }else{
                                    imgProfile.strokeWidth = 0f
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
            }

        }
    }

    fun setData(list : MutableList<Friend>){
        friendList = list
        notifyDataSetChanged()
    }
}