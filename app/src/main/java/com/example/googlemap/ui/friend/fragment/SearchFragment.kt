package com.example.googlemap.ui.friend.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.googlemap.R
import com.example.googlemap.adapter.SearchAdapter
import com.example.googlemap.databinding.FragmentSearchBinding
import com.example.googlemap.model.UserData
import com.example.googlemap.ui.friend.SearchViewModel
import com.example.googlemap.ui.friend.dialog.SearchBottomSheetDialog
import com.example.googlemap.ui.friend.dialog.SearchBottomSheetDialogArgs
import com.example.googlemap.utils.KeyboardUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SearchFragment : Fragment() {

    private val binding : FragmentSearchBinding by lazy {
        FragmentSearchBinding.inflate(layoutInflater)
    }
    private val sharedViewModel : SearchViewModel by activityViewModels()

    private lateinit var adapter: SearchAdapter
    private var searchList = mutableListOf<UserData>()
    private var tempList = mutableListOf<UserData>()
    private lateinit var auth : FirebaseAuth
    private lateinit var database : FirebaseDatabase
    private lateinit var textWatcher: TextWatcher
    private val searchDelayMillis = 400L
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var isEditMode = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        showKeyBoard()
        initRecycler()
        searchPeople()
    }

    private fun searchPeople() {
        textWatcher = object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!isEditMode){
                    isEditMode = true
                    return
                }

                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s.toString().trim()
                searchRunnable = Runnable {
                    if (isEditMode){
                        if (query.isNotEmpty()){
                            performSearch(query.lowercase())
                        }else{
                            populateData()
                        }
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, searchDelayMillis)
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        }
        binding.edSearch.addTextChangedListener(textWatcher)
    }

    private fun performSearch(searchQuery: String) {
        database.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                searchList.clear()
                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(UserData::class.java)
                    user?.let {
                        if (it.userName?.lowercase()?.contains(searchQuery) == true || it.email.lowercase().contains(searchQuery)){
                            if (it.userId!=auth.uid){
                                searchList.add(it)
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })

    }

    private fun showKeyBoard() {
        binding.edSearch.requestFocus()
        KeyboardUtils.showSoftKeyboard(binding.edSearch,requireContext())
    }

    private fun populateData() {
        database.getReference("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    searchList.clear()
                    for (datasnap in snapshot.children){
                        val user = datasnap.getValue(UserData::class.java)
                        user?.let {
                            if (it.userId!=auth.uid){
                                searchList.add(it)
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun initRecycler(){
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSearch.layoutManager = layoutManager
        adapter = SearchAdapter(searchList, onClick = {userData ->
            KeyboardUtils.hideSoftKeyboard(binding.edSearch,requireContext())
            binding.edSearch.clearFocus()
          //  sharedViewModel.setUserData(userData = userData)
            val action = SearchFragmentDirections.actionSearchFragmentToSearchBottomSheetDialog(userData)
            with(findNavController()){
                currentDestination?.getAction(R.id.action_searchFragment_to_searchBottomSheetDialog)?.
                let {
                    navigate(action)
                }
            }
        })
        binding.recyclerSearch.adapter = adapter
    }
}