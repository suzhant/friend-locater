package com.example.googlemap.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.TransitionManager
import com.example.googlemap.R
import com.example.googlemap.databinding.ActivitySettingBinding
import com.example.googlemap.databinding.ChangePasswordBinding
import com.example.googlemap.databinding.InputLayoutBinding
import com.example.googlemap.ui.main.MainActivityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFade
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySettingBinding
    private var auth : FirebaseAuth ?= null
    private lateinit var database: FirebaseDatabase
    private lateinit var dialog : Dialog
    private var email = ""
    private val mainViewModel : MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        mainViewModel.setLinkEnabled(isEmailLinked())


        binding.txtPassAuthentication.setOnClickListener {
            if (isEmailLinked()){
                unlinkDialog()
            }else{
                buildDialog()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.txtChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        database.getReference("users").child(auth?.uid!!).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                email = snapshot.child("email").getValue(String::class.java)!!
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        mainViewModel.linkEnabled.observe(this){enabled ->
            if (enabled){
                binding.txtPassAuthentication.text = "Unlink with Password authentication"
                binding.txtChangePassword.visibility = View.VISIBLE
            }else{
                binding.txtPassAuthentication.text = "Link with Password authentication"
                binding.txtChangePassword.visibility = View.GONE
            }
        }

    }

    private fun showChangePasswordDialog() {
        val builder = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
        val passBinding : ChangePasswordBinding = ChangePasswordBinding.inflate(LayoutInflater.from(this),binding.root,false)
        val materialFade = MaterialFade().apply {
            duration = 1000L
        }
        TransitionManager.beginDelayedTransition(binding.root, materialFade)
        builder.setTitle("Change Password")
        builder.setView(passBinding.root)
        builder.setCancelable(true)
        dialog = builder.create()
        dialog.show()


        with(passBinding){
            btnConfirm.setOnClickListener {
                val currentPassword = etPassword.text.toString()
                if (currentPassword.isEmpty()){
                    ipPassword.error = "Password cannot be empty"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }
                ipPassword.isErrorEnabled = false

                val newPassword = etNewPass.text.toString()
                val passwordValid = isPasswordValid(newPassword)
                if (!passwordValid){
                    ipNewPass.error = "Password must contain at least 8 character, one digit, one uppercase and one lowercase"
                    etNewPass.requestFocus()
                    return@setOnClickListener
                }
                ipNewPass.isErrorEnabled = false

                val confirmPass = etConfirmPass.text.toString()
                if (confirmPass != newPassword){
                    ipConfirmPass.error = "Password doesn't match with the new password"
                    etConfirmPass.requestFocus()
                    return@setOnClickListener
                }
                ipConfirmPass.isErrorEnabled = false

                changePassword(newPassword,currentPassword)

            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
        }

    }

    private fun changePassword(newPassword: String, currentPassword : String) {
        val user = auth?.currentUser
        val credential = EmailAuthProvider
            .getCredential(email, currentPassword)

        user?.reauthenticate(credential)
            ?.addOnCompleteListener {reAuthTask ->
                if (reAuthTask.isSuccessful){
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(applicationContext,"Password changed successfully",Toast.LENGTH_SHORT).show()
                                auth?.signOut()
                                val intent = Intent(this,LoginActivity::class.java)
                                startActivity(intent)
                                finishAfterTransition()
                            }else{
                                Toast.makeText(applicationContext,"Password changed failed",Toast.LENGTH_SHORT).show()
                            }
                        }
                }else{
                    Toast.makeText(applicationContext,"Password changed failed",Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun unlinkDialog(){
        val dialog = MaterialAlertDialogBuilder(this,R.style.AlertDialogTheme)
        dialog.setTitle("Unlink Account")
        dialog.setMessage("Are you sure you want to unlink password authentication?")
        dialog.setPositiveButton("yes"){ _ , _ ->
            unlinkAccount()
        }
        dialog.setNegativeButton("no"){ _ , _ ->

        }
        dialog.show()
    }

    private fun isEmailLinked(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = auth.currentUser

        if (currentUser != null) {
            val providerData = currentUser.providerData
            for (userInfo in providerData) {
                if (userInfo.providerId == EmailAuthProvider.PROVIDER_ID) {
                    // The user is linked to email authentication
                    return true
                }
            }
        }

        // The user is not linked to email authentication
        return false
    }

    private fun buildDialog(){
        val builder = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
        val inputBinding : InputLayoutBinding = InputLayoutBinding.inflate(LayoutInflater.from(this),binding.root,false)
        builder.setTitle("Link Account")
        builder.setView(inputBinding.root)

        builder.setCancelable(true)
        dialog = builder.create()
        dialog.show()

        with(inputBinding){
            btnLink.setOnClickListener {
                val password = etPassword.text.toString()
                val passwordValid = isPasswordValid(password)
                if (!passwordValid){
                    ipPassword.error = "Password must contain at least 8 character, one digit, one uppercase and one lowercase"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }

                ipPassword.isErrorEnabled = false
                linkAccount(etPassword.text.toString())
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    private fun unlinkAccount(){
        auth?.currentUser?.unlink(EmailAuthProvider.PROVIDER_ID)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Auth provider unlinked from account
                    mainViewModel.setLinkEnabled(false)
                   Toast.makeText(applicationContext,"account unlink successful", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(applicationContext,"account unlink failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun linkAccount(password : String){
        val currentUser = auth?.currentUser
        currentUser?.let {
            //if multiple provider is merged then email will return null
            val credential = EmailAuthProvider.getCredential(email, password)
            currentUser.linkWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        dialog.dismiss()
                        mainViewModel.setLinkEnabled(true)
                        Toast.makeText(applicationContext,"Account link successful",Toast.LENGTH_SHORT).show()
                    } else {
                        dialog.dismiss()
                        Toast.makeText(
                            applicationContext,
                            "Authentication failed ${task.exception?.message}.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9]).{8,}\$"
        //8 length,should contain digit,uppercase,lowercase and special char
        val passwordPattern = Regex(passwordRegex)
        return passwordPattern.matches(password)
    }
}