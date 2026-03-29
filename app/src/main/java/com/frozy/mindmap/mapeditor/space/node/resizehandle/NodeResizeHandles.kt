package com.frozy.mindmap.mapeditor.space.node.resizehandle

import androidx.compose.ui.geometry.Rect

data class NodeResizeHandles(
    val topLeft: Rect,
    val topRight: Rect,
    val bottomLeft: Rect,
    val bottomRight: Rect,
) {
    val entries: List<Pair<Rect, NodeResizeHandleType>>
        get() = listOf(
            topLeft to NodeResizeHandleType.TOP_LEFT,
            topRight to NodeResizeHandleType.TOP_RIGHT,
            bottomLeft  to NodeResizeHandleType.BOTTOM_LEFT,
            bottomRight to NodeResizeHandleType.BOTTOM_RIGHT,
        )
}