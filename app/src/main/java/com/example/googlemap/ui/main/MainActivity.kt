package com.example.googlemap.ui.main

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.googlemap.R
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.listener.PlaceListener
import com.example.googlemap.model.UserData
import com.example.googlemap.receiver.ConnectivityReceiver
import com.example.googlemap.services.LocationUpdateService
import com.example.googlemap.ui.LoginActivity
import com.example.googlemap.ui.SettingActivity
import com.example.googlemap.ui.friend.FriendsActivity
import com.example.googlemap.ui.friend.SearchViewModel
import com.example.googlemap.utils.Constants
import com.example.googlemap.utils.SettingPref
import com.example.osm.adapter.PlaceAdapter
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale


class MainActivity : AppCompatActivity(), PlaceListener {

    private val REQUEST_CHECK_SETTINGS: Int = 123
    private lateinit var binding: ActivityMainBinding
    private val viewModel : MainActivityViewModel by viewModels()
    private val searchViewModel : SearchViewModel by viewModels()
    private lateinit var adapter: PlaceAdapter
    private val searchDelayMillis = 400L
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private lateinit var textWatcher: TextWatcher
    private var isEditMode = true
    private var auth : FirebaseAuth ?= null
    private lateinit var database: FirebaseDatabase
    private lateinit var infoConnected : DatabaseReference
    private lateinit var eventListener: ValueEventListener
    private lateinit var placesClient : PlacesClient
    private lateinit var wifiReceiver: BroadcastReceiver
    private var isOnline = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        initializePlace()
        manageConnection()
        linkDrawerLayout()
        setHeaderView()
        initViews()
        initRecycler()
        initReceiver()

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("firebaseMessage", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            val update = mapOf<String,Any>(
                "token" to token
            )
            database.getReference("Token").child(auth?.uid!!).updateChildren(update)
        })

        database.getReference("users").child(auth?.uid!!).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val user = snapshot.getValue(UserData::class.java)
                    user?.let {
                        searchViewModel.setUserData(user)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        lifecycleScope.launch {
            val key = booleanPreferencesKey(Constants.NETWORK_STATE)
            SettingPref(applicationContext,key).getNetworkState.collectLatest { connection ->
                isOnline = connection
            }

        }

    }

    private fun initializePlace(){
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.map_api), Locale.US)
        }
        placesClient = Places.createClient(this)
    }

    private fun manageConnection() {
        val statusRef = database.reference.child("Connection").child(auth!!.uid!!)
        val status: DatabaseReference = statusRef.child("status")
        val lastOnlineRef: DatabaseReference = statusRef.child("lastOnline")
        infoConnected = database.getReference(".info/connected")
        eventListener = infoConnected.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java)!!
                if (connected) {
                    status.setValue("online")
                    lastOnlineRef.setValue(ServerValue.TIMESTAMP)
                } else {
                    status.onDisconnect().setValue("offline")
                    lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun linkDrawerLayout() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        findViewById<NavigationView>(R.id.navigation_view)
            .setupWithNavController(navController)
    }


    private fun initViews() {
        binding.searchView.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchView.editText.text.toString()
                if (query.isNotEmpty()){
                    predictLocation(query)
                }
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
                        if (query.isNotEmpty()){
                            predictLocation(query)
                        }
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

        binding.searchBar.menu.findItem(R.id.notification).actionView?.setOnClickListener {

        }

        binding.navigationView.setCheckedItem(R.id.explore)
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            // Handle menu item selected
            when(menuItem.itemId){
                R.id.sign_out -> {
                    if (isOnline){
                        database.getReference("Token").child(auth?.uid!!).removeValue()
                            .addOnSuccessListener {
                                val serviceIntent = Intent(this, LocationUpdateService::class.java)
                                stopService(serviceIntent)
                                signOut()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finishAfterTransition()
                            }

                    }else{
                        Toast.makeText(applicationContext,"You are offline",Toast.LENGTH_SHORT).show()
                    }
                }

                R.id.setting -> {
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                }

                R.id.explore -> {
                    if (binding.navigationView.checkedItem?.itemId != R.id.explore){
                        //    initFragment()
                    }
                }

                R.id.friends -> {
                    val intent = Intent(this, FriendsActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
    }

    private fun predictLocation(query : String){
        val token = AutocompleteSessionToken.newInstance()
        val request =
            FindAutocompletePredictionsRequest.builder()
                .setCountries("np")
                .setQuery(query)
                .setSessionToken(token)
                .build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                val predictions = response.autocompletePredictions
                adapter.differ.submitList(predictions)
            }
            .addOnFailureListener { exception: Exception ->
                // Handle error
                Log.d("places",exception.message.toString())
            }
    }

    private fun setHeaderView(){
        val headerView = binding.navigationView.getHeaderView(0)
        val imgProfile = headerView.findViewById<ImageView>(R.id.imgProfile)
        val txtName = headerView.findViewById<TextView>(R.id.txt_name)
        val txtEmail = headerView.findViewById<TextView>(R.id.txt_email)

        database.getReference("users").child(auth?.uid!!).addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(UserData::class.java)
                    // Now you have the object (user) fetched from the database
                    if (user != null) {
                        // You can use the user object here
                        val name = user.userName
                        val email = user.email
                        val profilePic = user.profilePicUrl
                        Glide.with(this@MainActivity).load(profilePic ?: "").placeholder(R.drawable.img).into(imgProfile)
                        loadIconWithGlide(profilePic ?: "")
                        txtName.text = name
                        txtEmail.text = email
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

    }

    private fun loadIconWithGlide(iconUrl: String) {
        Glide.with(this)
            .load(iconUrl)
            .placeholder(R.drawable.img) // Placeholder image while loading
            .circleCrop() // If you want a circular icon, use circleCrop()
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?,
                ) {
                    // Set the loaded icon to the MenuItem
                    val menuItem = binding.searchBar.menu.findItem(R.id.avatar)
                    menuItem.icon = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Optional: You can do something when the placeholder is cleared
                }
            })
    }

    private fun signOut() {
        auth?.signOut()
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS){
            if (resultCode == RESULT_OK){
               viewModel.setGps(true)
            }
        }
    }

    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerSearch.layoutManager = layoutManager
        adapter = PlaceAdapter(this)
        binding.recyclerSearch.adapter = adapter
        binding.recyclerSearch.addItemDecoration(DividerItemDecoration(this,DividerItemDecoration.VERTICAL))
    }

    override fun onPlaceClicked(place: AutocompletePrediction) {
        getPlaces(place.placeId)
        binding.searchBar.text = place.getPrimaryText(null)
        isEditMode = false
        binding.searchView.hide()
    }

    private fun getPlaces(placeId : String){
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response: FetchPlaceResponse ->
                val place = response.place
                viewModel.setDestinationData(place)
            }.addOnFailureListener { exception: Exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.d("places",statusCode.toString())
                }
            }
    }

    override fun onResume() {
        binding.navigationView.setCheckedItem(R.id.explore)
        super.onResume()
    }

    override fun onDestroy() {
        infoConnected.removeEventListener(eventListener)
        unregisterReceiver(wifiReceiver)
        super.onDestroy()
    }

    private fun initReceiver() {
        wifiReceiver = ConnectivityReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(wifiReceiver, intentFilter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }
}