package com.example.googlemap.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingPref(private val context: Context, private val key : Preferences.Key<Boolean>){

    companion object{
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    }

     suspend fun isLocationTracked(isTrack : Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = isTrack
        }
    }


    val getLocationTrack : Flow<Boolean> =  context.dataStore.data
        .map { preferences ->
            preferences[key] ?: false
        }
}
