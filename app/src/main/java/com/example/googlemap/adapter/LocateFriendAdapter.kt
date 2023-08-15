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

class LocateFriendAdapter(val onClick : (Friend) -> Unit) : RecyclerView.Adapter<LocateFriendAdapter.LocateViewHolder>() {

    private var friendList = mutableListOf<Friend>()
    private lateinit var context: Context

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
        }
    }

    fun setData(list : MutableList<Friend>){
        friendList = list
        notifyDataSetChanged()
    }
}