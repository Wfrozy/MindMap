package com.frozy.mindmap.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.frozy.mindmap.storage.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object FileIO {

    //writes data (json text) to the selected uri (file path)
    suspend fun writeTextToUri(
        context: Context,
        uri: Uri,
        text: String
    ): Boolean = withContext(context = Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(text.toByteArray(Charsets.UTF_8))
            } ?: return@withContext false

            return@withContext true
        } catch (e: IOException) {
            Log.w("writeTextToUri", "IO error while writing to $uri (IOException).", e)
            return@withContext false
        } catch (e: SecurityException) {
            Log.w("writeTextToUri", "Cannot access $uri (SecurityException)", e)
            return@withContext false
        } catch (e: Exception) {
            Log.w("writeTextToUri", "Unexpected error while writing to $uri (generic Exception)", e)
            return@withContext false
        }
    }

    //write text to file in app storage
    suspend fun writeTextToFileInAppStorage(
        context: Context,
        fileName: String,
        text: String
    ): Boolean {
        return withContext(context = Dispatchers.IO) {
            try {
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    it.write(text.toByteArray(Charsets.UTF_8))
                }
                return@withContext true
            } catch (e: IOException) {
                Log.w(
                    "writeTextToFileInAppStorage",
                    "IO error while writing to \"$fileName\" (IOException).",
                    e
                )
                return@withContext false
            } catch (e: SecurityException) {
                Log.w(
                    "writeTextToFileInAppStorage",
                    "Cannot access \"$fileName\" (SecurityException)",
                    e
                )
                return@withContext false
            } catch (e: Exception) {
                Log.w(
                    "writeTextToFileInAppStorage",
                    "Unexpected error while writing to \"$fileName\" (generic Exception)",
                    e
                )
                return@withContext false
            }
        }
    }

    //reads text from file in app storage
    suspend fun readTextFromFileInAppStorage(
        context: Context,
        fileName: String
    ): String? = withContext(context = Dispatchers.IO) {
        try {
            return@withContext context.openFileInput(fileName).bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
        } catch (e: FileNotFoundException) {
            Log.w(
                "readTextFromFileInAppStorage",
                "File named \"$fileName\" not found (FileNotFoundException).",
                e
            )
            return@withContext null
        } catch (e: IOException) {
            Log.w(
                "readTextFromFileInAppStorage",
                "IO error while reading \"$fileName\" (IOException).",
                e
            )
            return@withContext null
        } catch (e: SecurityException) {
            Log.w(
                "readTextFromFileInAppStorage",
                "Cannot access \"$fileName\" (SecurityException)",
                e
            )
            return@withContext null
        } catch (e: Exception) {
            Log.w(
                "readTextFromFileInAppStorage",
                "Unexpected error while reading \"$fileName\" (generic Exception)",
                e
            )
            return@withContext null
        }
    }

    //lists files stored in app storage
    suspend fun listFilesInAppStorage(
        context: Context
    ): List<File> {
        var files = withContext(Dispatchers.IO) {
            context.filesDir.listFiles()?.toList() ?: emptyList()
        }
        files = files.filter { f -> f.name.endsWith(suffix = ".json") }
        return files
    }

    //make unique filename if there is a clash (adds ([number]) to the file name)
    fun     makeFileNameUnique(
        dir: File,
        baseName: String
    ): String {
        var candidate = baseName
        var index = 1
        while (File(dir, candidate).exists()) {
            val nameWithoutExt = baseName.removeSuffix(".json")
            candidate = "$nameWithoutExt($index).json"
            index++
        }
        return candidate
    }

    suspend fun deleteFileInAppStorage(
        context: Context,
        fileName: String
    ): Boolean = withContext(context = Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            return@withContext file.exists() && file.delete()
        } catch (e: SecurityException) {
            Log.w(
                "Function deleteFileInAppStorage",
                "No permission to delete $fileName (SecurityException)",
                e
            )
            return@withContext false
        } catch (e: Exception) {
            Log.w(
                "Function deleteFileInAppStorage",
                "Unexpected error deleting $fileName (generic Exception)",
                e
            )
            return@withContext false
        }
    }

    suspend fun deleteFileInDeviceStorage(
        context: Context,
        uri: Uri?
    ): Boolean = withContext(context = Dispatchers.IO) {

        if (uri == null) {
            return@withContext false
        }

        try {
            //the android saf (storage access framework) uses a database abstraction, so that's why there is this "rows" terminology
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            return@withContext rowsDeleted > 0
        } catch (e: SecurityException) {
            Log.w(
                "deleteFileInDeviceStorage",
                "No permissions to delete file at $uri (SecurityException)",
                e
            )
            return@withContext false
        } catch (e: Exception) {
            Log.w(
                "deleteFileInDeviceStorage",
                "Unexpected error when deleting file at $uri (generic Exception)",
                e
            )
            return@withContext false
        }
    }

    fun getFileNameFromUri(
        context: Context,
        uri: Uri
    ): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    suspend fun getJsonDataFromUris(
        context: Context,
        uris: List<Uri>
    ): List<OperationResult<Pair<String, JSONObject>>> = withContext(context = Dispatchers.IO) {
        return@withContext uris.mapIndexed { index, uri ->
            try {
                var fileName = getFileNameFromUri(context, uri)
                if (fileName.isNullOrEmpty()) {
                    fileName = ""
                }
                val rawData = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: throw IOException("InputStream is null")

                if (rawData.isBlank()) {
                    throw JSONException("Empty file")
                }

                val parsedJsonData = JSONObject(rawData)
                val fileNameAndData = fileName to parsedJsonData

                return@mapIndexed OperationResult(
                    isSuccess = true,
                    data = fileNameAndData,
                    index = index
                )

            } catch (e: Throwable) {
                Log.w("getJsonDataFromUris", "Failed at index $index", e)

                return@mapIndexed OperationResult(
                    isSuccess = false,
                    errorInfo = e,
                    index = index
                )
            }
        }
    }
}