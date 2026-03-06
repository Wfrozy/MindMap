package com.frozy.mindmap.storage.utils

import com.frozy.mindmap.storage.FileData
import com.frozy.mindmap.storage.StorageOption
import org.json.JSONObject

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