package com.example.googlemap.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.googlemap.R
import com.example.googlemap.databinding.WidgetSearchUserBinding
import com.example.googlemap.model.UserData

class SearchAdapter(
    private val searchList : List<UserData>,
    private val onClick : (UserData) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>()  {

    private lateinit var mContext : Context

    inner class SearchViewHolder(val binding: WidgetSearchUserBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        mContext = parent.context
        val view = WidgetSearchUserBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return SearchViewHolder(view)
    }

    override fun getItemCount(): Int {
        return searchList.size
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val user = searchList[position]
        with(holder.binding){
            Glide.with(mContext).load(user.profilePicUrl).placeholder(R.drawable.img).into(imgProfile)
            txtName.text = user.userName
            txtEmail.text = user.email
        }

        holder.itemView.setOnClickListener {
            onClick(user)
        }
    }


}