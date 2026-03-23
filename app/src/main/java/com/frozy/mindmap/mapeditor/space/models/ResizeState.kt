package com.frozy.mindmap.mapeditor.space.models

import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
    ResizeState is a temporary interaction state that exists while the user is dragging one of a SpaceNode's resize handles.
    It stores the information needed to compute the new node size during the drag.
 **/
data class ResizeState(
    val nodeId: UUID,
    val resizingOn: NodeResizeHandles,
    val startPointer: Offset,
    val startWidth: Float,
    val startHeight: Float,
    val startOffset: Offset
)