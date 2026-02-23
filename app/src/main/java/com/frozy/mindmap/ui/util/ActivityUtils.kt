package com.frozy.mindmap.ui.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.frozy.mindmap.mapeditor.MapEditorActivity
import com.frozy.mindmap.settings.SettingsActivity
import com.frozy.mindmap.storage.FileData

fun Context.openSettingsActivity(){
    val intent = Intent(this, SettingsActivity::class.java)
    /*
        if the activity already exists in the current task’s back stack, Android will
        destroy every Activity above it,
        bring the existing instance to the foreground,
        reuse it instead of creating a new one,
        and prevent recreation if the activity is already on top
    */
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    this.startActivity(intent)
}

fun openSelectedMap(context: Context, file: FileData){
    Log.d("getJsonDataFromUris", "$file")
    val intent = Intent(context, MapEditorActivity::class.java).apply {
        putExtra("file_name", file.fileName)
        putExtra("storage", file.storedIn.name)
    }
    /*
        if the activity already exists in the current task’s back stack, Android will
        destroy every Activity above it,
        bring the existing instance to the foreground,
        reuse it instead of creating a new one,
        and prevent recreation if the activity is already on top
    */
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    context.startActivity(intent)
}