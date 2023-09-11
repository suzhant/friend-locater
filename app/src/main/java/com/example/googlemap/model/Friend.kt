package com.example.googlemap.model

import android.os.Parcelable
import com.example.googlemap.model.enums.FriendStatus
import com.example.googlemap.model.enums.Presence
import kotlinx.parcelize.Parcelize

@Parcelize
data class Friend(
    val userData: UserData?,
    val status : FriendStatus? ,
    val timestamp : Long?
): Parcelable{
    constructor() : this(null,null,null)
}
