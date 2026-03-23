package com.frozy.mindmap.mapeditor.space.models

import androidx.compose.ui.geometry.Rect

data class NodeArrowHandles(
    val top: Rect,
    val bottom: Rect,
    val left: Rect,
    val right: Rect,
) {
    val entries: List<Pair<Rect, ArrowHandleType>>
        get() = listOf(
            top to ArrowHandleType.TOP,
            bottom to ArrowHandleType.BOTTOM,
            left to ArrowHandleType.LEFT,
            right to ArrowHandleType.RIGHT
        )
}