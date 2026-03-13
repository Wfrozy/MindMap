package com.frozy.mindmap.mapeditor.models

import androidx.compose.ui.geometry.Rect
data class NodeResizeHandles(
    val topLeft: Rect,
    val topRight: Rect,
    val bottomLeft: Rect,
    val bottomRight: Rect,
) {
    val entries = listOf(
        topLeft, topRight, bottomLeft, bottomRight
    )
}