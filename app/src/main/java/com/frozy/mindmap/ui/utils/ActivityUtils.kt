package com.frozy.mindmap.ui.utils

import android.content.Context
import android.content.Intent
import com.frozy.mindmap.mapeditor.MapEditorActivity
import java.util.UUID

fun Context.openSelectedMap(entryUUID: UUID, mapName: String){

    val intent = Intent(this, MapEditorActivity::class.java).apply {
        putExtra("entryUUID", entryUUID.toString())
        putExtra("mapName", mapName)
    }
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