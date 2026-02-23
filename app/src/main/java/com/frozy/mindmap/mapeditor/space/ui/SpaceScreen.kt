package com.frozy.mindmap.mapeditor.space.ui

import android.app.Activity
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.R
import com.frozy.mindmap.mapeditor.MapEditorViewModel
import com.frozy.mindmap.mapeditor.space.SpaceCameraState
import com.frozy.mindmap.ui.components.BottomSheetItem
import kotlinx.coroutines.launch

const val MIN_WORLD_X = -2000f
const val MAX_WORLD_X = 2000f
const val MAX_OVERSCROLL = 150f
const val DRAG_THRESHOLD = 50f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceScreen(
    activity: Activity?,
    nodes: List<MapEditorViewModel.SpaceNode>,
    pagerState: PagerState,
    pagerListSize: Int,
    currentPagerIndex: Int
) {
    val coroutineScope = rememberCoroutineScope()

    var camera by remember { mutableStateOf(value = SpaceCameraState()) }

    val sheetState = rememberModalBottomSheetState()
    var isNodeSheetVisible by remember { mutableStateOf(value = false) }

    //todo [small] animation stuff on the boundaries
//    val overscrollAnim = remember { Animatable(initialValue = 0f) }
//    val overscrollAnimValue = overscrollAnim.value

    //starts at 0 but then gets the value once the Canvas gets drawn
    var canvasSize by remember { mutableStateOf(value = Size.Zero) }
    val leftArrowPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack)
    val rightArrowPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowForward)

    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val edgeColor = MaterialTheme.colorScheme.onBackground
    val arrowColor = MaterialTheme.colorScheme.onSurface

    val leftEdgeCam = -camera.offset.x / camera.scale
    val rightEdgeCam = leftEdgeCam + canvasSize.width / camera.scale

    val fadeWidth = 60f
    val arrowSize = 42f

    val isCameraAtLeftBoundary = leftEdgeCam <= MIN_WORLD_X + 5f
    val isCameraAtRightBoundary = rightEdgeCam >= MAX_WORLD_X - 5f

    //minimum and maximum x values the camera can have on THIS CURRENT frame
    val currentMinCameraX = canvasSize.width - MAX_WORLD_X * camera.scale
    val currentMaxCameraX = -MIN_WORLD_X * camera.scale

    val currentOverscrollValue =
        when {
            camera.offset.x < currentMinCameraX -> currentMinCameraX - camera.offset.x
            camera.offset.x > currentMaxCameraX -> camera.offset.x - currentMaxCameraX
            else -> 0f
        }

    val leftAlpha by animateFloatAsState(
        targetValue =
            if (isCameraAtLeftBoundary) 0.25f
            else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    val rightAlpha by animateFloatAsState(
        targetValue =
            if (isCameraAtRightBoundary) 0.25f
            else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(key1 = pagerState.currentPage) {
        if (canvasSize != Size.Zero) {
            camera = camera.copy(
                offset = Offset(
                    x = canvasSize.width / 2f,
                    y = canvasSize.height / 2f
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(key1 = Unit){
                detectTapGestures(onLongPress = {
                    Log.d("hi", "it worked")
                    coroutineScope.launch {
                        sheetState.show()
                    }.invokeOnCompletion {
                        isNodeSheetVisible = true
                    }
                })
            }
            //todo [small for now] make camera not reset after recomposition
            .pointerInput(key1 = Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (camera.scale * zoom).coerceIn(0.4f, 3f)
                    val scaleChange = newScale / camera.scale
                    val rawOffset =
                        (camera.offset + pan) + (camera.offset - centroid) * (scaleChange - 1f)

                    //minimum and maximum x values the camera can have on the FUTURE frame
                    val minCameraX = canvasSize.width - MAX_WORLD_X * newScale
                    val maxCameraX = -MIN_WORLD_X * newScale

                    val restrictedOffsetX = rawOffset.x.coerceIn(minCameraX, maxCameraX)
                    camera = camera.copy(
                        offset = Offset(x = restrictedOffsetX, y = rawOffset.y),
                        scale = newScale
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { newSize ->
                    //when the canvas is loaded, update the canvasSize variable
                    //this setup is needed because canvasSize is needed before the Canvas
                    canvasSize = Size(
                        width = newSize.width.toFloat(),
                        height = newSize.height.toFloat()
                    )

                    if (camera.offset == Offset.Zero) {
                        camera = camera.copy(
                            offset = Offset(
                                x = newSize.width / 2f,
                                y = newSize.height / 2f
                            )
                        )
                    }
                }
        ) {
            val fadeWidthPx = fadeWidth.dp.toPx()
            val arrowSizePx = arrowSize.dp.toPx()

            drawInfiniteDotGrid(
                camera = camera,
                dotRadius = 1.5.dp.toPx(),
                dotSpacing = 24.dp.toPx(),
                dotColor = dotColor,
            )

            if (leftAlpha > 0f) {
                drawLeftBoundaryFade(
                    color = edgeColor,
                    width = fadeWidthPx,
                    alpha = leftAlpha,
                    overscroll = currentOverscrollValue
                )

                val arrowX = fadeWidthPx / 2f - arrowSizePx / 2f
                val arrowY = size.height / 2f - arrowSizePx / 2f

                if (pagerState.canScrollBackward) {
                    drawBoundaryArrow(
                        painter = leftArrowPainter,
                        alpha = leftAlpha,
                        translateLeft = arrowX,
                        translateTop = arrowY,
                        drawSizeWidth = arrowSizePx,
                        drawSizeHeight = arrowSizePx,
                        tintColor = arrowColor
                    )
                }
            }

            if (rightAlpha > 0f) {
                drawRightBoundaryFade(
                    color = edgeColor,
                    width = fadeWidthPx,
                    alpha = rightAlpha,
                    overscroll = currentOverscrollValue,
                )

                val arrowX = size.width - fadeWidthPx / 2f - arrowSizePx / 2f
                val arrowY = size.height / 2f - arrowSizePx / 2f

                if (pagerState.canScrollForward) {
                    drawBoundaryArrow(
                        painter = rightArrowPainter,
                        alpha = rightAlpha,
                        translateLeft = arrowX,
                        translateTop = arrowY,
                        drawSizeWidth = arrowSizePx,
                        drawSizeHeight = arrowSizePx,
                        tintColor = arrowColor
                    )
                }
            }

            reddottest(camera)
        }
        BoundaryInteractionArea(
            width = fadeWidth,
            isAtLeftBoundary = true,
            overscroll = currentOverscrollValue,
            canvasSize = canvasSize,
            camera = camera,
            pagerState = pagerState,
            onDragEnd = { camera = camera.copy(overscrollX = 0f) },
            onDragCancel = { camera = camera.copy(overscrollX = 0f) }
        ) { _, dragAmount ->
            camera = camera.copy(
                overscrollX = (camera.overscrollX + dragAmount).coerceAtMost(maximumValue = MAX_OVERSCROLL)
            )
            if(dragAmount > DRAG_THRESHOLD && pagerState.canScrollBackward){
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }
        }

        BoundaryInteractionArea(
            width = fadeWidth,
            isAtRightBoundary = true,
            overscroll = currentOverscrollValue,
            canvasSize = canvasSize,
            camera = camera,
            pagerState = pagerState,
            onDragEnd = { camera = camera.copy(overscrollX = 0f) },
            onDragCancel = { camera = camera.copy(overscrollX = 0f) }
        ) { _, dragAmount ->
            camera = camera.copy(
                overscrollX = (camera.overscrollX + dragAmount).coerceAtLeast(minimumValue = -MAX_OVERSCROLL)
            )
            if(dragAmount < -DRAG_THRESHOLD && pagerState.canScrollForward){
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }

    if(isNodeSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    isNodeSheetVisible = false
                }
            },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(all = 16.dp)) {
                BottomSheetItem(
                    icon = Icons.Default.LocalActivity,
                    contentDescription = stringResource(R.string.contentDescription_map_editor_add_new_node),
                    text = stringResource(R.string.map_editor_add_new_node),
                    itemOnClick = {
                        //todo
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isNodeSheetVisible = false
                        }
                    },
                )

                BottomSheetItem(
                    icon = Icons.Default.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.map_editor_add_image),
                    text = stringResource(R.string.map_editor_add_image),
                    itemOnClick = {
                        //todo
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isNodeSheetVisible = false
                        }
                    },
                    includeSpacer = false
                )
            }
        }
    }
}