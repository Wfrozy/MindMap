package com.frozy.mindmap.mapeditor.space.ui.components

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.R
import com.frozy.mindmap.mapeditor.MapEditorViewModel
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.BOUNDARY_ALPHA_ANIMATION_DURATION_MILLIS
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.BOUNDARY_ARROW_SIZE
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.BOUNDARY_FADE_WIDTH
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.BOUNDARY_TOLERANCE
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.DRAG_THRESHOLD
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.MAX_OVERSCROLL
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.MAX_WORLD_X
import com.frozy.mindmap.mapeditor.space.constants.SpaceValues.MIN_WORLD_X
import com.frozy.mindmap.mapeditor.space.input.HitAt
import com.frozy.mindmap.mapeditor.space.input.categorizeHitAtType
import com.frozy.mindmap.mapeditor.space.input.returnHitNodeOrNull
import com.frozy.mindmap.mapeditor.space.input.returnHitNodeSideOrNull
import com.frozy.mindmap.mapeditor.space.models.SpaceObject
import com.frozy.mindmap.mapeditor.space.node.constants.NodeValues.DEFAULT_NODE_HEIGHT
import com.frozy.mindmap.mapeditor.space.node.constants.NodeValues.DEFAULT_NODE_WIDTH
import com.frozy.mindmap.mapeditor.space.node.imagenode.uriToByteArray
import com.frozy.mindmap.mapeditor.space.node.layout.buildNodeLayout
import com.frozy.mindmap.mapeditor.space.nodelink.PendingNodeLink
import com.frozy.mindmap.mapeditor.space.ui.components.nodecolorpicker.NodeColorPicker
import com.frozy.mindmap.ui.components.BottomSheetItem
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
    pagerState: PagerState,
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val mipl = mevm.mapItemPagerList.collectAsState()

    var editingTextNode by remember { mutableStateOf<SpaceObject.Node.TextNode?>(value = null) }

    val space by remember(key1 = mipl) {
        derivedStateOf {
            mipl.value.firstOrNull { mapItem ->
                mapItem is MapItem.Space && mapItem.uuid == mapItemUUID
            } as? MapItem.Space
        }
    }
    val thisSpace = space ?: return

    val allNodes by remember(key1 = thisSpace) {
        derivedStateOf {
            thisSpace.objectInfo.filterIsInstance<SpaceObject.Node>()
        }
    }

    val allNodeLinks by remember(key1 = thisSpace) {
        derivedStateOf {
            thisSpace.objectInfo.filterIsInstance<SpaceObject.NodeLink>()
        }
    }


    var camera by remember { mutableStateOf(value = thisSpace.cameraState) }

    var pendingNodeLink by remember { mutableStateOf<PendingNodeLink?>(value = null) }
    var selectedNodeLink by remember { mutableStateOf<SpaceObject.NodeLink?>(value = null) }

    val deleteIconPainter = rememberVectorPainter(image = Icons.Default.Close)



    //--- build all layouts at the start of the frame ---//

    val l = allNodes.map { it.buildNodeLayout(camera) }
    val nodeLayoutsState = rememberUpdatedState(newValue = l)

    val layouts by remember(key1 = nodeLayoutsState){
        derivedStateOf{ nodeLayoutsState.value }
    }

    //--- ---//



    val imageBitmapMap: Map<UUID, ImageBitmap> = remember(key1 = allNodes) {
        allNodes.filterIsInstance<SpaceObject.Node.ImageNode>().associate { image ->
            image.uuid to BitmapFactory
                .decodeByteArray(image.bitmapBytes, 0, image.bitmapBytes.size)
                .asImageBitmap()
        }
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
    var snapshotNodeValue by remember { mutableStateOf<SpaceObject.Node?>(value = null) }

    val boundaryLeftArrowPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowBack)
    val boundaryRightArrowPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.ArrowForward)

    //I could just use one arrow and flip it around... but I have other things to do
    val nodeArrowUpPainter = rememberVectorPainter(image = Icons.Default.KeyboardArrowUp)
    val nodeArrowDownPainter = rememberVectorPainter(image = Icons.Default.KeyboardArrowDown)
    val nodeArrowLeftPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.KeyboardArrowLeft)
    val nodeArrowRightPainter = rememberVectorPainter(image = Icons.AutoMirrored.Filled.KeyboardArrowRight)

    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val nodeLinkColor = MaterialTheme.colorScheme.onBackground
    val arrowColor = MaterialTheme.colorScheme.onSurface
    val fallbackSelectedNodeBorderColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    //default SpaceNode parameters when creating a node in a Space
    val defaultNodeWidth = DEFAULT_NODE_WIDTH
    val defaultNodeHeight = DEFAULT_NODE_HEIGHT
    val defaultNodeBorderColor = Color.White
    val defaultNodeBackgroundColor = MaterialTheme.colorScheme.background.lighten(fraction = 0.12f)
    val defaultNodeText = stringResource(id = R.string.default_space_node_text)
    val defaultNodeFontSize = MaterialTheme.typography.bodyMedium.fontSize

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

    //list of colors for the NodeColorPickers
    val predefinedColorPickerBorderColors = listOf(
        MaterialTheme.colorScheme.tertiary,
        Color.White,
        Color(color = 0xFFFFCA28), //yellow
        Color(color = 0xFFEF5350), //red
        Color(color = 0xFF66BB6A), //green
        Color(color = 0xFF42A5F5), //blue
    )

    val predefinedColorPickerBackgroundColors = listOf(
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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult

        coroutineScope.launch {
            val bitmapBytes = uriToByteArray(context, uri) ?: return@launch

            val canvasCenterX = (canvasSize.width/2f - camera.offset.x) / camera.scale
            val canvasCenterY = (canvasSize.height/2f - camera.offset.y) / camera.scale
            val defaultNodeOffset = Offset(
                x = canvasCenterX - defaultNodeWidth/2,
                y = canvasCenterY - defaultNodeHeight/2
            )

            val newImageNode = SpaceObject.Node.ImageNode(
                offset = defaultNodeOffset,
                width = defaultNodeWidth,
                height = defaultNodeHeight,
                borderColor = defaultNodeBorderColor,
                backgroundColor = defaultNodeBackgroundColor,
                bitmapBytes = bitmapBytes
            )

            mevm.addNode(
                spaceUUID = thisSpace.uuid,
                newNode = newImageNode
            )
        }
    }

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

    LaunchedEffect(key1 = camera) {
        mevm.updateSpaceCamera(
            spaceUUID = mapItemUUID,
            camera = camera
        )
        Log.v("", "layouts: $layouts")
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
                        layouts = layouts,
                        nodeLinks = allNodeLinks,
                        selectedNodeLink = selectedNodeLink,
                        pointerPos = firstDown.position
                    )

                    when (hit) {
                        is HitAt.HitNodeResizeHandle -> {
                            selectedNodeLink = null

                            do {
                                val event = awaitPointerEvent()
                                val change =
                                    event.changes.firstOrNull { it.id == firstDown.id } ?: break
                                change.consume()

                                val worldDelta =
                                    (change.position - firstDown.position) / camera.scale

                                mevm.resizeSpaceNode(
                                    spaceUUID = thisSpace.uuid,
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
                            selectedNodeLink = null

                            val isAlreadySelected = hit.layout.node.isSelected

                            if (!isAlreadySelected) {
                                mevm.selectNode(
                                    spaceUUID = thisSpace.uuid,
                                    nodeUUID = hit.layout.node.uuid
                                )
                            }

                            var didDrag = false

                            do {
                                val event = awaitPointerEvent()
                                val change =
                                    event.changes.firstOrNull { it.id == firstDown.id } ?: break
                                change.consume()

                                val totalMoved =
                                    (change.position - firstDown.position).getDistance()
                                if (totalMoved > viewConfiguration.touchSlop) didDrag = true

                                if (didDrag) {
                                    val worldDelta =
                                        (change.position - change.previousPosition) / camera.scale
                                    mevm.moveSpaceNode(
                                        spaceUUID = thisSpace.uuid,
                                        nodeUUID = hit.layout.node.uuid,
                                        delta = worldDelta
                                    )
                                }
                            } while (event.changes.any { it.pressed })

                            //if the finger moved, it means it a drag not a tap so don't open the editor
                            if (didDrag) {
                                editingTextNode = null
                            } else if (isAlreadySelected && hit.layout.node is SpaceObject.Node.TextNode) {
                                editingTextNode = hit.layout.node
                            }
                        }

                        //hit on node arrow
                        is HitAt.HitNodeArrow -> {
                            selectedNodeLink = null
                            var currentPointerPos = firstDown.position

                            do {
                                val event = awaitPointerEvent()
                                val change =
                                    event.changes.firstOrNull { it.id == firstDown.id } ?: break
                                change.consume()
                                currentPointerPos = change.position

                                pendingNodeLink = PendingNodeLink(
                                    fromNodeUUID = hit.layout.node.uuid,
                                    fromNodeSide = hit.sideType,
                                    currentEndPos = currentPointerPos
                                )
                            } while (event.changes.any { it.pressed })

                            val targetNode = returnHitNodeOrNull(
                                layouts = layouts,
                                pointerPos = currentPointerPos
                            )

                            val targetNodeSide = returnHitNodeSideOrNull(
                                layouts = layouts,
                                pointerPos = currentPointerPos
                            )

                            if (
                                targetNode != null &&
                                targetNodeSide != null &&
                                targetNode.uuid != hit.layout.node.uuid
                            ) {
                                mevm.addNodeLink(
                                    spaceUUID = thisSpace.uuid,
                                    fromNodeUUID = hit.layout.node.uuid,
                                    fromNodeSide = hit.sideType,
                                    toNodeUUID = targetNode.uuid,
                                    toNodeSide = targetNodeSide,
                                )
                            }

                            pendingNodeLink = null
                        }

                        is HitAt.HitNodeLink -> {
                            selectedNodeLink = hit.link
                            mevm.deselectAllNodes(
                                spaceUUID = thisSpace.uuid
                            )
                        }

                        is HitAt.HitNodeLinkDeleteButton -> {
                            mevm.deleteNodeLink(
                                spaceUUID = thisSpace.uuid,
                                linkUUID = hit.link.uuid
                            )
                            selectedNodeLink = null
                        }

                        is HitAt.HitCanvas -> {
                            mevm.deselectAllNodes(spaceUUID = thisSpace.uuid)
                            selectedNodeLink = null

                            var prevPositions = mapOf(firstDown.id to firstDown.position)

                            do {
                                val event = awaitPointerEvent()
                                val activePointers = event.changes.filter { it.pressed }

                                //pinch to zoom
                                if (activePointers.size >= 2) {
                                    val pointer1 = activePointers[0]
                                    val pointer2 = activePointers[1]

                                    val pointer1PrevPos = prevPositions[pointer1.id] ?: pointer1.position
                                    val pointer2PrevPos = prevPositions[pointer2.id] ?: pointer2.position

                                    val prevDist = (pointer2PrevPos - pointer1PrevPos).getDistance()
                                    val currDist = (pointer2.position - pointer1.position).getDistance()
                                    val zoom = if (prevDist > 0f) {
                                        currDist / prevDist
                                    } else {
                                        1f
                                    }

                                    val prevCentroid = (pointer1PrevPos + pointer2PrevPos) / 2f
                                    val currCentroid = (pointer1.position + pointer2.position) / 2f
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
                when(layout.node) {
                    is SpaceObject.Node.ImageNode -> {
                        val bitmap = imageBitmapMap[layout.node.uuid] ?: continue
                        drawImageNode(
                            layout = layout,
                            imageBitmap = bitmap,
                            fallbackSelectedBorderColor = fallbackSelectedNodeBorderColor,
                            arrowUpPainter = nodeArrowUpPainter,
                            arrowDownPainter = nodeArrowDownPainter,
                            arrowLeftPainter = nodeArrowLeftPainter,
                            arrowRightPainter = nodeArrowRightPainter,
                        )
                    }

                    is SpaceObject.Node.TextNode -> {
                        drawTextNode(
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
                }
            }

            drawAllNodeLinks(
                links = allNodeLinks,
                layouts = layouts,
                selectedLink = selectedNodeLink,
                color = nodeLinkColor
            )

            selectedNodeLink?.let { link ->
                drawSelectedNodeLinkDeleteButton(
                    link = link,
                    layouts = layouts,
                    painter = deleteIconPainter,
                    tintColor = errorColor
                )
            }

            pendingNodeLink?.let { preview ->
                drawPendingNodeLink(
                    pendingNodeLink = preview,
                    layouts = layouts,
                    arrowColor = nodeLinkColor
                )
            }
            
            if (leftBoundaryAlpha > 0f) {
                drawLeftBoundaryFade(
                    color = nodeLinkColor,
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
                    color = nodeLinkColor,
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
                    text = stringResource(id = R.string.map_editor_new_text_node),
                    itemOnClick = {
                        val canvasCenterX = (canvasSize.width/2f - camera.offset.x) / camera.scale
                        val canvasCenterY = (canvasSize.height/2f - camera.offset.y) / camera.scale
                        val defaultNodeOffset = Offset(
                            x = canvasCenterX - defaultNodeWidth/2,
                            y = canvasCenterY - defaultNodeHeight/2
                        )

                        val newTextNode = SpaceObject.Node.TextNode(
                            offset = defaultNodeOffset,
                            width = defaultNodeWidth,
                            height = defaultNodeHeight,
                            borderColor = defaultNodeBorderColor,
                            backgroundColor = defaultNodeBackgroundColor,
                            text = defaultNodeText,
                            fontSize = defaultNodeFontSize
                        )

                        mevm.addNode(
                            spaceUUID = thisSpace.uuid,
                            newNode = newTextNode
                        )

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
                    contentDescription = stringResource(id = R.string.map_editor_add_image_node),
                    text = stringResource(id = R.string.map_editor_add_image_node),
                    itemOnClick = {
                        coroutineScope.launch {
                            itemAdderSheetState.hide()
                        }.invokeOnCompletion {
                            imagePickerLauncher.launch("image/*")
                            mevm.changeNodeSheetVisibility(value = false)
                            activity?.hideSystemStatusBar()
                        }
                    }
                )
            }
        }
    }


    editingTextNode?.let { node ->
        NodeTextEditDialog(
            initialText = node.text,
            initialFontSize = node.fontSize,
            onConfirm = { newText, newFontSize ->
                mevm.changeNode(
                    spaceUUID = thisSpace.uuid,
                    nodeUUID = node.uuid,
                    newNode = node.copy(
                        text = newText,
                        fontSize = newFontSize
                    ),
                )
                editingTextNode = null
            },
            onDismiss = { editingTextNode = null }
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

            val liveNode = allSelectedNodes.firstOrNull { it.uuid == thisNode.uuid } ?: thisNode

            Column(modifier = Modifier.padding(all = 16.dp)) {
                NodeColorPicker(
                    label = stringResource(id = R.string.node_editor_sheet_border_color_label),
                    selectedColor = liveNode.borderColor,
                    predefinedColors = predefinedColorPickerBorderColors,
                    onColorSelected = { color ->
                        mevm.changeNode(
                            spaceUUID = thisSpace.uuid,
                            nodeUUID = liveNode.uuid,
                            newNode = when(liveNode) {
                                is SpaceObject.Node.TextNode -> {
                                    liveNode.copy(borderColor = color)
                                }
                                is SpaceObject.Node.ImageNode -> {
                                    liveNode.copy(borderColor = color)
                                }
                            }
                        )
                    },
                )

                Spacer(modifier = Modifier.height(height = 8.dp))

                NodeColorPicker(
                    label = stringResource(id = R.string.node_editor_sheet_background_color_label),
                    selectedColor = liveNode.backgroundColor,
                    predefinedColors = predefinedColorPickerBackgroundColors,
                    onColorSelected = { color ->
                        mevm.changeNode(
                            spaceUUID = thisSpace.uuid,
                            nodeUUID = liveNode.uuid,
                            newNode = when(liveNode){
                                is SpaceObject.Node.ImageNode -> {
                                    liveNode.copy(backgroundColor = color)
                                }
                                is SpaceObject.Node.TextNode -> {
                                    liveNode.copy(backgroundColor = color)
                                }
                            }
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                BottomSheetItem(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.contentDescription_delete_selected_nodes_option),
                    text = stringResource(id = R.string.map_editor_delete_selected_nodes),
                    itemOnClick = {
                        if(allSelectedNodeUUIDs.isEmpty()) return@BottomSheetItem

                        mevm.deleteSpaceNodes(
                            spaceUUID = thisSpace.uuid,
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
