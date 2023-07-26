package com.example.googlemap.ui.main

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.googlemap.R
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.listener.PlaceListener
import com.example.googlemap.modal.LocationResult
import com.example.googlemap.modal.UserData
import com.example.googlemap.ui.LoginActivity
import com.example.googlemap.ui.SettingActivity
import com.example.googlemap.ui.main.fragments.MapsFragment
import com.example.googlemap.utils.PlaceSearch
import com.example.osm.adapter.PlaceAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        initFragment()
        setHeaderView()
        initViews()
        initRecycler()
    }

    private fun initViews() {
        binding.searchView.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchView.editText.text.toString()
                //  performPlaceSearch(query)
                if (query.isNotEmpty()){
                    viewModel.fetchPlaces(query)
                }
             //   searchLocation(query)
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
                        //  performPlaceSearch(query)
                        if (query.isNotEmpty()){
                            viewModel.fetchPlaces(query)
                        }
                     //   searchLocation(query)
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

        binding.navigationView.setCheckedItem(R.id.search)
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            // Handle menu item selected
            when(menuItem.itemId){
                R.id.sign_out -> {
                    binding.drawerLayout.close()
                    signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finishAfterTransition()
                }

                R.id.setting -> {
                    binding.drawerLayout.close()
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                }

                R.id.search -> {
                    if (binding.navigationView.checkedItem?.itemId != R.id.search){
                        initFragment()
                    }
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
                        Glide.with(this@MainActivity).load(profilePic).placeholder(R.drawable.img).into(imgProfile)
                        loadIconWithGlide(profilePic!!)
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
                    transition: Transition<in Drawable>?
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
        viewModel.setDestinationData(place)
        adapter.setPlaces(mutableListOf())
        binding.searchBar.text = place.displayName
        isEditMode = false
        binding.searchView.hide()
    }


    private fun hideKeyboard(view : View){
        // Inside your activity or fragment
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)

    }

    override fun onResume() {
        binding.navigationView.setCheckedItem(R.id.search)
        super.onResume()
    }


}