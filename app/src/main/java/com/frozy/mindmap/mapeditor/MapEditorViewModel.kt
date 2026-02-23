package com.frozy.mindmap.mapeditor

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MapEditorViewModel(application: Application) : AndroidViewModel(application) {

    //just in case I need context
    val context = getApplication<Application>()

    private val _isEditorModeEnabled = MutableStateFlow(value = false)
    val isEditorModeEnabled: StateFlow<Boolean> = _isEditorModeEnabled.asStateFlow()
    fun changeEditorModeState(value: Boolean) { _isEditorModeEnabled.value = value }

    private val _pagerList = MutableStateFlow(value = emptyList<MapItem>())
    val pagerList: StateFlow<List<MapItem>> = _pagerList.asStateFlow()
    fun changePagerList(value: List<MapItem>) { _pagerList.value = value }


    sealed class MapItem(open val uuid: UUID) {

        data class Note(
            override val uuid: UUID = UUID.randomUUID(),
            val titleText: String = "",
            val contentText: String = ""
        ) : MapItem(uuid)

        data class Space(
            override val uuid: UUID = UUID.randomUUID(),
            val nodeInfo: List<SpaceNode> = emptyList()
        ) : MapItem(uuid)

//        data class Image() : MapItem()
    }

    data class SpaceNode(
        val id: Int,
        val uuid: UUID,
        val offset: Offset,
        val borderColor: Color?,
        val text: String
    )


    fun changeNoteTitle(noteUUID: UUID, newTitle: String) {
        _pagerList.value = _pagerList.value.map { item ->
            if (item is MapItem.Note && item.uuid == noteUUID) {
                item.copy(titleText = newTitle)
            } else item
        }
    }

    fun changeNoteContent(noteUUID: UUID, newContent: String) {
        _pagerList.value = _pagerList.value.map { item ->
            if (item is MapItem.Note && item.uuid == noteUUID) {
                item.copy(contentText = newContent)
            } else item
        }
    }
}

