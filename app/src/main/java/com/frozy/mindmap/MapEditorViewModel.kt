package com.frozy.mindmap

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MapEditorViewModel(private val application: Application) : AndroidViewModel(application) {

    //just in case I need context
    val context = getApplication<Application>()

    private val _isEditorModeEnabled = MutableStateFlow(value = false)
    val isEditorModeEnabled: StateFlow<Boolean> = _isEditorModeEnabled.asStateFlow()
    fun changeEditorModeState(value: Boolean) { _isEditorModeEnabled.value = value }

    private val _pagerList = MutableStateFlow(value = emptyList<MapItem>())
    val pagerList: StateFlow<List<MapItem>> = _pagerList.asStateFlow()
    fun changePagerList(value: List<MapItem>) { _pagerList.value = value }


    sealed class MapItem {
        val uuid: UUID = UUID.randomUUID()

        data class Note(
            val id: Int,
            val titleText: String,
            val contentText: String
        ) : MapItem() {
            companion object {
                private var nextId = 0

                fun create(
                     titleText: String = "",
                     contentText: String = ""
                ): Note {
                    return Note(
                        id = nextId++,
                        titleText = titleText,
                        contentText = contentText
                    )
                }
            }
        }

        data class Space(
            val nodeInfo: List<SpaceNode> = emptyList()
        ) : MapItem()

//        data class Image() : MapItem()
    }

    data class SpaceNode(
        val id: Int,
        val uuid: UUID,
        val offset: Offset,
        val hasBorder: Boolean,
        val text: String
    ){
        //companion objects is kotlin's replacement for static
        companion object {
            private var nextId = 0

            fun create(
                offset: Offset = Offset.Zero,
                hasBorder: Boolean = true,
                text: String = ""
            ): SpaceNode {
                return SpaceNode(
                    id = nextId++,
                    uuid = UUID.randomUUID(),
                    offset = offset,
                    hasBorder = hasBorder,
                    text = text
                )
            }
        }
    }

    data class SpaceCameraState(
        val offset: Offset = Offset.Zero,
        val scale: Float = 1f
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

