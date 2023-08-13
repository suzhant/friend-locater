package com.example.googlemap.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.googlemap.R
import com.example.googlemap.databinding.WidgetFriendRequestBinding
import com.example.googlemap.model.Friend
import kotlinx.coroutines.delay

class FriendRequestAdapter(private val onConfirm : (Friend) -> Unit,private val onDelete : (Friend) -> Unit) : RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder>() {

    private lateinit var context: Context

    inner class FriendRequestViewHolder(val binding : WidgetFriendRequestBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        context = parent.context
        val view = WidgetFriendRequestBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return FriendRequestViewHolder(view)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val friend = differ.currentList[position]
        with(holder.binding){
            friend.userData?.let {
                txtName.text = it.userName
                Glide.with(context).load(it.profilePicUrl).placeholder(R.drawable.img).into(imgProfile)
            }

            btnConfirm.setOnClickListener {
                onConfirm(friend)
            }

            btnDelete.setOnClickListener {
                onDelete(friend)
            }
        }

    }

    private val differCallback = object : DiffUtil.ItemCallback<Friend>(){
        override fun areItemsTheSame(oldItem: Friend, newItem: Friend): Boolean {
            return  oldItem.userData?.userId == newItem.userData?.userId
        }

        override fun areContentsTheSame(oldItem: Friend, newItem: Friend): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this,differCallback)

}