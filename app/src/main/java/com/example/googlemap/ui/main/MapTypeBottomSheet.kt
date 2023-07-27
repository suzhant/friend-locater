package com.example.googlemap.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.googlemap.databinding.FragmentMapTypeBottomSheetBinding
import com.example.googlemap.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class MapTypeBottomSheet : BottomSheetDialogFragment() {

    private var _binding : FragmentMapTypeBottomSheetBinding  ?= null
    private val mapViewModel : MainActivityViewModel by activityViewModels()
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMapTypeBottomSheetBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set custom behavior to control the height and state of the bottom sheet
        val bottomSheetBehavior = BottomSheetBehavior<View>()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.peekHeight = 200 // Set the peek height (initial height when collapsed)

        // Attach the behavior to the bottom sheet's view
//       binding.root.minimumHeight = resources.displayMetrics.heightPixels

        with(binding){
            mapViewModel.mapType.observe(viewLifecycleOwner){mapType ->
                when(mapType){
                    Constants.MAP_NORMAL -> {
                        resetStrokeWidth()
                       cardDefault.strokeWidth = 5
                    }
                    Constants.MAP_SATELLITE -> {
                        resetStrokeWidth()
                        cardSatellite.strokeWidth = 5
                    }
                    Constants.MAP_HYBRID -> {
                        resetStrokeWidth()
                        cardHybrid.strokeWidth = 5
                    }
                }

            }

            linearDefault.setOnClickListener {
                mapViewModel.setMapType(Constants.MAP_NORMAL)
            }

            linearSatellite.setOnClickListener {
                mapViewModel.setMapType(Constants.MAP_SATELLITE)
            }

            linearHybrid.setOnClickListener {
                mapViewModel.setMapType(Constants.MAP_HYBRID)
            }
        }

    }

    private fun resetStrokeWidth(){
        with(binding){
            cardDefault.strokeWidth = 0
            cardHybrid.strokeWidth = 0
            cardSatellite.strokeWidth = 0
        }
    }


    companion object {
        const val TAG = "ModalBottomSheet"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}