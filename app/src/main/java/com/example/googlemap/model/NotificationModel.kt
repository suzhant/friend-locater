package com.example.googlemap.model

data class NotificationModel(
    val title : String?,
    val body : String?,
    val avatar : String?,
    val senderId: String?,
    val receiverId : String?,
    val msgType : String?,
)