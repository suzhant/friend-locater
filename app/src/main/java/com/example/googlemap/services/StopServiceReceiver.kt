package com.example.googlemap.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class StopServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stop the foreground service when the action button is clicked
        val serviceIntent = Intent(context, LocationUpdateService::class.java)
        context.stopService(serviceIntent)
    }
}
