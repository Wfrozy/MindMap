package com.frozy.mindmap

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import java.util.UUID

class MapEditorViewModel : ViewModel() {

    var spaceNodeID = 0
    sealed class MapItem {
        val timestampID: Long = System.currentTimeMillis()

        data class Note(
            val titleText: String = "",
            val contentText: String = ""
        ) : MapItem()

        data class Space(
            val nodeInfo: List<SpaceNode> = emptyList()
        ) : MapItem()

//        data class Image() : MapItem()
    }

    data class SpaceNode(
        val uuid: UUID = UUID.randomUUID(),
        val offset: Offset = Offset.Zero,
        val hasBorder: Boolean = true,
        val text: String = ""
    )

    data class SpaceCameraState(
        val offset: Offset = Offset.Zero,
        val scale: Float = 1f
    )
}