package com.example.googlemap.ui.friend.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.googlemap.R
import com.example.googlemap.databinding.FragmentSearchBottomSheetDialogBinding
import com.example.googlemap.model.Friend
import com.example.googlemap.model.UserData
import com.example.googlemap.model.enums.FriendStatus
import com.example.googlemap.ui.friend.SearchViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

class SearchBottomSheetDialog : BottomSheetDialogFragment() {

    private val binding : FragmentSearchBottomSheetDialogBinding by lazy {
        FragmentSearchBottomSheetDialogBinding.inflate(layoutInflater)
    }
    private val sharedViewModel : SearchViewModel by activityViewModels()
    private lateinit var database: FirebaseDatabase
    private lateinit var auth : FirebaseAuth
    private val friendArgs : SearchBottomSheetDialogArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheetBehavior = BottomSheetBehavior<View>()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        bottomSheetBehavior.peekHeight = 200 // Set the peek height (initial height when collapsed)

        // Attach the behavior to the bottom sheet's view
       binding.root.minimumHeight = resources.displayMetrics.heightPixels

        val userData = friendArgs.userData
        with(binding){
            Glide.with(requireActivity()).load(userData.profilePicUrl).placeholder(R.drawable.img).into(imgProfile)
            txtName.text = userData.userName
            txtEmail.text = userData.email
        }

        database.getReference("friends").child(auth.uid!!).child(userData.userId).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                hideProgress(true)
                if (snapshot.exists()){
                    val user = snapshot.getValue(Friend::class.java)
                    when(user?.status){
                        FriendStatus.ACCEPTED -> setButtonText(UNFRIEND)
                        FriendStatus.PENDING -> setButtonText(CANCEL)
                        FriendStatus.REJECTED -> setButtonText(ADD_FRIEND)
                        else -> setButtonText(ADD_FRIEND)
                    }
                }else{
                    setButtonText(ADD_FRIEND)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        binding.btnAction.setOnClickListener {
            hideProgress(false)
            when(binding.btnAction.text){
                UNFRIEND -> {
                    cancelRequest()
                }
                CANCEL -> {
                    cancelRequest()
                }
                ADD_FRIEND -> {
                    sendFriendRequest()
                }
            }
        }
    }

    private fun hideProgress(hide : Boolean){
        if (hide){
            binding.progressBar.visibility = View.GONE
            binding.btnAction.visibility = View.VISIBLE
        }else{
            binding.progressBar.visibility = View.VISIBLE
            binding.btnAction.visibility = View.GONE
        }
    }

    private fun cancelRequest(){
        val friendId = friendArgs.userData.userId
        auth.uid?.let {id ->
            database.getReference("friends").child(id).child(friendId).removeValue()
                .addOnSuccessListener {
                    database.getReference("friends").child(friendId).child(id).removeValue()
                        .addOnSuccessListener {
                            hideProgress(true)
                            Toast.makeText(requireContext(),"Friend Request cancelled successfully",Toast.LENGTH_SHORT).show()
                            setButtonText(ADD_FRIEND)
                        }.addOnFailureListener {
                            hideProgress(true)
                            Toast.makeText(requireContext(),"failed",Toast.LENGTH_SHORT).show()
                        }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(),"failed",Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun sendFriendRequest() {
        val friendId = friendArgs.userData.userId
        val time = Date().time
          val receiver = Friend(
              userData = friendArgs.userData,
              status = FriendStatus.PENDING,
              timestamp = time
          )

        val sender = sharedViewModel.userData.value?.let {userData ->
            Friend(
                userData = userData,
                status = FriendStatus.INCOMING,
                timestamp = time
            )
        }

        auth.uid?.let {id ->
            database.getReference("friends").child(id).child(friendId).setValue(receiver)
                .addOnSuccessListener {
                    database.getReference("friends").child(friendId).child(id).setValue(sender)
                        .addOnSuccessListener {
                            hideProgress(true)
                            Toast.makeText(requireContext(),"Friend Request sent successfully",Toast.LENGTH_SHORT).show()
                            setButtonText(CANCEL)
                        }.addOnFailureListener {
                            hideProgress(true)
                            Toast.makeText(requireContext(),"failed",Toast.LENGTH_SHORT).show()
                        }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(),"failed",Toast.LENGTH_SHORT).show()
                }
        }

    }

    private fun setButtonText(text : String){
        binding.btnAction.text = text
    }

    companion object {
        const val TAG = "SearchBottomSheet"
        const val UNFRIEND = "Unfriend"
        const val CANCEL = "Cancel"
        const val ADD_FRIEND = "Add Friend"
    }

}