package com.frozy.mindmap

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class MainActivityViewModel(private val application: Application) : AndroidViewModel(application) {
    val context = getApplication<Application>()
    private val _fileList = MutableStateFlow(value = emptyList<FileData>())
    val fileList: StateFlow<List<FileData>> = _fileList.asStateFlow()
    fun changeFileList(value: List<FileData>){ _fileList.value = value }

    init {
        initLoadFiles()
    }

    fun initLoadFiles() {
        viewModelScope.launch {
            val files = withContext(context = Dispatchers.IO) {
                val filesInAppStorage = FileIO.listFilesInAppStorage(context)
                val fileListLocal = mutableListOf<FileData>()

                filesInAppStorage.forEach { f ->
                    val text =
                        FileIO.readTextFromFileInAppStorage(context = context, filename = f.name)
                    if (!text.isNullOrEmpty()) {
                        val fileData = try {
                            val obj = JSONObject(text)
                            FileData(
                                fileName = f.name,
                                fileContent = obj.optString("fileContent", ""),
                                storage = StorageOption.APP,
                                timeStampID = obj.optLong("createdAt", f.lastModified())
                            )
                        } catch (e: JSONException) {
                            Log.w("MainActivityViewModelInitLoadFiles", "JSON Error (JSON Exception).", e)
                            FileData(
                                fileName = f.name,
                                fileContent = text,
                                storage = StorageOption.APP,
                                timeStampID = f.lastModified()
                            )
                        }
                        fileListLocal.add(fileData)
                    }
                }
                return@withContext fileListLocal.sortedByDescending { it.fileName }
            }
            _fileList.value = files
        }
    }
}
