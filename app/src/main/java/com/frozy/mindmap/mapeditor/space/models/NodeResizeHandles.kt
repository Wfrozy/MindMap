package com.frozy.mindmap.mapeditor.space.models

import androidx.compose.ui.geometry.Rect

data class NodeResizeHandles(
    val topLeft: Rect,
    val topRight: Rect,
    val bottomLeft: Rect,
    val bottomRight: Rect,
) {
    val entries: List<Pair<Rect, ResizeHandleType>>
        get() = listOf(
            topLeft to ResizeHandleType.TOP_LEFT,
            topRight to ResizeHandleType.TOP_RIGHT,
            bottomLeft  to ResizeHandleType.BOTTOM_LEFT,
            bottomRight to ResizeHandleType.BOTTOM_RIGHT,
        )
}