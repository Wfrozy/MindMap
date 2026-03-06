package com.frozy.mindmap.mapeditor.model

import androidx.compose.ui.geometry.Offset
import java.util.UUID

/*
    ResizeState is just a temporary interaction state that exists while the user is dragging one of a SpaceNode's resize handle.
    It stores the information needed to correctly compute the new node size during the drag.
 */
data class ResizeState(
    val nodeId: UUID,
    val handle: NodeResizeHandle,
    val startPointer: Offset,
    val startWidth: Float,
    val startHeight: Float,
    val startOffset: Offset
)