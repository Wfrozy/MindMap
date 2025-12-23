package com.frozy.mindmap

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream


//this file contains functions related to file IO operations


object FileIO {

    //writes data (json text) to the selected uri (file path)
    suspend fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
        return withContext(Dispatchers.IO) {
            var outputStream: OutputStream? = null
            try {
                outputStream = context.contentResolver.openOutputStream(uri)
                outputStream?.write(text.toByteArray(Charsets.UTF_8))
                outputStream?.flush()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                try { outputStream?.close() } catch (_: Exception) {}
            }
        }
    }

    //write text to file in app storage
    suspend fun writeTextToFileInAppStorage(context: Context, filename: String, text: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.openFileOutput(filename, Context.MODE_PRIVATE).use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    //reads text from file in app storage
    suspend fun readTextFromFileInAppStorage(context: Context, filename: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                context.openFileInput(filename).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    //lists files stored in app storage
    //gets executed at recomposition
    suspend fun listFilesInAppStorage(context: Context): List<File> {
        var files = withContext(Dispatchers.IO) {
            context.filesDir.listFiles()?.toList() ?: emptyList()
        }
        files = files.filter { f -> f.name.endsWith(suffix = ".json") }
        return files
    }

    //make unique filename if clash (adds ([number]) to the file name)
    fun makeUniqueFilename(dir: File, baseName: String): String {
        var candidate = baseName
        var index = 1
        while (File(dir, candidate).exists()) {
            val nameWithoutExt = baseName.removeSuffix(".json")
            candidate = "$nameWithoutExt($index).json"
            index++
        }
        return candidate
    }

    fun deleteFileInAppStorage(context: Context, filename: String): Boolean {
        val file = File(context.filesDir, filename)
        return file.exists() && file.delete()
    }
}
