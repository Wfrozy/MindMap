package com.frozy.mindmap

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

class SettingsViewModel() : ViewModel() {
    val Context.settingsDataStore by preferencesDataStore(
        name = "settingsDataStore"
    )
}