package com.frozy.mindmap.mapeditor.space.node.arrowhandle

import androidx.compose.ui.geometry.Rect
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType

data class NodeArrowHandles(
    val top: Rect,
    val bottom: Rect,
    val left: Rect,
    val right: Rect,
) {
    val entries: List<Pair<Rect, NodeSideType>>
        get() = listOf(
            top to NodeSideType.TOP,
            bottom to NodeSideType.BOTTOM,
            left to NodeSideType.LEFT,
            right to NodeSideType.RIGHT
        )
}