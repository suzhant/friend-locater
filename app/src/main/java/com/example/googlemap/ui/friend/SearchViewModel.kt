package com.example.googlemap.ui.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.googlemap.model.UserData

class SearchViewModel : ViewModel() {

    private var _userData = MutableLiveData<UserData>()
    val userData : LiveData<UserData> get() = _userData

    fun setUserData(userData: UserData){
        _userData.value = userData
    }
}