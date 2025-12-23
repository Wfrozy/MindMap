package com.frozy.mindmap

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.json.JSONObject


//this file includes extension functions

fun JSONObject.applyBasicContent(
    fileName: String,
    selectedStorage: StorageOption,
    fileContent: FileData
): JSONObject {
    this.apply {
        put("fileName", fileName)
        put("storage", selectedStorage.name)
        put("createdAt", System.currentTimeMillis())
        put("fileContent", fileContent.fileContent)
        put("storedIn", selectedStorage.label)
    }
    return this
}

//ensure filename ends with .json and does not contain invalid chars in the file name
fun String.sanitizeAndEnsureJsonExtension(fallbackString: String): String {
    val trimmed = this.trim().ifBlank { fallbackString }

    //replace illegal file chars with underscore
    val sanitized = trimmed.replace(Regex("[/\\\\:*?\"<>|]"), "_")

    return if (sanitized.endsWith(".json", ignoreCase = true)) sanitized
    else "$sanitized.json"
}

//todo fix this
fun String.checkIfNameExists(
    fileList: List<FileData>
): Boolean {
    var doesNameExist = false
    for(file in fileList){ doesNameExist = (this == file.fileName); if(doesNameExist) break }
    return doesNameExist
}

fun Activity.hideSystemStatusBar(){
    WindowCompat.setDecorFitsSystemWindows(this.window, false)

    val controller = WindowInsetsControllerCompat(this.window, this.window.decorView)

    controller.hide(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}