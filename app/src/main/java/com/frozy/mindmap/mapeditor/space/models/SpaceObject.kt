package com.frozy.mindmap.mapeditor.space.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType
import java.util.UUID

sealed class SpaceObject(
    open val uuid: UUID
) {

    sealed class Node(
        override val uuid: UUID,
        open val offset: Offset,
        open val width: Float,
        open val height: Float,
        open val isSelected: Boolean,
        open val borderColor: Color?,
        open val backgroundColor: Color?,
    ) : SpaceObject(uuid) {

        //todo remove isSelected from this class
        //todo fix bytearray warning
        data class ImageNode(
            override val uuid: UUID = UUID.randomUUID(),
            override val offset: Offset,
            override val width: Float,
            override val height: Float,
            override val isSelected: Boolean = false,
            override val borderColor: Color?,
            override val backgroundColor: Color?,
            val bitmapBytes: ByteArray
        ) : Node(uuid, offset, width, height, isSelected, borderColor, backgroundColor)

        //todo remove isSelected from this class
        data class TextNode(
            override val uuid: UUID = UUID.randomUUID(),
            override val offset: Offset,
            override val width: Float,
            override val height: Float,
            override val isSelected: Boolean = false,
            override val borderColor: Color?,
            override val backgroundColor: Color?,
            val text: String,
            val fontSize: TextUnit
        ) : Node(uuid, offset, width, height, isSelected, borderColor, backgroundColor)
    }

    data class NodeLink(
        override val uuid: UUID = UUID.randomUUID(),
        val fromNodeUUID: UUID,
        val fromNodeSide: NodeSideType,
        val toNodeUUID: UUID,
        val toNodeSide: NodeSideType,
    ) : SpaceObject(uuid)
}