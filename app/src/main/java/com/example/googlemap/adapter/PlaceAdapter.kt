package com.example.osm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.googlemap.databinding.WidgetPlaceBinding
import com.example.googlemap.listener.PlaceListener
import com.example.googlemap.modal.LocationResult

class PlaceAdapter(private val placeListener: PlaceListener) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

   // private var places = mutableListOf<PlaceSearch.Place>()
    private var places = mutableListOf<LocationResult>()
    private lateinit var context: Context

    inner class PlaceViewHolder(val binding: WidgetPlaceBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        context = parent.context
        val view = WidgetPlaceBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PlaceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return places.size
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        with(holder.binding){
            txtPlace.text = place.displayName

            txtPlace.setOnClickListener {
                placeListener.onPlaceClicked(place)
            }
        }
    }

//    fun setPlaces(placeSearch: MutableList<PlaceSearch.Place>){
//        this.places = placeSearch
//        notifyDataSetChanged()
//    }

    fun setPlaces(placeSearch: MutableList<LocationResult>){
        this.places = placeSearch
        notifyDataSetChanged()
    }

}