package com.frozy.mindmap.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.datastore.preferences.preferencesDataStore

class SettingsViewModel() : ViewModel() {
    val Context.settingsDataStore by preferencesDataStore(
        name = "settingsDataStore"
    )
}