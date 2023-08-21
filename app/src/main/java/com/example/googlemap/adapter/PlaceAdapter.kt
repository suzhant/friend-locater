package com.example.osm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.googlemap.databinding.WidgetPlaceBinding
import com.example.googlemap.listener.PlaceListener
import com.google.android.libraries.places.api.model.AutocompletePrediction

class PlaceAdapter(private val placeListener: PlaceListener) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    private lateinit var context: Context

    inner class PlaceViewHolder(val binding: WidgetPlaceBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        context = parent.context
        val view = WidgetPlaceBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PlaceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = differ.currentList[position]
        with(holder.binding){
            txtPrimaryAddress.text = place.getPrimaryText(null)
            txtSecondaryAddress.text = place.getSecondaryText(null)

            holder.itemView.setOnClickListener {
                placeListener.onPlaceClicked(place)
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<AutocompletePrediction>(){
        override fun areItemsTheSame(oldItem: AutocompletePrediction, newItem: AutocompletePrediction): Boolean {
            return  oldItem.placeId == newItem.placeId
        }

        override fun areContentsTheSame(oldItem: AutocompletePrediction, newItem: AutocompletePrediction): Boolean {
            return oldItem.equals(newItem)
        }

    }

    val differ = AsyncListDiffer(this,differCallback)

}