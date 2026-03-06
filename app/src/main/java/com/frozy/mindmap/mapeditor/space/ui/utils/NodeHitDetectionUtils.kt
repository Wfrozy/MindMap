package com.frozy.mindmap.mapeditor.space.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.frozy.mindmap.mapeditor.model.MapItemObject
import com.frozy.mindmap.mapeditor.model.NodeResizeHandle
import com.frozy.mindmap.mapeditor.model.SpaceCameraState

fun getResizeHandleRects(
    node: MapItemObject.SpaceNode,
    camera: SpaceCameraState
): Map<NodeResizeHandle, Rect> {

    val scaledNodeOffset = (node.offset * camera.scale) + camera.offset
    val scaledNodeWidth = node.width * camera.scale
    val scaledNodeHeight = node.height * camera.scale

    val cornerSize = 45f

    val topLeft = Rect(
        offset = Offset(
            x = scaledNodeOffset.x - (cornerSize / 2.5f),
            y = scaledNodeOffset.y - (cornerSize / 2.5f)
        ),
        size = Size(width = cornerSize, height = cornerSize)
    )

    val topRight = Rect(
        offset = Offset(
            x = (scaledNodeOffset.x + scaledNodeWidth) - (cornerSize / 2.5f),
            y = scaledNodeOffset.y - (cornerSize / 2.5f)
        ),
        size = Size(width = cornerSize, height = cornerSize)
    )

    val bottomLeft = Rect(
        offset = Offset(
            x = scaledNodeOffset.x - (cornerSize / 2.5f),
            y = (scaledNodeOffset.y + scaledNodeHeight) - (cornerSize / 2.5f)
        ),
        size = Size(width = cornerSize, height = cornerSize)
    )

    val bottomRight = Rect(
        offset = Offset(
            x = (scaledNodeOffset.x + scaledNodeWidth) - (cornerSize / 2.5f),
            y = (scaledNodeOffset.y + scaledNodeHeight) - (cornerSize / 2.5f)
        ),
        size = Size(width = cornerSize, height = cornerSize)
    )

    return mapOf(
        NodeResizeHandle.TopLeft to topLeft,
        NodeResizeHandle.TopRight to topRight,
        NodeResizeHandle.BottomLeft to bottomLeft,
        NodeResizeHandle.BottomRight to bottomRight
    )
}

fun detectResizeHandleOrNull(
    tap: Offset,
    node: MapItemObject.SpaceNode,
    camera: SpaceCameraState
): NodeResizeHandle? {

    val handles = getResizeHandleRects(node, camera)

    return handles.entries.firstOrNull { (_, rect) ->
        tap.x in rect.left..rect.right &&
        tap.y in rect.top..rect.bottom
    }?.key
}