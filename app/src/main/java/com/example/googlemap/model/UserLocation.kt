package com.example.googlemap.model

data class UserLocation(
     val id: String?,
     val longitude: Double?,
     val latitude : Double?,
     val timestamp : Long?
){
     constructor(): this(null,null,null,null)
}
