package com.example.googlemap.utils

import android.content.Context
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.googlemap.R
import com.example.googlemap.model.NotificationModel
import com.google.auth.oauth2.GoogleCredentials
import org.json.JSONException
import org.json.JSONObject


class FcmNotification(
    private val context: Context,
    private var userFcmToken: String?,
    private val notificationModel: NotificationModel,
) {

    private val postUrl = "https://fcm.googleapis.com/v1/projects/map-e5664/messages:send"
    private val SCOPES = listOf("https://www.googleapis.com/auth/firebase.messaging")

    private fun getAccessToken(): String? {
        val inputStream = context.resources.openRawResource(R.raw.serviceaccount)
        val googleCredentials = GoogleCredentials
            .fromStream(inputStream)
            .createScoped(SCOPES)
        return googleCredentials.refreshAccessToken().tokenValue
    }

    fun sendNotifications() {
        val requestQueue: RequestQueue = Volley.newRequestQueue(context)
        try {
            val mainObj = JSONObject()
            val contentObj = JSONObject()
            val notificationObj = JSONObject()
            val dataObject = JSONObject()
            notificationModel.apply {
                dataObject.put("senderId", senderId)
                dataObject.put("receiverId", receiverId)
                dataObject.put("profilePic", avatar)
                dataObject.put("msgType", msgType)
                notificationObj.put("title",title)
                notificationObj.put("body",body)
            }

            contentObj.put("token", userFcmToken)
            contentObj.put("notification",notificationObj)
            contentObj.put("data", dataObject)
            mainObj.put("message",contentObj)
            val request: JsonObjectRequest = object : JsonObjectRequest(
                Method.POST,
                postUrl,
                mainObj,
                Response.Listener {

                },
                Response.ErrorListener {
                    Log.d("volleyError",it.message.toString())

                }) {
                override fun getHeaders(): Map<String, String> {
                    val header: MutableMap<String, String> = HashMap()
                    header["content-type"] = "application/json"
                    header["authorization"] = "Bearer ${getAccessToken()}"
                    return header
                }
            }
            requestQueue.add(request)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}