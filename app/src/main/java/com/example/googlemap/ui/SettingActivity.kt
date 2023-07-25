package com.example.googlemap.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.example.googlemap.R
import com.example.googlemap.databinding.ActivitySettingBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class SettingActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySettingBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var dialog : Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        binding.txtPassAuthentication.setOnClickListener {
            buildDialog()
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun buildDialog(){
        val builder = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
        val viewInflate = LayoutInflater.from(this).inflate(
            R.layout.input_layout,
            findViewById(android.R.id.content),
            false
        )
        builder.setTitle("Link Account")
        builder.setView(viewInflate)
        val ipPassword = viewInflate.findViewById<TextInputLayout>(R.id.ip_password)
        val etPassword = viewInflate.findViewById<TextInputEditText>(R.id.et_password)
        val btnCancel = viewInflate.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnLink = viewInflate.findViewById<MaterialButton>(R.id.btn_link)
        builder.setCancelable(true)
        dialog = builder.create()
        dialog.show()

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


    private fun linkAccount(password : String){
        val currentUser = auth.currentUser
        val email = currentUser?.email
        val credential = EmailAuthProvider.getCredential(email.toString(), password)
        currentUser!!.linkWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    dialog.dismiss()
                    Toast.makeText(applicationContext,"Account link successful",Toast.LENGTH_SHORT).show()
                } else {
                    dialog.dismiss()
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
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