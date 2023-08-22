package com.example.googlemap.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.example.googlemap.utils.Constants
import com.example.googlemap.utils.SettingPref
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ConnectivityReceiver : BroadcastReceiver() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var myJob: Job? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val connection =  isNetworkAvailable(context)
           myJob = coroutineScope.launch {
               val key = booleanPreferencesKey(Constants.NETWORK_STATE)
               SettingPref(context, key).setNetworkState(connection)
               cancelCoroutines()
           }
            Log.d("wifiState",connection.toString())
        }
    }

    private fun cancelCoroutines() {
        myJob?.cancel()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            //for other device how are able to connect with Ethernet
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            //for check internet over Bluetooth
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }
}
