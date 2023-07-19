package com.example.googlemap

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.listener.PlaceListener
import com.example.googlemap.modal.LocationResult
import com.example.googlemap.services.ApiClient
import com.example.googlemap.services.LocationIQService
import com.example.googlemap.utils.Constants
import com.example.osm.adapter.PlaceAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainActivity : AppCompatActivity(), PlaceListener {


    private lateinit var binding: ActivityMainBinding
    private val viewModel : MapViewModel by viewModels()
    private lateinit var placeSearch: PlaceSearch
    private lateinit var adapter: PlaceAdapter
    private val searchDelayMillis = 400L
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private lateinit var textWatcher: TextWatcher
    private var isEditMode = true
    private lateinit var locationIQService : LocationIQService
    private var currentLat = 0.0
    private var currentLong = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermission()
        checkGps()
        initFragment()
        initViews()
        locationIQService = ApiClient.retrofit.create(LocationIQService::class.java)
        initRecycler()
    }

    private fun initViews() {
        placeSearch = PlaceSearch()
        binding.editQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editQuery.text.toString()
                //  performPlaceSearch(query)
                searchLocation(query)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }


        textWatcher = object : TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (!isEditMode){
                    isEditMode = true
                    return
                }

                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s.toString().trim()
                searchRunnable = Runnable {
                    if (isEditMode){
                        //  performPlaceSearch(query)
                        searchLocation(query)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, searchDelayMillis)

            }

            override fun afterTextChanged(s: Editable?) {

            }

        }
        binding.editQuery.addTextChangedListener(textWatcher)

    }



    private fun initFragment() {
        // Get the FragmentManager and start a transaction
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val fragment = MapsFragment()

        // Replace the contents of the fragment container with your fragment
        fragmentTransaction.replace(binding.fragmentContainer.id, fragment)

        // Commit the transaction
        fragmentTransaction.commit()
    }

    private fun checkGps() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGPSEnabled) {
            // GPS is disabled
            buildAlertMessageNoGps()
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
        builder.setTitle(getString(R.string.gps_title))
        builder.setMessage(getString(R.string.gps_message))
        builder.setPositiveButton(R.string.settings) { dialog, which ->
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
        builder.setNegativeButton(R.string.cancel) { _, _ ->

        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun checkPermission() {
        val permissions = arrayOf(
            ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
        )
        val finePermission = ContextCompat.checkSelfPermission(
            this,
            permissions[0]
        ) == PackageManager.PERMISSION_GRANTED
        val coarsePermission = ContextCompat.checkSelfPermission(
            this,
            permissions[1]
        ) == PackageManager.PERMISSION_GRANTED

        if (!finePermission || !coarsePermission) {
            requestPermissionLauncher.launch(
                arrayOf(
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION
                )
            )
        }else{
            viewModel.setLocationPermissionGranted(true)
        }
    }

    private val requestPermissionLauncher= registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var locationGranted = false
        permissions.entries.forEach {
            val isGranted = it.value
            val permissionName = it.key
            if (permissionName == ACCESS_FINE_LOCATION || permissionName == ACCESS_COARSE_LOCATION) {
                locationGranted = isGranted
                viewModel.setLocationPermissionGranted(true)
            }
        }
        if (!locationGranted) {
            showDialog()
        }
    }

    private fun showDialog() {
        var message = ""
        message = getString(R.string.location_permission_message)

        val builder = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
        builder.setTitle(R.string.permission_required)
        builder.setMessage(message)
        builder.setPositiveButton(R.string.settings) { dialog, which ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
        builder.setNegativeButton(R.string.cancel) { _, _ ->

        }
        builder.setCancelable(false)
        builder.show()
    }


    private fun searchLocation(query: String) {
        val apiKey = Constants.API_KEY
        val call = locationIQService.searchLocation(apiKey, query)
        call.enqueue(object : retrofit2.Callback<List<LocationResult>> {
            override fun onResponse(
                call: retrofit2.Call<List<LocationResult>>,
                response: retrofit2.Response<List<LocationResult>>,
            ) {
                if (response.isSuccessful) {
                    val locations = response.body()
                    adapter.setPlaces(locations as MutableList<LocationResult>)

                    // Handle the list of locations returned
                } else {
                    // Handle error response
                }
            }

            override fun onFailure(call: retrofit2.Call<List<LocationResult>>, t: Throwable) {
                // Handle failure
            }
        })
    }

//    private fun performPlaceSearch(query: String) {
//        // Perform the search
//        placeSearch.searchPlace(query, object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                e.printStackTrace()
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                if (response.isSuccessful) {
//                    val responseBody = response.body?.string()
//                    val places = placeSearch.gson.fromJson<MutableList<PlaceSearch.Place>>(
//                        responseBody,
//                        object : TypeToken<List<PlaceSearch.Place>>() {}.type
//                    )
//
//
//                    CoroutineScope(Dispatchers.Main).launch {
//                        adapter.setPlaces(places)
//                    }
//
//                } else {
//                    // Handle the failure case
//                }
//            }
//        })
//
//    }

    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerSearch.layoutManager = layoutManager
        adapter = PlaceAdapter(this)
        binding.recyclerSearch.adapter = adapter
    }

    override fun onPlaceClicked(place: LocationResult) {
        viewModel.setData(place)
        adapter.setPlaces(mutableListOf())
        binding.editQuery.setText(place.displayName)
        binding.editQuery.setSelection(binding.editQuery.text!!.length)
        hideKeyboard(binding.editQuery)
        isEditMode = false
    }


    private fun hideKeyboard(view : View){
        // Inside your activity or fragment
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)

    }


}