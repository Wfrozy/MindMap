package com.frozy.mindmap.mapeditor.space.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import java.util.UUID

/**
 * Does not include camera pans and zooming. Those actions are handled by detectTransformGestures.
 **/
sealed class InteractionType {

    data object Idle : InteractionType()

    data class NodeDrag(
        val nodeId: UUID,
        val startPointerOffset: Offset,
        val startNodeOffset: Offset
    ) : InteractionType()

    data class NodeResize(
        val nodeId: UUID,
        val selectedHandle: Rect,
        val startHandleOffset: Offset,
        val startPointerOffset: Offset,
        val startNodeWidth: Float,
        val startNodeHeight: Float,
        val startNodeOffset: Offset
    ) : InteractionType()

    data class NodeArrowDrag(
        val nodeId: UUID,
        val startPointerOffset: Offset
    ) : InteractionType()

    data class CanvasLongPress(
        val startPointerOffset: Offset,
        val startTimeMillis: Long
    ) : InteractionType()

    data class CanvasTransform(
        val startCameraOffset: Offset,
        val startCameraScale: Float
    ) : InteractionType()
}


//.pointerInput(key1 = nodes, key2 = camera) {
//    detectDragGestures(
//        onDragStart = { pointer -> //todo rewrite pointer stuff
//
//            //tests to see which node is active
//            val activeNode = nodes.lastOrNull { node ->
//                return@lastOrNull getActiveResizeHandleOrNull(
//                    tap = pointer,
//                    layout = layout
//                ) != null
//            }
//            Log.d("", "activeNode: $activeNode")
//
//            if(activeNode != null) {
//                val activeHandle = getActiveResizeHandleOrNull(
//                    tap = pointer,
//                    node = activeNode
//                )!! //double exclam to get the compiler to shut up
//                Log.d("", "activeNode: $activeHandle")
//
//                resizeState = ResizeState(
//                    nodeId = activeNode.uuid,
//                    resizingOn = activeHandle,
//                    startPointer = pointer,
//                    startWidth = activeNode.width,
//                    startHeight = activeNode.height,
//                    startOffset = activeNode.offset
//                )
//            }
//        },
//        onDrag = { change, _ ->
//
//            val state = resizeState ?: return@detectDragGestures
//
//            val nodeIndex = nodes.indexOfFirst { it.uuid == state.nodeId }
//            val node = nodes[nodeIndex]
//
//            val dx = (change.position.x - state.startPointer.x) / camera.scale
//            val dy = (change.position.y - state.startPointer.y) / camera.scale
//
//            val newNode = when (state.resizingOn) {
//
//                NodeResizeHandle.BOTTOM_RIGHT -> {
//                    node.copy(
//                        width = state.startWidth + dx,
//                        height = state.startHeight + dy
//                    )
//                }
//
//                NodeResizeHandle.BOTTOM_LEFT -> {
//                    node.copy(
//                        width = state.startWidth - dx,
//                        height = state.startHeight + dy,
//                        offset = Offset(
//                            x = state.startOffset.x + dx,
//                            y = state.startOffset.y
//                        )
//                    )
//                }
//
//                NodeResizeHandle.TOP_RIGHT -> {
//                    node.copy(
//                        width = state.startWidth + dx,
//                        height = state.startHeight - dy,
//                        offset = Offset(
//                            x = state.startOffset.x,
//                            y = state.startOffset.y + dy
//                        )
//                    )
//                }
//
//                NodeResizeHandle.TOP_LEFT -> {
//                    node.copy(
//                        width = state.startWidth - dx,
//                        height = state.startHeight - dy,
//                        offset = Offset(
//                            x = state.startOffset.x + dx,
//                            y = state.startOffset.y + dy
//                        )
//                    )
//                }
//            }
//
//            mevm.miplChangeSpaceNode(
//                mapItemUUID = thisSpace.uuid,
//                nodeUUID = nodes[nodeIndex].uuid,
//                newNode = newNode
//            )
//        },
//        onDragEnd = {
//            resizeState = null
//        }
//    )
//}
//.pointerInput(key1 = Unit) {
//    detectTapGestures(
//        //long press -> open up bottom sheet
//        onLongPress = { offset ->
//            longPressOffset = offset
//            coroutineScope.launch {
//                sheetState.show()
//            }.invokeOnCompletion {
//                isNodeSheetVisible = true
//            }
//        },
//        //tap -> tap is on a node -> interact with node
//        onTap = { tapOffset ->
//
//            val tapWorldPos = (tapOffset - camera.offset) / camera.scale
//
//            //checks to see if the tap coordinates are within the coordinate space of a node
//            val nodeHitOrNull = nodes.firstOrNull { node ->
//                return@firstOrNull tapWorldPos.x in node.offset.x..(node.offset.x + node.width) &&
//                        tapWorldPos.y in node.offset.y..(node.offset.y + node.height)
//            }
//
//
//            if (nodeHitOrNull != null) {
//                onNodeHit(nodeHitOrNull, nodeHitOrNull.uuid)
//            } else {
//                mevm.miplDeselectAllSpaceNodes(
//                    mapItemUUID = thisSpace.uuid
//                )
//                resizeState = null
//            }
//        }
//    )
//}