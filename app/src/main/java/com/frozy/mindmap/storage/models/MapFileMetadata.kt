package com.frozy.mindmap.storage.models

import android.net.Uri
import com.frozy.mindmap.constants.FileExtension.APP_FILE_EXTENSION

data class MapFileMetadata(
    val fileName: String,
    val uri: Uri? = null,
    val filePath: String? = null,
    val storedIn: StorageOption = StorageOption.DEVICE,
    val lastModified: Long = System.currentTimeMillis(),
)