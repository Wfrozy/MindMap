package com.frozy.mindmap.mapeditor.space.ui.components

import android.app.Activity
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.R
import com.frozy.mindmap.mapeditor.MapEditorViewModel
import com.frozy.mindmap.mapeditor.models.HitAt
import com.frozy.mindmap.mapeditor.models.InteractionType
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.mapeditor.models.MapItemObject
import com.frozy.mindmap.mapeditor.models.ResizeState
import com.frozy.mindmap.mapeditor.models.SpaceCameraState
import com.frozy.mindmap.mapeditor.models.SpaceValues.DRAG_THRESHOLD
import com.frozy.mindmap.mapeditor.models.SpaceValues.MAX_OVERSCROLL
import com.frozy.mindmap.mapeditor.models.SpaceValues.MAX_WORLD_X
import com.frozy.mindmap.mapeditor.models.SpaceValues.MIN_WORLD_X
import com.frozy.mindmap.mapeditor.space.ui.utils.buildNodeLayout
import com.frozy.mindmap.mapeditor.space.ui.utils.categorizeHitAtType
import com.frozy.mindmap.mapeditor.space.ui.utils.returnHitNodeOrNull
import com.frozy.mindmap.ui.components.BottomSheetItem
import com.frozy.mindmap.ui.utils.hideSystemStatusBar
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceScreen(
    activity: Activity?,
    mevm: MapEditorViewModel,
    mapItemUUID: UUID,
    onAddNode: (Size, SpaceCameraState, Offset) -> Unit,
    onNodeHit: (MapItemObject.SpaceNode, UUID) -> Unit,
    pagerState: PagerState,
) {
    val coroutineScope = rememberCoroutineScope()
    val mipl = mevm.mapItemPagerList.collectAsState()

    var resizeState by remember { mutableStateOf<ResizeState?>(value = null) }

    val thisSpace by remember(key1 = mipl) {
        derivedStateOf {
            mipl.value.first { mapItem ->
                mapItem is MapItem.Space && mapItem.uuid == mapItemUUID
            } as MapItem.Space
        }
    }

    val nodes by remember(key1 = mipl) {
        derivedStateOf {
            (mipl.value.first { mapItem ->
                mapItem is MapItem.Space && mapItem.uuid == mapItemUUID
            } as MapItem.Space).spaceNodeInfo
        }
    }


    var camera by remember { mutableStateOf(value = SpaceCameraState()) }
    var interaction by remember { mutableStateOf<InteractionType>(value = InteractionType.Idle) }

    //build all layouts at the start of the frame
    //rememberUpdatedState is for updating the value of layouts that .pointerInput() receives
    val l = nodes.map { node -> return@map node.buildNodeLayout(camera) }

    val layoutsState = rememberUpdatedState(newValue = l)

    val layouts by remember(key1 = layoutsState){
        derivedStateOf{ layoutsState.value }
    }

    //starts at 0 but then gets the value once a Canvas gets drawn
    var canvasSize by remember { mutableStateOf(value = Size.Zero) }

    val sheetState = rememberModalBottomSheetState()
    var isNodeSheetVisible by remember { mutableStateOf(value = false) }

    //todo [small] animation stuff on the boundaries
//    val overscrollAnim = remember { Animatable(initialValue = 0f) }
//    val overscrollAnimValue = overscrollAnim.value


    val boundaryLeftArrowPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack)
    val boundaryRightArrowPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowForward)

    val nodeArrowUpPainter = rememberVectorPainter(image = Icons.Default.KeyboardArrowUp)
    val nodeArrowDownPainter = rememberVectorPainter(image = Icons.Default.KeyboardArrowDown)
    val nodeArrowLeftPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.KeyboardArrowLeft)
    val nodeArrowRightPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.KeyboardArrowRight)

    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val edgeColor = MaterialTheme.colorScheme.onBackground
    val arrowColor = MaterialTheme.colorScheme.onSurface
    val selectedNodeBorderColor = MaterialTheme.colorScheme.tertiary

    val textMeasurer = rememberTextMeasurer()

    val leftEdgeCam = -camera.offset.x / camera.scale
    val rightEdgeCam = leftEdgeCam + canvasSize.width / camera.scale

    val fadeWidth = 60f
    val arrowSize = 42f

    val isCameraAtLeftBoundary = leftEdgeCam <= MIN_WORLD_X + 5f
    val isCameraAtRightBoundary = rightEdgeCam >= MAX_WORLD_X - 5f

    //minimum and maximum x values the camera can have on THIS CURRENT frame
    val currentMinCameraX = canvasSize.width - MAX_WORLD_X * camera.scale
    val currentMaxCameraX = -MIN_WORLD_X * camera.scale

    //variable that stores the offset of the last long press on the Space
    //used for calculating the spawn position of nodes when you create them
    var longPressOffset by remember { mutableStateOf(value = Offset.Zero) }

    val currentOverscrollValue =
        when {
            camera.offset.x < currentMinCameraX -> currentMinCameraX - camera.offset.x
            camera.offset.x > currentMaxCameraX -> camera.offset.x - currentMaxCameraX
            else -> 0f
        }

    val leftBoundaryAlpha by animateFloatAsState(
        targetValue =
            if (isCameraAtLeftBoundary) 0.25f
            else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    val rightBoundaryAlpha by animateFloatAsState(
        targetValue =
            if (isCameraAtRightBoundary) 0.25f
            else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(key1 = pagerState.currentPage) {
        activity?.hideSystemStatusBar()
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
            .pointerInput(key1 = Unit) {
                detectTapGestures(onLongPress = { offset ->
                    longPressOffset = offset
                    coroutineScope.launch {
                        sheetState.show()
                    }.invokeOnCompletion {
                        isNodeSheetVisible = true
                    }
                })
            }
            .pointerInput(key1 = Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown()

                    val hit = categorizeHitAtType(
                        layouts = layouts,
                        pointerPos = firstDown.position
                    )

                    when (hit) {
                        is HitAt.HitNodeResizeHandle -> {
                            interaction = InteractionType.NodeResize(
                                nodeId = hit.layout.node.uuid,
                                selectedHandle = hit.handleHitbox,
                                startPointerOffset = firstDown.position,
                                startNodeWidth = hit.layout.node.width,
                                startNodeHeight = hit.layout.node.height,
                                startNodeOffset = hit.layout.node.offset
                            )
                        }

                        is HitAt.HitNodeBody -> {
                            interaction = InteractionType.NodeDrag(
                                nodeId = hit.layout.node.uuid,
                                startPointerOffset = firstDown.position,
                                startNodeOffset = hit.layout.node.offset
                            )
                            val hitNode = returnHitNodeOrNull(layouts = layouts, pointerPos = firstDown.position)
                            mevm.miplSelectSpaceNode(
                                mapItemUUID = thisSpace.uuid,
                                nodeUUID = hitNode!!.uuid
                            )
                        }

                        is HitAt.HitNodeArrow -> {
                            interaction = InteractionType.NodeArrowDrag(
                                nodeId = hit.layout.node.uuid,
                                startPointerOffset = firstDown.position
                            )
                        }

                        is HitAt.HitCanvas -> {
                            interaction = InteractionType.CanvasLongPress(
                                startPointerOffset = firstDown.position,
                                startTimeMillis = System.currentTimeMillis()
                            )
                            mevm.miplDeselectAllSpaceNodes(mapItemUUID = thisSpace.uuid)
                        }
                    }
                    //todo remove this
                    Log.d("", "interactionType: ${hit.javaClass}")
                }
            }
            .pointerInput(key1 = Unit) {
                //drag -> move the camera around
                //pinch -> change camera zoom
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
                    activity?.hideSystemStatusBar()
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { newSize ->
                    val newSize = Size(
                        width = newSize.width.toFloat(),
                        height = newSize.height.toFloat()
                    )
                    //when the canvas is loaded, update the canvasSize
                    //this setup is needed because canvasSize is needed before the Canvas is loaded
                    canvasSize = newSize

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

            for (layout in layouts) {
                drawNode(
                    layout = layout,
                    camera = camera,
                    selectedNodeBorderColor = selectedNodeBorderColor,
                    arrowUpPainter = nodeArrowUpPainter,
                    arrowDownPainter = nodeArrowDownPainter,
                    arrowLeftPainter = nodeArrowLeftPainter,
                    arrowRightPainter = nodeArrowRightPainter,
                    textMeasurer = textMeasurer
                )
            }
            
            if (leftBoundaryAlpha > 0f) {
                drawLeftBoundaryFade(
                    color = edgeColor,
                    width = fadeWidthPx,
                    alpha = leftBoundaryAlpha,
                    overscroll = currentOverscrollValue
                )

                val arrowX = fadeWidthPx / 2f - arrowSizePx / 2f
                val arrowY = size.height / 2f - arrowSizePx / 2f

                if (pagerState.canScrollBackward) {
                    drawBoundaryArrow(
                        painter = boundaryLeftArrowPainter,
                        alpha = leftBoundaryAlpha,
                        translateLeft = arrowX,
                        translateTop = arrowY,
                        drawSizeWidth = arrowSizePx,
                        drawSizeHeight = arrowSizePx,
                        tintColor = arrowColor
                    )
                }
            }

            if (rightBoundaryAlpha > 0f) {
                drawRightBoundaryFade(
                    color = edgeColor,
                    width = fadeWidthPx,
                    alpha = rightBoundaryAlpha,
                    overscroll = currentOverscrollValue,
                )

                val arrowX = size.width - fadeWidthPx / 2f - arrowSizePx / 2f
                val arrowY = size.height / 2f - arrowSizePx / 2f

                if (pagerState.canScrollForward) {
                    drawBoundaryArrow(
                        painter = boundaryRightArrowPainter,
                        alpha = rightBoundaryAlpha,
                        translateLeft = arrowX,
                        translateTop = arrowY,
                        drawSizeWidth = arrowSizePx,
                        drawSizeHeight = arrowSizePx,
                        tintColor = arrowColor
                    )
                }
            }
        }
        BoundaryHitbox(
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

        BoundaryHitbox(
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
                    activity?.hideSystemStatusBar()
                }
            },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(all = 16.dp)) {
                BottomSheetItem(
                    icon = Icons.Default.LocalActivity,
                    contentDescription = stringResource(id = R.string.contentDescription_map_editor_add_new_node),
                    text = stringResource(id = R.string.map_editor_add_new_node),
                    itemOnClick = {
                        onAddNode(canvasSize, camera, longPressOffset)
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isNodeSheetVisible = false
                            activity?.hideSystemStatusBar()
                        }
                    },
                )

                BottomSheetItem(
                    icon = Icons.Default.AddPhotoAlternate,
                    contentDescription = stringResource(id = R.string.map_editor_add_image),
                    text = stringResource(id = R.string.map_editor_add_image),
                    itemOnClick = {
                        //todo
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            isNodeSheetVisible = false
                            activity?.hideSystemStatusBar()
                        }
                    },
                    includeSpacer = false
                )
            }
        }
    }
}