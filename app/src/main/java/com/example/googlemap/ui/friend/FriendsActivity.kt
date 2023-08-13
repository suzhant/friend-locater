package com.example.googlemap.ui.friend


import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.googlemap.R
import com.example.googlemap.adapter.FriendPagerAdapter
import com.example.googlemap.databinding.ActivityFriendsBinding
import com.example.googlemap.model.UserData
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FriendsActivity : AppCompatActivity() {

    private lateinit var searchView : SearchView
    private val binding : ActivityFriendsBinding by lazy {
        ActivityFriendsBinding.inflate(layoutInflater)
    }
    private val searchViewModel : SearchViewModel by viewModels()
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        database.getReference("users").child(auth.uid!!).addListenerForSingleValueEvent(object : ValueEventListener{
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


    }

}