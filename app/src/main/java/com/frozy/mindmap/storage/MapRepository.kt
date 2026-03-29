package com.frozy.mindmap.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.frozy.mindmap.constants.FileExtension.APP_FILE_EXTENSION
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.storage.models.LoadedMapData
import com.frozy.mindmap.storage.models.MapFileMetadata
import com.frozy.mindmap.storage.models.OperationResult
import com.frozy.mindmap.storage.models.StorageOption
import com.frozy.mindmap.storage.models.serializables.MapFileSerializable
import com.frozy.mindmap.storage.models.serializables.MapItemSerializable
import com.frozy.mindmap.storage.utils.sanitizeAndEnsureExtension
import com.frozy.mindmap.storage.utils.toDeserialized
import com.frozy.mindmap.storage.utils.toSerializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.json.Json

class MapRepository(private val context: Context) {

    /**
     * Maps a MapListEntry UUID to a MapFileMetadata.
    **/
    private val _metadataMap =
        MutableStateFlow<Map<UUID, MapFileMetadata>>(value = emptyMap())

    val metadataMap: StateFlow<Map<UUID, MapFileMetadata>> =
        _metadataMap.asStateFlow()

    private fun addMetadataEntry(metadata: MapFileMetadata){
        val entryUUID = UUID.randomUUID()
        _metadataMap.update { map ->
            map + (entryUUID to metadata)
        }
    }

