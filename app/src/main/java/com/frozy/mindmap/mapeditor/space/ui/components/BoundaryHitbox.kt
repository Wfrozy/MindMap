package com.frozy.mindmap.mapeditor.space.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.mapeditor.space.camera.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.MAX_WORLD_X
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.MIN_WORLD_X

@Composable
fun BoxScope.BoundaryHitbox(
    camera: SpaceCameraState,
    canvasSize: Size,
    width: Float,
    overscroll: Float,
    pagerState: PagerState,
    isAtLeftBoundary: Boolean = false,
    isAtRightBoundary: Boolean = false,
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDetectDragGestures: (PointerInputChange, Float) -> Unit
){
    val leftEdgeCam = -camera.offset.x / camera.scale
    val rightEdgeCam = leftEdgeCam + canvasSize.width / camera.scale

    val isCameraAtLeftBoundary = leftEdgeCam <= MIN_WORLD_X + 5f
    val isCameraAtRightBoundary = rightEdgeCam >= MAX_WORLD_X - 5f

    //depending on which boundary was chosen, the variable depends on a different condition
    val isCameraAtScrollableBoundary = when {
        isAtLeftBoundary -> isCameraAtLeftBoundary && pagerState.canScrollBackward
        isAtRightBoundary -> isCameraAtRightBoundary && pagerState.canScrollForward
        else -> isCameraAtLeftBoundary
    }

    val boxAlignment: Alignment = when {
        isAtLeftBoundary -> Alignment.CenterStart
        isAtRightBoundary -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }


    if(isCameraAtScrollableBoundary){
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(width = (width.dp + overscroll.dp))
                .align(boxAlignment)
                .pointerInput(key1 = Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel
                    ) { change, dragAmount ->
                        onDetectDragGestures(change, dragAmount)
                    }
                }
        )
    }
}