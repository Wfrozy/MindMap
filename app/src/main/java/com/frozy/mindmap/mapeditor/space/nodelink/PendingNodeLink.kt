package com.frozy.mindmap.mapeditor.space.nodelink

import androidx.compose.ui.geometry.Offset
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType
import java.util.UUID

data class PendingNodeLink(
    val fromNodeUUID: UUID,
    val fromNodeSide: NodeSideType,
    val currentEndPos: Offset
)