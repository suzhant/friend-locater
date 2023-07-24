package com.example.googlemap.ui

import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.googlemap.R
import com.example.googlemap.databinding.ActivityLoginBinding
import com.example.googlemap.modal.UserData
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        progressDialog = buildProgressDialog()

        binding.btnLogin.setOnClickListener {

        }

        binding.txtSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.btnGoogleLogin.setOnClickListener {
            loginWithGoogle()
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
            if (result != null) {
                val data = result.data
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
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
                                                profilePicUrl = photoUrl?.toString()
                                            )

                                        }

                                       user?.let {
                                           checkIfKeyExists(user.uid,userData!!)
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

    private fun buildProgressDialog() : Dialog{
        val dialog = MaterialAlertDialogBuilder(this,R.style.ProgressDialogTheme)
        dialog.setView(R.layout.progress_bar)
        dialog.setCancelable(false)
        return dialog.create()
    }

    private fun checkIfKeyExists(key: String, userData: UserData) {
        FirebaseDatabase.getInstance().getReference(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val keyExists = dataSnapshot.hasChild(key)
                if (!keyExists){
                    database.getReference("users").child(key).setValue(userData).addOnSuccessListener {
                        if (progressDialog.isShowing){
                            progressDialog.dismiss()
                        }
                        goToNextActivity()
                    }
                }else{
                    goToNextActivity()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors that occurred while reading from the database
            }
        })
    }

    private fun goToNextActivity(){
        val intent = Intent(this,MainActivity::class.java)
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