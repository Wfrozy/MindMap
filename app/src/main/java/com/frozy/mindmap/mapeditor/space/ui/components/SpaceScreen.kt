package com.frozy.mindmap.mapeditor.space.ui.components

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.R
import com.frozy.mindmap.mapeditor.MapEditorViewModel
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.BOUNDARY_ALPHA_ANIMATION_DURATION_MILLIS
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.BOUNDARY_ARROW_SIZE
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.BOUNDARY_FADE_WIDTH
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.BOUNDARY_TOLERANCE
import com.frozy.mindmap.mapeditor.space.models.HitAt
import com.frozy.mindmap.mapeditor.space.models.MapItem
import com.frozy.mindmap.mapeditor.space.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.models.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.DRAG_THRESHOLD
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.MAX_OVERSCROLL
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.MAX_WORLD_X
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.MIN_WORLD_X
import com.frozy.mindmap.mapeditor.space.models.ArrowDragPreview
import com.frozy.mindmap.mapeditor.space.ui.utils.buildNodeLayout
import com.frozy.mindmap.mapeditor.space.ui.utils.categorizeHitAtType
import com.frozy.mindmap.mapeditor.space.ui.utils.returnHitNodeOrNull
import com.frozy.mindmap.ui.components.BottomSheetItem
import com.frozy.mindmap.ui.components.NodeTextEditDialog
import com.frozy.mindmap.ui.components.nodecolorpicker.NodeColorPicker
import com.frozy.mindmap.ui.utils.hideSystemStatusBar
import com.frozy.mindmap.ui.utils.lighten
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

    var editingNode by remember { mutableStateOf<MapItemObject.SpaceNode?>(value = null) }

    val thisSpace by remember(key1 = mipl) {
        derivedStateOf {
            mipl.value.first { mapItem ->
                mapItem is MapItem.Space && mapItem.uuid == mapItemUUID
            } as MapItem.Space
        }
    }

    val nodes by remember(key1 = thisSpace) {
        derivedStateOf {
            thisSpace.spaceNodeInfo
        }
    }


    var camera by remember { mutableStateOf(value = thisSpace.cameraState) }
    var arrowDragPreview by remember { mutableStateOf<ArrowDragPreview?>(value = null) }

    //build all layouts at the start of the frame
    //rememberUpdatedState is for updating the value of layouts that .pointerInput() receives
    val l = nodes.map { node -> return@map node.buildNodeLayout(camera) }

    val layoutsState = rememberUpdatedState(newValue = l)

    val layouts by remember(key1 = layoutsState){
        derivedStateOf{ layoutsState.value }
    }

    //starts at 0 but then gets the value once a Canvas gets drawn
    var canvasSize by remember { mutableStateOf(value = Size.Zero) }

    val itemAdderSheetState = rememberModalBottomSheetState()
    val nodeEditorSheetState = rememberModalBottomSheetState()

    val isItemAdderSheetVisible by mevm.isItemAdderSheetVisible.collectAsState()
    val isNodeEditorSheetVisible by mevm.isNodeEditorSheetVisible.collectAsState()
    val allSelectedNodes by mevm.allSelectedNodes.collectAsState()
    val allSelectedNodeUUIDs by remember {
        derivedStateOf {
            allSelectedNodes.map { it.uuid }
        }
    }

    //when the bottom sheet with the node editing stuff becomes visible, it uses this variable to
    // snapshot it to avoid a NullPointerException
    var snapshotNodeValue by remember { mutableStateOf<MapItemObject.SpaceNode?>(value = null) }

    val boundaryLeftArrowPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack)
    val boundaryRightArrowPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowForward)

    //I could just use one arrow and flip it around... but I have other things to do
    val nodeArrowUpPainter = rememberVectorPainter(image = Icons.Default.KeyboardArrowUp)
    val nodeArrowDownPainter = rememberVectorPainter(image = Icons.Default.KeyboardArrowDown)
    val nodeArrowLeftPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.KeyboardArrowLeft)
    val nodeArrowRightPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.KeyboardArrowRight)

    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val edgeColor = MaterialTheme.colorScheme.onBackground
    val arrowColor = MaterialTheme.colorScheme.onSurface
    val fallbackSelectedNodeBorderColor = MaterialTheme.colorScheme.tertiary

    val textMeasurer = rememberTextMeasurer()

    val leftEdgeCam = -camera.offset.x / camera.scale
    val rightEdgeCam = leftEdgeCam + canvasSize.width / camera.scale

    val fadeWidth = BOUNDARY_FADE_WIDTH
    val boundaryArrowSize = BOUNDARY_ARROW_SIZE

    val isCameraAtLeftBoundary = leftEdgeCam <= MIN_WORLD_X + BOUNDARY_TOLERANCE
    val isCameraAtRightBoundary = rightEdgeCam >= MAX_WORLD_X - BOUNDARY_TOLERANCE

    //minimum and maximum x values the camera can have on THIS CURRENT frame
    val currentMinCameraX = canvasSize.width - MAX_WORLD_X * camera.scale
    val currentMaxCameraX = -MIN_WORLD_X * camera.scale

    //variable that stores the offset of the last long press on the Space
    //used for calculating the spawn position of nodes when you create them
    var longPressOffset by remember { mutableStateOf(value = Offset.Zero) }

    //list of colors for the NodeColorPickers
    val predefinedBorderColors = listOf(
        MaterialTheme.colorScheme.tertiary,
        Color.White,
        Color(color = 0xFFFFCA28), //yellow
        Color(color = 0xFFEF5350), //red
        Color(color = 0xFF66BB6A), //green
        Color(color = 0xFF42A5F5), //blue
    )

    val predefinedBackgroundColors = listOf(
        MaterialTheme.colorScheme.background.lighten(fraction = 0.12f),
        Color.White,
        Color(color = 0xFFFFCA28), //yellow
        Color(color = 0xFFEF5350), //red
        Color(color = 0xFF66BB6A), //green
        Color(color = 0xFF42A5F5), //blue
    )

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
        animationSpec = tween(
            durationMillis = BOUNDARY_ALPHA_ANIMATION_DURATION_MILLIS
        )
    )

    val rightBoundaryAlpha by animateFloatAsState(
        targetValue =
            if (isCameraAtRightBoundary) 0.25f
            else 0f,
        animationSpec = tween(
            durationMillis = BOUNDARY_ALPHA_ANIMATION_DURATION_MILLIS
        )
    )

    //avoids race conditions with the composable being toggled with if() and .show() animation
    LaunchedEffect(key1 = isItemAdderSheetVisible) {
        if (isItemAdderSheetVisible) {
            itemAdderSheetState.show()
        }
    }

    //avoids race conditions with the composable being toggled with if() and .show() animation
    LaunchedEffect(key1 = isNodeEditorSheetVisible) {
        if (isNodeEditorSheetVisible) {
            nodeEditorSheetState.show()
        }
    }

    LaunchedEffect(key1 = isNodeEditorSheetVisible) {
        if (isNodeEditorSheetVisible) {
            snapshotNodeValue = allSelectedNodes.firstOrNull()
        }
    }


