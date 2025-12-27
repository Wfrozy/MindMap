package com.frozy.mindmap

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

//todo and pls don't get stage 4 cancer while doing so
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")