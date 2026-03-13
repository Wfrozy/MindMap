package com.frozy.mindmap.mapeditor.models

import androidx.compose.ui.geometry.Rect
data class NodeArrowHandles(
    val top: Rect,
    val bottom: Rect,
    val left: Rect,
    val right: Rect,
) {
    val entries = listOf(
        top, bottom, left, right
    )
}