//    LaunchedEffect(key1 = pagerState.currentPage) {
//        activity?.hideSystemStatusBar()
//        if (canvasSize != Size.Zero) {
//            camera = camera.copy(
//                offset = Offset(
//                    x = canvasSize.width / 2f,
//                    y = canvasSize.height / 2f
//                )
//            )
//        }
//    }

    LaunchedEffect(key1 = camera) {
        mevm.miplUpdateSpaceCamera(
            mapItemUUID = mapItemUUID,
            camera = camera
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(key1 = Unit) {
                awaitEachGesture {
                    //use firstDown for initial gesture stuff
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    firstDown.consume()

                    val hit = categorizeHitAtType(
                        layouts = layoutsState.value,
                        pointerPos = firstDown.position
                    )

                    when (hit) {
                        is HitAt.HitNodeResizeHandle -> {
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == firstDown.id } ?: break
                                change.consume()

                                val worldDelta = (change.position - firstDown.position) / camera.scale

                                mevm.miplResizeSpaceNode(
                                    mapItemUUID = thisSpace.uuid,
                                    nodeUUID = hit.layout.node.uuid,
                                    handleType = hit.handleType,
                                    worldDragDelta = worldDelta,
                                    startNodeWidth = hit.layout.node.width,
                                    startNodeHeight = hit.layout.node.height,
                                    startNodeOffset = hit.layout.node.offset
                                )
                            } while (event.changes.any { it.pressed })
                        }

                        is HitAt.HitNodeBody -> {
                            val isAlreadySelected = hit.layout.node.isSelected

                            if (isAlreadySelected) {
                                editingNode = hit.layout.node
                            } else {
                                mevm.miplSelectSpaceNode(
                                    mapItemUUID = thisSpace.uuid,
                                    nodeUUID    = hit.layout.node.uuid
                                )
                            }

                            var didDrag = false

                            do {
                                val event  = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == firstDown.id } ?: break
                                change.consume()

                                val totalMoved = (change.position - firstDown.position).getDistance()
                                if (totalMoved > viewConfiguration.touchSlop) didDrag = true

                                if (didDrag) {
                                    val worldDelta = (change.position - change.previousPosition) / camera.scale
                                    mevm.miplMoveSpaceNode(
                                        mapItemUUID = thisSpace.uuid,
                                        nodeUUID = hit.layout.node.uuid,
                                        delta = worldDelta
                                    )
                                }
                            } while (event.changes.any { it.pressed })

                            //if the finger moved, it was a drag not a tap so don't open the editor
                            if (didDrag) editingNode = null
                        }

                        //hit on node arrow
                        is HitAt.HitNodeArrow -> {
                            var currentPointerPos = firstDown.position

                            do {
                                val event  = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == firstDown.id } ?: break
                                change.consume()
                                currentPointerPos = change.position

                                arrowDragPreview = ArrowDragPreview(
                                    fromNodeUUID = hit.layout.node.uuid,
                                    currentScreenPos = currentPointerPos
                                )
                            } while (event.changes.any { it.pressed })

                            // check if released over a different node
                            val targetNode = returnHitNodeOrNull(
                                layouts    = layoutsState.value,
                                pointerPos = currentPointerPos
                            )
                            if (targetNode != null && targetNode.uuid != hit.layout.node.uuid) {
                                mevm.miplCreateEdge(
                                    mapItemUUID  = thisSpace.uuid,
                                    fromNodeUUID = hit.layout.node.uuid,
                                    toNodeUUID   = targetNode.uuid
                                )
                            }

                            arrowDragPreview = null
                        }

                        //hit on canvas
                        is HitAt.HitCanvas -> {
                            mevm.miplDeselectAllSpaceNodes(mapItemUUID = thisSpace.uuid)

                            var prevPositions = mapOf(firstDown.id to firstDown.position)

                            do {
                                val event = awaitPointerEvent()
                                val activePointers = event.changes.filter { it.pressed }

                                //pinch to zoom
                                if (activePointers.size >= 2) {
                                    val p0 = activePointers[0]
                                    val p1 = activePointers[1]

                                    val prevC0 = prevPositions[p0.id] ?: p0.position
                                    val prevC1 = prevPositions[p1.id] ?: p1.position

                                    val prevDist = (prevC1 - prevC0).getDistance()
                                    val currDist = (p1.position - p0.position).getDistance()
                                    val zoom = if (prevDist > 0f) {
                                        currDist / prevDist
                                    } else {
                                        1f
                                    }

                                    val prevCentroid = (prevC0 + prevC1) / 2f
                                    val currCentroid = (p0.position + p1.position) / 2f
                                    val pan = currCentroid - prevCentroid

                                    val newScale = (camera.scale * zoom).coerceIn(0.4f, 3f)
                                    val scaleChange = newScale / camera.scale
                                    val rawOffset = (camera.offset + pan) +
                                            (camera.offset - currCentroid) * (scaleChange - 1f)

                                    val minX = canvasSize.width - MAX_WORLD_X * newScale
                                    val maxX = -MIN_WORLD_X * newScale

                                    camera = camera.copy(
                                        offset = Offset(
                                            x = rawOffset.x.coerceIn(minX, maxX),
                                            y = rawOffset.y
                                        ),
                                        scale = newScale
                                    )
                                    activePointers.forEach { it.consume() }

                                //pan to move around
                                } else if (activePointers.size == 1) {
                                    val change = activePointers[0]

                                    if ((change.position - firstDown.position).getDistance() > viewConfiguration.touchSlop) {
                                        val delta = change.position - change.previousPosition
                                        val minX = canvasSize.width - MAX_WORLD_X * camera.scale
                                        val maxX = -MIN_WORLD_X * camera.scale

                                        camera = camera.copy(
                                            offset = Offset(
                                                x = (camera.offset.x + delta.x).coerceIn(
                                                    minX,
                                                    maxX
                                                ),
                                                y = camera.offset.y + delta.y
                                            )
                                        )
                                        activity?.hideSystemStatusBar()
                                    }
                                    change.consume()
                                }

                                prevPositions = activePointers.associate { it.id to it.position }

                            } while (event.changes.any { it.pressed })
                        }
                    }
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
            val boundaryArrowSizePx = boundaryArrowSize.dp.toPx()

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
                    fallbackSelectedBorderColor = fallbackSelectedNodeBorderColor,
                    arrowUpPainter = nodeArrowUpPainter,
                    arrowDownPainter = nodeArrowDownPainter,
                    arrowLeftPainter = nodeArrowLeftPainter,
                    arrowRightPainter = nodeArrowRightPainter,
                    textMeasurer = textMeasurer
                )
            }

            drawEdges(
                edges = thisSpace.edges,
                layouts = layouts,
                edgeColor = edgeColor
            )

            arrowDragPreview?.let { preview ->
                drawArrowDragPreview(
                    preview = preview,
                    layouts = layouts,
                    edgeColor = edgeColor
                )
            }
            
            if (leftBoundaryAlpha > 0f) {
                drawLeftBoundaryFade(
                    color = edgeColor,
                    width = fadeWidthPx,
                    alpha = leftBoundaryAlpha,
                    overscroll = currentOverscrollValue
                )

                val arrowX = fadeWidthPx / 2f - boundaryArrowSizePx / 2f
                val arrowY = size.height / 2f - boundaryArrowSizePx / 2f

                if (pagerState.canScrollBackward) {
                    drawBoundaryArrow(
                        painter = boundaryLeftArrowPainter,
                        alpha = leftBoundaryAlpha,
                        translateLeft = arrowX,
                        translateTop = arrowY,
                        drawSizeWidth = boundaryArrowSizePx,
                        drawSizeHeight = boundaryArrowSizePx,
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

                val arrowX = size.width - fadeWidthPx / 2f - boundaryArrowSizePx / 2f
                val arrowY = size.height / 2f - boundaryArrowSizePx / 2f

                if (pagerState.canScrollForward) {
                    drawBoundaryArrow(
                        painter = boundaryRightArrowPainter,
                        alpha = rightBoundaryAlpha,
                        translateLeft = arrowX,
                        translateTop = arrowY,
                        drawSizeWidth = boundaryArrowSizePx,
                        drawSizeHeight = boundaryArrowSizePx,
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

    if(isItemAdderSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    itemAdderSheetState.hide()
                }.invokeOnCompletion {
                    mevm.changeNodeSheetVisibility(value = false)
                    activity?.hideSystemStatusBar()
                }
            },
            sheetState = itemAdderSheetState
        ) {
            Column(modifier = Modifier.padding(all = 16.dp)) {
                BottomSheetItem(
                    icon = Icons.Default.LocalActivity,
                    contentDescription = stringResource(id = R.string.contentDescription_map_editor_add_new_node),
                    text = stringResource(id = R.string.map_editor_add_new_node),
                    itemOnClick = {
                        onAddNode(canvasSize, camera, longPressOffset)
                        coroutineScope.launch {
                            itemAdderSheetState.hide()
                        }.invokeOnCompletion {
                            mevm.changeNodeSheetVisibility(value = false)
                            activity?.hideSystemStatusBar()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(height = 8.dp))

                BottomSheetItem(
                    icon = Icons.Default.AddPhotoAlternate,
                    contentDescription = stringResource(id = R.string.map_editor_add_image),
                    text = stringResource(id = R.string.map_editor_add_image),
                    itemOnClick = {
                        coroutineScope.launch {
                            itemAdderSheetState.hide()
                        }.invokeOnCompletion {
                            mevm.changeNodeSheetVisibility(value = false)
                            activity?.hideSystemStatusBar()
                        }
                    }
                )
            }
        }
    }


    editingNode?.let { node ->
        NodeTextEditDialog(
            initialText = node.text,
            onConfirm = { newText ->
                mevm.miplChangeSpaceNode(
                    mapItemUUID = thisSpace.uuid,
                    nodeUUID    = node.uuid,
                    newNode     = node.copy(text = newText)
                )
                editingNode = null
            },
            onDismiss = { editingNode = null }
        )
    }

    //bottom sheet that contains options for changing node stuff
    if(isNodeEditorSheetVisible){
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    nodeEditorSheetState.hide()
                }.invokeOnCompletion {
                    mevm.changeNodeEditorSheetVisibility(value = false)
                    activity?.hideSystemStatusBar()
                }
            },
            sheetState = nodeEditorSheetState,
            scrimColor = Color.Transparent
        ) {
            //this in practice will never be null
            val thisNode = snapshotNodeValue ?: return@ModalBottomSheet

            Column(modifier = Modifier.padding(all = 16.dp)) {
                NodeColorPicker(
                    label = stringResource(id = R.string.node_editor_sheet_border_color_label),
                    selectedColor = thisNode.borderColor,
                    predefinedColors = predefinedBorderColors,
                    onColorSelected = { color ->
                        mevm.miplChangeSpaceNode(
                            mapItemUUID = thisSpace.uuid,
                            nodeUUID = thisNode.uuid,
                            newNode = thisNode.copy(borderColor = color)
                        )
                    },
                )

                Spacer(modifier = Modifier.height(height = 8.dp))

                NodeColorPicker(
                    label = stringResource(id = R.string.node_editor_sheet_background_color_label),
                    selectedColor = thisNode.backgroundColor,
                    predefinedColors = predefinedBackgroundColors,
                    onColorSelected = { color ->
                        mevm.miplChangeSpaceNode(
                            mapItemUUID = thisSpace.uuid,
                            nodeUUID = thisNode.uuid,
                            newNode = thisNode.copy(backgroundColor = color)
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                BottomSheetItem(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.contentDescription_delete_selected_nodes_option),
                    text = stringResource(id = R.string.delete_selected_nodes_label),
                    itemOnClick = {
                        if(allSelectedNodeUUIDs.isEmpty()){
                            return@BottomSheetItem
                        }
                        mevm.miplRemoveSpaceNodesFromSpace(
                            mapItemUUID = thisSpace.uuid,
                            *allSelectedNodeUUIDs.toTypedArray()
                        )
                        coroutineScope.launch {
                            nodeEditorSheetState.hide()
                        }.invokeOnCompletion {
                            mevm.changeNodeEditorSheetVisibility(value = false)
                            activity?.hideSystemStatusBar()
                        }
                    },
                    //if somehow this menu gets accessed with no selected nodes,
                    // you still can't press delete
                    isClickable = allSelectedNodes.isNotEmpty()
                )
            }
        }
    }
}
