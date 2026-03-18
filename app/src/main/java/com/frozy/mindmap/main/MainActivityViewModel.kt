package com.frozy.mindmap.main

import android.app.Application
import com.frozy.mindmap.R
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.frozy.mindmap.constants.FileExtension.APP_FILE_EXTENSION
import com.frozy.mindmap.main.models.MapListEntry
import com.frozy.mindmap.main.models.ToastEvent
import com.frozy.mindmap.storage.MapRepository
import com.frozy.mindmap.storage.models.MapFileMetadata
import com.frozy.mindmap.storage.models.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

//todo [small] more specific toasts

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val repository = MapRepository(context = application)

    val mapEntryList: StateFlow<List<MapListEntry>> =
        repository.metadataMap.map { metadataMap ->
            metadataMap.map { (uuid, metadata) ->

                MapListEntry(
                    uuid = uuid,
                    name = metadata.fileName.removeSuffix(suffix = APP_FILE_EXTENSION).removeSuffix(suffix = ".json"),
                    lastModified = metadata.lastModified,
                    storedIn = metadata.storedIn
                )
            }.sortedByDescending { it.lastModified }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _toastEvents = MutableSharedFlow<ToastEvent>()
    val toastEvents: SharedFlow<ToastEvent> = _toastEvents.asSharedFlow()


    init {
        viewModelScope.launch(context = Dispatchers.IO) {
            //retrieve uris from the persistent storage
            val uris = context.contentResolver.persistedUriPermissions.map { it.uri }
            Log.v("", "$uris")

            //make the user's files from app storage appear
            val opRes = repository.loadMapFilesFromAppStorage()

            //make the user's files from device storage appear
            repository.importMapFilesFromUris(uris)
        }
    }

    fun resolveMetadata(entryUUID: UUID): MapFileMetadata {
        return repository.resolveMetadata(entryUUID)
    }

    fun importMaps(userSelectedUris: List<Uri>){
        viewModelScope.launch {
            val uris = context.contentResolver.persistedUriPermissions
                .map { it.uri }
                .toSet()

            //filter out uris that already exist
            val urisString = uris.map { it.toString() }
            val filteredUriss = userSelectedUris.filterNot { it in uris }
            val filteredUris = userSelectedUris.map { it.toString() }
                .filterNot { it in urisString }
            val existingUris = userSelectedUris.map { it.toString() }
                .filter { it in urisString }
            val filteredCount = userSelectedUris.size - filteredUris.size

            Log.d("", "userSelectedUris: $userSelectedUris")
            Log.d("", "uris: $uris")
            Log.d("", "existingUris: $existingUris")
            Log.d("", "filteredUris: $filteredUris")
            Log.d("", "filteredCount: $filteredCount")

            //show toast if something was filtered
            if (filteredCount > 0) {
                _toastEvents.emit(
                    value = ToastEvent(
                        messageResId = R.string.toast_number_of_already_existing_maps,
                        formatArgs = listOf(filteredCount.toString())
                    )
                )
            }

            repository.importMapFilesFromUris(filteredUriss)
        }
    }

    fun createMapInAppStorage(mapName: String, defaultName: String){
        viewModelScope.launch(context = Dispatchers.IO) {
            val name = mapName.ifEmpty {
                defaultName
            }
            val toastEventSuccess = ToastEvent(
                messageResId = R.string.toast_file_created_success,
                formatArgs = listOf(name)
            )
            val toastEventFail = ToastEvent(
                messageResId = R.string.toast_file_created_fail,
                formatArgs = listOf(name)
            )
            val opRes = repository.createMapFileInAppStorage(mapName = name)
            when(opRes) {
                is OperationResult.Success -> {
                    _toastEvents.emit(value = toastEventSuccess)
                }
                is OperationResult.Error -> {
                    _toastEvents.emit(value = toastEventFail)
                }
            }
        }
    }

    fun createMapInDeviceStorage(mapName: String, defaultName: String, uri: Uri){
        viewModelScope.launch(context = Dispatchers.IO) {
            val name = mapName.ifEmpty {
                defaultName
            }

            val toastEventSuccess = ToastEvent(
                messageResId = R.string.toast_file_created_success,
                formatArgs = listOf(name)
            )
            val toastEventFail = ToastEvent(
                messageResId = R.string.toast_file_created_fail,
                formatArgs = listOf(name)
            )
            val opRes = repository.createMapFileInDeviceStorage(name, uri)
            when(opRes) {
                is OperationResult.Success -> {
                    _toastEvents.emit(value = toastEventSuccess)
                }
                is OperationResult.Error -> {
                    _toastEvents.emit(value = toastEventFail)
                }
            }
        }
    }

    fun renameMapInAppStorage(newName: String, entryUUID: UUID){
        val toastEventSuccess = ToastEvent(messageResId = R.string.toast_file_rename_success)
        val toastEventFail = ToastEvent(messageResId = R.string.toast_file_rename_fail)
        viewModelScope.launch(context = Dispatchers.IO) {
            val opRes = repository.renameMapFileInAppStorage(
                entryUUID = entryUUID,
                newMapName = newName
            )
            when(opRes) {
                is OperationResult.Success -> {
                    _toastEvents.emit(value = toastEventSuccess)
                }
                is OperationResult.Error -> {
                    _toastEvents.emit(value = toastEventFail)
                }
            }
        }
    }

    fun renameMapInDeviceStorage(newName: String, entryUUID: UUID){
        viewModelScope.launch(context = Dispatchers.IO) {
            val toastEventSuccess = ToastEvent(messageResId = R.string.toast_file_rename_success)
            val toastEventFail = ToastEvent(messageResId = R.string.toast_file_rename_fail)
            val opRes = repository.renameMapFileInDeviceStorage(
                entryUUID = entryUUID,
                newMapName = newName,
             )
            when(opRes) {
                is OperationResult.Success -> {
                    _toastEvents.emit(value = toastEventSuccess)
                }
                is OperationResult.Error -> {
                    _toastEvents.emit(value = toastEventFail)
                }
            }
        }
    }

    fun deleteMapInAppStorage(entryUUID: UUID){
        viewModelScope.launch(context = Dispatchers.IO) {
            repository.deleteMapFileInAppStorage(entryUUID = entryUUID)
        }
    }

    fun deleteMapInDeviceStorage(
        entryUUID: UUID,
        mapName: String
    ){
        viewModelScope.launch(context = Dispatchers.IO) {
            val toastEventSuccess = ToastEvent(
                messageResId = R.string.toast_file_deleted_success,
                formatArgs = listOf(mapName)
            )
            val toastEventFail = ToastEvent(
                messageResId = R.string.toast_file_deleted_fail,
                formatArgs = listOf(mapName)
            )
            val opRes = repository.deleteMapFileInDeviceStorage(entryUUID = entryUUID)

            when(opRes) {
                is OperationResult.Success -> {
                    _toastEvents.emit(value = toastEventSuccess)
                }
                is OperationResult.Error -> {
                    _toastEvents.emit(value = toastEventFail)
                }
            }
        }
    }
}
