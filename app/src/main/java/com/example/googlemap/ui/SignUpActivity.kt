package com.example.googlemap.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.googlemap.databinding.ActivitySignUpBinding
import com.example.googlemap.model.UserData
import com.example.googlemap.ui.main.MainActivity
import com.example.googlemap.utils.ProgressHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySignUpBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var progressDialog : Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        progressDialog = ProgressHelper.buildProgressDialog(this)

        binding.txtLogin.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSignUp.setOnClickListener {
            signUp()
        }
    }

    private fun signUp() {
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val emailValid = isEmailValid(email)
        val passwordValid = isPasswordValid(password)

        if (name.isEmpty()){
            binding.ipName.error = "Name cannot be empty"
            binding.etName.requestFocus()
            return
        }
        binding.ipName.isErrorEnabled = false

        if (!emailValid){
            binding.ipEmail.error = "Email is not valid"
            binding.etEmail.requestFocus()
            return
        }
        binding.ipEmail.isErrorEnabled = false

        if (!passwordValid){
            binding.ipPassword.error = "Password must contain at least 8 character, one digit, one uppercase and one lowercase"
            binding.etPassword.requestFocus()
            return
        }
        binding.ipPassword.isErrorEnabled = false

        createAccountWithPassword(email,password,name)
    }

    private fun createAccountWithPassword(email: String, password: String,name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    val userData =  user?.run {
                        UserData(
                            userId = uid,
                            userName = name,
                            profilePicUrl = null,
                            email = email
                        )
                    }
                    user?.let {
                        database.getReference("users").child(user.uid).setValue(userData).addOnSuccessListener {
                            if (progressDialog.isShowing){
                                progressDialog.dismiss()
                            }
                            Toast.makeText(
                                baseContext,
                                "Account created successfully",
                                Toast.LENGTH_SHORT,
                            ).show()
                            auth.signOut()
                        }
                    }
                } else {
                    // If sign in fails, display a message to the user.

                    Toast.makeText(
                        baseContext,
                        task.exception?.message.toString(),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun isEmailValid(email: String): Boolean {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
        val emailPattern = Regex(emailRegex)
        return emailPattern.matches(email)
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9]).{8,}\$"
        //8 length,should contain digit,uppercase,lowercase and special char
        val passwordPattern = Regex(passwordRegex)
        return passwordPattern.matches(password)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            auth.signOut()
        }
    }

}