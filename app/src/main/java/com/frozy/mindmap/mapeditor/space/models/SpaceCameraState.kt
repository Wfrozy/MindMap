package com.frozy.mindmap.mapeditor.space.models

import androidx.compose.ui.geometry.Offset

data class SpaceCameraState(
    val offset: Offset = Offset.Zero, //coordinates (aka Offset) of the top-left corner of the screen
    val overscrollX: Float = 0f,
    val scale: Float = 1f
)