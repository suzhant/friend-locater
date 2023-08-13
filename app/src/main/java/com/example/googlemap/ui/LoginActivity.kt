package com.example.googlemap.ui

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.googlemap.R
import com.example.googlemap.databinding.ActivityLoginBinding
import com.example.googlemap.model.UserData
import com.example.googlemap.ui.main.MainActivity
import com.example.googlemap.utils.ProgressHelper
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private val TAG = "google_sign"
    private lateinit var binding: ActivityLoginBinding
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private var showOneTapUI = true
    private var auth: FirebaseAuth? = null
    private lateinit var database : FirebaseDatabase
    private lateinit var progressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        progressDialog = ProgressHelper.buildProgressDialog(this)

        binding.btnLogin.setOnClickListener {

        }

        binding.txtSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.btnGoogleLogin.setOnClickListener {
            googleSignIn()
        }

        binding.btnLogin.setOnClickListener {
            loginWithPassword()
        }

        binding.btnForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginWithPassword() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        if (email.isEmpty()){
           binding.ipEmail.error = "Email can't be empty"
           binding.etEmail.requestFocus()
            return
        }
        binding.ipEmail.isErrorEnabled = false

        if (password.isEmpty()){
            binding.ipPassword.error = "Email can't be empty"
            binding.etPassword.requestFocus()
            return
        }
        binding.ipPassword.isErrorEnabled = false


        progressDialog.show()
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth?.currentUser
                    if (user!=null){
                        progressDialog.dismiss()
                        goToNextActivity()
                    }

                } else {
                    progressDialog.dismiss()
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        task.exception?.message,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun googleSignIn() {
        val request = GetSignInIntentRequest.builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()
        Identity.getSignInClient(this)
            .getSignInIntent(request)
            .addOnSuccessListener { result: PendingIntent ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.intentSender).build()
                    googleIntentResultLauncher.launch(intentSenderRequest)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Google Sign-in failed")
                }
            }
            .addOnFailureListener { e: Exception? ->
                Log.e(
                    TAG,
                    "Google Sign-in failed",
                    e
                )
            }
    }

    private fun loginWithGoogle() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        signInWithIntent()
    }


    private fun signInWithIntent(){
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                    googleIntentResultLauncher.launch(intentSenderRequest)
                } catch (e: IntentSender.SendIntentException) {
                    Log.d(TAG,"failed : ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(TAG,"failed : ${e.localizedMessage}")
            }
    }

    private val googleIntentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data
                try {
                    val credential = Identity.getSignInClient(this)
                        .getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val password = credential.password

                    when {
                        idToken != null -> {
                            // Got an ID token from Google. Use it to authenticate
                            // with your backend.
                            Log.d(TAG, "Got ID token.")
                            if (!progressDialog.isShowing){
                                progressDialog.show()
                            }
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            auth?.signInWithCredential(firebaseCredential)
                                ?.addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d(TAG, "signInWithCredential:success")

                                        val user = auth?.currentUser
                                        val userData =  user?.run {
                                            UserData(
                                                userId = uid,
                                                userName = displayName,
                                                profilePicUrl = photoUrl?.toString(),
                                                email = email.toString()
                                            )
                                        }

                                        if (userData != null) {
                                            checkIfKeyExists(key = auth?.uid!!, userData = userData)
                                        }

                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                                    }
                                }
                        }

                        password != null -> {
                            // Got a saved username and password. Use them to authenticate
                            // with your backend.
                            Log.d(TAG, "Got password.")
                        }

                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token or password!")
                        }
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            Log.d(TAG, "One-tap dialog was closed.")
                            // Don't re-prompt the user.
                            showOneTapUI = false
                        }

                        CommonStatusCodes.NETWORK_ERROR -> {
                            Log.d(TAG, "One-tap encountered a network error.")
                            // Try again or just ignore.
                        }

                        else -> {
                            Log.d(
                                TAG, "Couldn't get credential from result." +
                                        " (${e.localizedMessage})"
                            )
                        }
                    }
                }
            }
        }


    private fun checkIfKeyExists(key: String, userData: UserData) {
        database.getReference("users").child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()){
                    database.getReference("users").child(key).setValue(userData).addOnSuccessListener {
                        if (progressDialog.isShowing){
                            progressDialog.dismiss()
                        }
                        goToNextActivity()
                    }
                }else{
                    val pic = auth?.currentUser?.photoUrl.toString()
                    val updates = hashMapOf<String, Any>(
                        "profilePicUrl" to pic
                    )
                    database.getReference("users").child(key).updateChildren(updates)
                        .addOnSuccessListener {
                        // Update was successful
                            if (progressDialog.isShowing){
                                progressDialog.dismiss()
                            }
                            goToNextActivity()
                    }.addOnFailureListener {
                            // Handle any errors that occurred during the update process

                        }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors that occurred while reading from the database
            }
        })
    }

    private fun goToNextActivity(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finishAfterTransition()
    }

    override fun onStart() {
        super.onStart()
        if (auth?.currentUser!=null){
            goToNextActivity()
        }
    }
}