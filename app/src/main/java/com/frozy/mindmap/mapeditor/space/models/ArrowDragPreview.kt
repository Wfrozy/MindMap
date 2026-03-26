package com.frozy.mindmap.mapeditor.space.models

import androidx.compose.ui.geometry.Offset
import java.util.UUID

data class ArrowDragPreview(
    val fromNodeUUID: UUID,
    val currentScreenPos: Offset
)