    private fun renameAppStorageMetadataEntry(entryUUID: UUID, newName: String) {
        _metadataMap.update { map ->

            val metadata = map[entryUUID] ?: return@update map

            if (metadata.storedIn != StorageOption.APP) {
                return@update map
            }

            return@update map + (
                entryUUID to metadata.copy(
                    fileName = newName,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
    }

    private fun renameDeviceStorageMetadataEntry(
        entryUUID: UUID,
        newName: String,
        newUri: Uri
    ) {
        _metadataMap.update { map ->

            val metadata = map[entryUUID] ?: return@update map

            if (metadata.storedIn != StorageOption.DEVICE) {
                return@update map
            }

            map + (
                entryUUID to metadata.copy(
                    fileName = newName,
                    uri = newUri,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
    }

    private fun removeMetadataEntry(entryUUID: UUID) {
        _metadataMap.update { map ->
            map - entryUUID
        }
    }

    fun resolveMetadata(entryUUID: UUID): MapFileMetadata {
        return _metadataMap.value[entryUUID] ?: error("_metadataMap.value[entryUUID] returned null")
    }

    suspend fun importMapFilesFromUris(uris: List<Uri>): List<OperationResult> {
        val opResList = mutableListOf<OperationResult>()
        withContext(context = Dispatchers.IO) {
            uris.forEach { uri ->
                //puts the cursor at the position of the file identified by the uri
                val cursor = context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null
                )

                //.use{} autocloses the cursor
                cursor?.use { cursor ->
                    //get the current column index that the pointer was moved to (moved to the uri's position)
                    //.getColumnIndex can return -1 if the pointer was moved to an invalid position (meaning the uri doesn't exist)
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst()) {
                        if (nameIndex == -1) {
                            opResList + OperationResult.Error(
                                e = FileNotFoundException("nameIndex returned -1")
                            )
                        } else {
                            //get the file name at the given column index
                            val fileName = cursor.getString(nameIndex)

                            if(
                                !fileName.endsWith(suffix = APP_FILE_EXTENSION, ignoreCase = true) &&
                                !fileName.endsWith(suffix = ".json", ignoreCase = true)
                            ){
                                opResList + OperationResult.Error(
                                    e = FileNotFoundException("Invalid extension")
                                )
                            } else {
                                addMetadataEntry(
                                    metadata = MapFileMetadata(
                                        fileName = fileName,
                                        storedIn = StorageOption.DEVICE,
                                        uri = uri
                                    )
                                )
                                opResList + OperationResult.Success
                            }
                        }
                    }
                }
            }
        }
        return opResList
    }

    //lists files stored in app storage
    suspend fun loadMapFilesFromAppStorage(): OperationResult {
        withContext(context = Dispatchers.IO) {
            try {
                var files = context.filesDir.listFiles()?.toList() ?: emptyList()
                files = files.filter { f -> f.name.endsWith(suffix = APP_FILE_EXTENSION) }

                for (file in files) {
                    addMetadataEntry(
                        metadata = MapFileMetadata(
                            fileName = file.name,
                            filePath = file.path,
                            storedIn = StorageOption.APP,
                        )
                    )
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            }
        }
        return OperationResult.Success
    }



    suspend fun createMapFileInAppStorage(mapName: String): OperationResult {
        withContext(context = Dispatchers.IO) {
            val mapName = mapName.sanitizeAndEnsureExtension()
            val file = File(context.filesDir, mapName)
            Log.d("", "mapName: $mapName")
            Log.d("", "file: $file")

            try {
                if (!file.exists()) {
                    //write text creates a new file if it doesn't exist
                    file.writeText(text = "{}") // empty JSON map file
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            } catch (e: IOException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            }

            val metadata = MapFileMetadata(
                fileName = mapName,
                filePath = file.path,
                storedIn = StorageOption.APP,
                lastModified = file.lastModified(),
                uri = null
            )

            addMetadataEntry(metadata)
        }
        return OperationResult.Success
    }

    suspend fun createMapFileInDeviceStorage(mapName: String, uri: Uri): OperationResult {
        withContext(context = Dispatchers.IO) {
            val mapName = mapName.sanitizeAndEnsureExtension()
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write("{}".toByteArray())
                }
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            } catch (e: SecurityException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            } catch (e: IOException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            }

            val metadata = MapFileMetadata(
                fileName = mapName,
                storedIn = StorageOption.DEVICE,
                uri = uri,
                lastModified = System.currentTimeMillis()
            )

            addMetadataEntry(metadata)
        }
        return OperationResult.Success
    }

    suspend fun renameMapFileInAppStorage(
        entryUUID: UUID,
        newMapName: String
    ): OperationResult {
        return withContext(context = Dispatchers.IO) {
            val metadata = resolveMetadata(entryUUID)
            val sanitizedName = newMapName.sanitizeAndEnsureExtension()
            try {

                val oldFile = File(context.filesDir, metadata.fileName)
                val newFile = File(context.filesDir, sanitizedName)

                if (!oldFile.exists()) {
                    return@withContext OperationResult.Error(
                        FileNotFoundException(metadata.fileName)
                    )
                }

                val wasRenameSuccessful = oldFile.renameTo(newFile)

                if (!wasRenameSuccessful) {
                    return@withContext OperationResult.Error(
                        e = IOException("Failed to rename file")
                    )
                }

                renameAppStorageMetadataEntry(
                    entryUUID = entryUUID,
                    newName = sanitizedName
                )

                OperationResult.Success

            } catch (e: SecurityException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            } catch (e: IOException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            }
        }
    }

    suspend fun renameMapFileInDeviceStorage(
        entryUUID: UUID,
        newMapName: String
    ): OperationResult {
        return withContext(context = Dispatchers.IO) {
            val metadata = resolveMetadata(entryUUID)
            val sanitizedName = newMapName.sanitizeAndEnsureExtension()
            Log.v("", "permsBefore: ${context.contentResolver.persistedUriPermissions}")
            try {
                val newUri = DocumentsContract.renameDocument(
                    context.contentResolver,
                    //this function only ever gets called when the map being dealt with is in device storage, so
                    //uri will never be null
                    metadata.uri!!,
                    sanitizedName
                ) ?: return@withContext OperationResult.Error(
                    e = IOException("Rename returned null")
                )
                Log.v("", "permsAfter: ${context.contentResolver.persistedUriPermissions}")
                //todo fix this!!!
//                try {
//                    context.contentResolver.takePersistableUriPermission(
//                        newUri,
//                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                    )
//                } catch (e: SecurityException) {
//                    e.printStackTrace()
//                    Log.w("URI_PERMISSION", "Map creation SecurityException from $newUri", e)
//                }

                renameDeviceStorageMetadataEntry(
                    newUri = newUri,
                    newName = sanitizedName,
                    entryUUID = entryUUID
                )

                OperationResult.Success

            } catch (e: SecurityException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            } catch (e: IOException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                OperationResult.Error(e = e)
            }
        }
    }

    suspend fun deleteMapFileInAppStorage(entryUUID: UUID): OperationResult {
        val metadata = resolveMetadata(entryUUID = entryUUID)
        return withContext(context = Dispatchers.IO) {
            val file = File(context.filesDir, metadata.fileName)

            if (!file.exists()) {
                return@withContext OperationResult.Error(e = FileNotFoundException("File not found."))
            }

            val wasDeleted = file.delete()

            if (wasDeleted) {
                removeMetadataEntry(entryUUID = entryUUID)
                return@withContext OperationResult.Success
            } else {
                return@withContext OperationResult.Error(e = Exception("Failed to delete file."))
            }
        }
    }

    suspend fun deleteMapFileInDeviceStorage(entryUUID: UUID): OperationResult {
        val metadata = resolveMetadata(entryUUID)
        return withContext(context = Dispatchers.IO) {
            try {
                //delete the file that has that specific uri
                val wasDeleted = DocumentsContract.deleteDocument(
                    context.contentResolver,
                    metadata.uri!!
                )

                //remove the stored uri
                context.contentResolver.persistedUriPermissions.filterNot { uriPermission ->
                    uriPermission.uri == metadata.uri
                }

                if (wasDeleted) {
                    removeMetadataEntry(entryUUID = entryUUID)
                    return@withContext OperationResult.Success
                } else {
                    return@withContext OperationResult.Error(e = Exception("Failed to delete file."))
                }

            } catch (e: SecurityException) {
                e.printStackTrace()
                return@withContext OperationResult.Error(e = e)
            } catch(e: FileNotFoundException){
                e.printStackTrace()
                return@withContext OperationResult.Error(e = e)
            } catch (e: UnsupportedOperationException) {
                e.printStackTrace()
                OperationResult.Error(e)
            }
        }
    }

    suspend fun saveMap(
        entryUUID: UUID,
        mapItems: List<MapItem>,
        lastPageIndex: Int
    ): OperationResult {
        val metadata = resolveMetadata(entryUUID)

        return withContext(context = Dispatchers.IO) {
            try {
                val fileData = MapFileSerializable(
                    serializedItems = mapItems.map { item ->
                        when (item) {
                            is MapItem.Note -> MapItemSerializable.Note(data = item.toSerializable())
                            is MapItem.Space -> MapItemSerializable.Space(data = item.toSerializable())
                        }
                    },
                    lastPageIndex = lastPageIndex
                )
                val json = Json.encodeToString(value = fileData)

                when (metadata.storedIn) {
                    StorageOption.APP -> {
                        File(context.filesDir, metadata.fileName).writeText(json)
                    }
                    StorageOption.DEVICE -> {
                        //wt means write + truncate (not wintrader)
                        context.contentResolver.openOutputStream(metadata.uri!!, "wt")?.use {
                            it.write(json.toByteArray(Charsets.UTF_8))
                        } ?: return@withContext OperationResult.Error(
                            e = IOException("OutputStream was null")
                        )
                    }
                }
                OperationResult.Success
            } catch (e: IOException) {
                OperationResult.Error(e = e)
            } catch (e: SecurityException) {
                OperationResult.Error(e = e)
            }
        }
    }

    suspend fun loadMap(
        entryUUID: UUID,
        lastPageIndex: Int
    ): LoadedMapData? {
        val metadata = resolveMetadata(entryUUID)

        return withContext(context = Dispatchers.IO) {
            try {
                val json = when (metadata.storedIn) {
                    StorageOption.APP -> {
                        File(context.filesDir, metadata.fileName).readText()
                    }
                    StorageOption.DEVICE -> {
                        context.contentResolver.openInputStream(metadata.uri!!)?.use {
                            it.bufferedReader(Charsets.UTF_8).readText()
                        } ?: return@withContext null
                    }
                }

                val fileData = Json.decodeFromString<MapFileSerializable>(string = json)
                return@withContext LoadedMapData(
                    items = fileData.serializedItems.map { item ->
                        when (item) {
                            is MapItemSerializable.Note  -> item.data.toDeserialized()
                            is MapItemSerializable.Space -> item.data.toDeserialized()
                        }
                    },
                    lastPageIndex = lastPageIndex
                )
            } catch (e: Exception) { //todo make this better
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}