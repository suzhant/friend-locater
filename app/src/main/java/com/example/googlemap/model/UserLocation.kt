package com.example.googlemap.model

data class UserLocation(
     val longitude: Double?,
     val latitude : Double?,
     val status : String?,
     val timestamp : Long?,
     val userData: UserData?
){
     constructor(): this(null,null,null,null,null)
}
