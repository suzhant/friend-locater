package com.example.googlemap.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserData(
    val userId: String,
    val userName : String?,
    val profilePicUrl : String?,
    val email : String
): Parcelable{
    constructor() : this("",null,null,"")
}