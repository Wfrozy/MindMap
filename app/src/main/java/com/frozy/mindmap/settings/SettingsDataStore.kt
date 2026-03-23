package com.frozy.mindmap.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

//todo
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")