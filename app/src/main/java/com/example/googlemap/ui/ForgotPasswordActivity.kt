package com.example.googlemap.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.googlemap.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding : ActivityForgotPasswordBinding
    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnReset.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val emailValid = isEmailValid(email)
           if (!emailValid){
            binding.ipEmail.error = "Email is not valid"
            binding.etEmail.requestFocus()
            return@setOnClickListener
        }
            binding.ipEmail.isErrorEnabled = false
            binding.ipEmail.clearFocus()

            resetPassword(email)
        }
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        baseContext,
                        "Email sent",
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

}