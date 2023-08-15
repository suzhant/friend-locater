package com.example.googlemap.ui.main

import android.content.Intent
import android.graphics.drawable.Drawable
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.googlemap.R
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.listener.PlaceListener
import com.example.googlemap.model.LocationResult
import com.example.googlemap.model.UserData
import com.example.googlemap.services.LocationUpdateService
import com.example.googlemap.services.LocationUpdateWorker
import com.example.googlemap.ui.LoginActivity
import com.example.googlemap.ui.SettingActivity
import com.example.googlemap.ui.friend.FriendsActivity
import com.example.osm.adapter.PlaceAdapter
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity(), PlaceListener {

    private val REQUEST_CHECK_SETTINGS: Int = 123
    private lateinit var binding: ActivityMainBinding
    private val viewModel : MainActivityViewModel by viewModels()
    private lateinit var adapter: PlaceAdapter
    private val searchDelayMillis = 400L
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private lateinit var textWatcher: TextWatcher
    private var isEditMode = true
    private var auth : FirebaseAuth ?= null
    private lateinit var database: FirebaseDatabase
    private lateinit var statusRef : DatabaseReference
    private lateinit var infoConnected : DatabaseReference
    private lateinit var eventListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        manageConnection()
        linkDrawerLayout()
        setHeaderView()
        initViews()
        initRecycler()
    //    scheduleLocationUpdates()

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

    }

    private fun manageConnection() {
        statusRef = database.reference.child("Connection").child(auth!!.uid!!)
        val status: DatabaseReference = statusRef.child("Status")
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

    private fun scheduleLocationUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a OneTimeWorkRequest to trigger the Worker
        val locationUpdateWorkRequest = OneTimeWorkRequest.Builder(LocationUpdateWorker::class.java)
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(locationUpdateWorkRequest)
    }


//    private fun initFragment() {
//        // Get the FragmentManager and start a transaction
//        val fragmentManager = supportFragmentManager
//        val fragmentTransaction = fragmentManager.beginTransaction()
//        val fragment = MapsFragment()
//
//        // Replace the contents of the fragment container with your fragment
//        fragmentTransaction.replace(binding.fragmentContainer.id, fragment)
//
//        // Commit the transaction
//        fragmentTransaction.commit()
//    }



    private fun initViews() {
        binding.searchView.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchView.editText.text.toString()
                if (query.isNotEmpty()){
                    viewModel.fetchPlaces(query)
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        viewModel.placesLiveData.observe(this){locations ->
            adapter.setPlaces(locations as MutableList<LocationResult>)
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
                            viewModel.fetchPlaces(query)
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
                    signOut()
                    val serviceIntent = Intent(this, LocationUpdateService::class.java)
                    stopService(serviceIntent)
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finishAfterTransition()
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
    }

    override fun onPlaceClicked(place: LocationResult) {
        viewModel.setDestinationData(place)
        adapter.setPlaces(mutableListOf())
        binding.searchBar.text = place.displayName
        isEditMode = false
        binding.searchView.hide()
    }

    override fun onResume() {
        binding.navigationView.setCheckedItem(R.id.explore)
        super.onResume()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }
}