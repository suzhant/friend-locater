package com.example.googlemap.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.googlemap.MapViewModel
import com.example.googlemap.PlaceSearch
import com.example.googlemap.R
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.listener.PlaceListener
import com.example.googlemap.modal.LocationResult
import com.example.googlemap.services.ApiClient
import com.example.googlemap.services.LocationIQService
import com.example.googlemap.utils.Constants
import com.example.osm.adapter.PlaceAdapter
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import okhttp3.internal.wait


class MainActivity : AppCompatActivity(), PlaceListener {

    private val REQUEST_CHECK_SETTINGS: Int = 123
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
    private var auth : FirebaseAuth ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        initFragment()
        initViews()
        locationIQService = ApiClient.retrofit.create(LocationIQService::class.java)
        initRecycler()
    }

    private fun initViews() {
        placeSearch = PlaceSearch()
        binding.searchView.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchView.editText.text.toString()
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
        binding.searchView.editText.addTextChangedListener(textWatcher)

        binding.searchBar.setNavigationOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            // Handle menu item selected
            when(menuItem.itemId){
                R.id.sign_out -> {
                    menuItem.isChecked = true
                    binding.drawerLayout.close()
                    signOut()
                    val intent = Intent(this,LoginActivity::class.java)
                    startActivity(intent)
                    finishAfterTransition()
                }
            }
            true
        }
    }

    private fun signOut() {
        auth?.signOut()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS){
            if (resultCode == RESULT_OK){
               viewModel.setGps(true)
            }
        }
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
        binding.searchBar.text = place.displayName
//        binding.searchView.editText.setSelection(binding.searchView.editText.text!!.length)
//        hideKeyboard(binding.editQuery)
        isEditMode = false
        binding.searchView.hide()
    }


    private fun hideKeyboard(view : View){
        // Inside your activity or fragment
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)

    }


}