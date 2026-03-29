package com.frozy.mindmap.mapeditor.models

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType
import java.util.UUID

sealed class MapItemObject(
    open val uuid: UUID,
) {

    data class Image(
        override val uuid: UUID = UUID.randomUUID(),
        val offset: Offset,
        val width: Float,
        val height: Float,
        val imageUri: Uri
    ) : MapItemObject(uuid)

    //todo remove isSelected from this class
    data class SpaceNode(
        override val uuid: UUID = UUID.randomUUID(),
        val offset: Offset,
        val width: Float,
        val height: Float,
        val isSelected: Boolean = false,
        val borderColor: Color?,
        val backgroundColor: Color?,
        val text: String,
        val fontSize: TextUnit
    ) : MapItemObject(uuid)

    data class SpaceNodeLink(
        override val uuid: UUID = UUID.randomUUID(),
        val fromNodeUUID: UUID,
        val fromNodeSide: NodeSideType,
        val toNodeUUID: UUID,
        val toNodeSide: NodeSideType,
    ) : MapItemObject(uuid)
}