package com.frozy.mindmap.ui.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.frozy.mindmap.constants.FileExtension.APP_FILE_EXTENSION
import com.frozy.mindmap.mapeditor.MapEditorActivity
import com.frozy.mindmap.settings.SettingsActivity
import com.frozy.mindmap.storage.models.MapFileMetadata

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

fun openSelectedMap(context: Context, metadata: MapFileMetadata){

    Log.d("openSelectedMap", "metadata: $metadata")

    val intent = Intent(context, MapEditorActivity::class.java).apply {
        putExtra("fileName", metadata.fileName.removeSuffix(suffix = APP_FILE_EXTENSION).removeSuffix(suffix = ".json"))
        putExtra("filePath", metadata.filePath)
        putExtra("storage", metadata.storedIn.name)
        putExtra("timeStampID", metadata.lastModified)
        putExtra("uri", metadata.uri)
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