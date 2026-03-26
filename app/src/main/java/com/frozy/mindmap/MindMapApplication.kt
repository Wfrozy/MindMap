package com.frozy.mindmap

import android.app.Application
import com.frozy.mindmap.storage.MapRepository

class MindMapApplication : Application() {
    /*
        MapRepository is a class created once on app startup by using android:name in the
        application tag in AndroidManifest.xml

        Using a single shared instance ensures that both MainActivityViewModel
        and MapEditorViewModel read from and write to the same state
    */
    val mapRepository by lazy { MapRepository(context = this) }
}