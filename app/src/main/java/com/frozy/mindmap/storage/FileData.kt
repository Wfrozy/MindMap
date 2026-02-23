package com.frozy.mindmap.storage

import android.net.Uri
import com.frozy.mindmap.storage.StorageOption
import org.json.JSONObject
import java.io.File

data class FileData(
    val fileName: String = "",
    val fileContent: JSONObject = JSONObject(),
    val uri: Uri? = null,
    val filePath: File? = null,
    val storedIn: StorageOption = StorageOption.DEVICE,
    val timeStampID: Long = System.currentTimeMillis(),
){
    val fileNameNoJson: String get() = fileName.substringBeforeLast(".json", fileName)
}