package com.frozy.mindmap.mapeditor.space.ui.utils

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.frozy.mindmap.mapeditor.space.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.models.NodeResizeHandles
import com.frozy.mindmap.mapeditor.space.models.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.models.NodeLayout
import com.frozy.mindmap.mapeditor.space.models.NodeArrowHandles
import com.frozy.mindmap.mapeditor.space.constants.NodeArrowHandleValues.ARROW_HANDLE_SPACING_FROM_NODE
import com.frozy.mindmap.mapeditor.space.constants.NodeArrowHandleValues.ARROW_HANDLE_WIDTH_AND_HEIGHT
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_CORNER_RADIUS_X
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_CORNER_RADIUS_Y
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_OUTLINE_WIDTH
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_TEXT_PADDING
import com.frozy.mindmap.mapeditor.space.constants.models.NodeResizeHandleValues.NODE_RESIZE_HANDLE_HEIGHT
import com.frozy.mindmap.mapeditor.space.constants.models.NodeResizeHandleValues.NODE_RESIZE_HANDLE_WIDTH

fun MapItemObject.SpaceNode.buildNodeLayout(
    camera: SpaceCameraState
): NodeLayout {

    val scaledOffset = (this.offset * camera.scale) + camera.offset
    val scaledNodeWidth = this.width * camera.scale
    val scaledNodeHeight = this.height * camera.scale
    val nodeOutlineWidth = NODE_OUTLINE_WIDTH * camera.scale
    val cornerRadius = CornerRadius(
        x = NODE_CORNER_RADIUS_X * camera.scale,
        y = NODE_CORNER_RADIUS_Y * camera.scale
    )
    val textPadding = (NODE_TEXT_PADDING * camera.scale).toInt()

    val nodeHitbox = Rect(
        offset = scaledOffset,
        size = Size(scaledNodeWidth, scaledNodeHeight)
    )

    val handleWidth = NODE_RESIZE_HANDLE_WIDTH
    val handleHeight = NODE_RESIZE_HANDLE_HEIGHT
    val nodeTopLeft = nodeHitbox.topLeft

    val resizeHandles = NodeResizeHandles(
        topLeft = Rect(
            Offset(
                x = nodeTopLeft.x - handleWidth/2.5f,
                y = nodeTopLeft.y - handleHeight/2.5f
            ),
            Size(width = handleWidth, height = handleHeight)
        ),

        topRight = Rect(
            Offset(
                x = nodeHitbox.right - handleWidth/2.5f,
                y = nodeHitbox.top - handleHeight/2.5f
            ),
            Size(width = handleWidth, height = handleHeight)
        ),

        bottomLeft = Rect(
            Offset(
                x = nodeHitbox.left - handleWidth/2.5f,
                y = nodeHitbox.bottom - handleHeight/2.5f
            ),
            Size(width = handleWidth, height = handleHeight)
        ),

        bottomRight = Rect(
            Offset(
                x = nodeHitbox.right - handleWidth/2.5f,
                y = nodeHitbox.bottom - handleHeight/2.5f
            ),
            Size(width = handleWidth, height = handleHeight)
        )
    )

    val centerX = nodeTopLeft.x + nodeHitbox.width/2
    val centerY = nodeTopLeft.y + nodeHitbox.height/2

    val arrowSize = ARROW_HANDLE_WIDTH_AND_HEIGHT
    val arrowSpacing = ARROW_HANDLE_SPACING_FROM_NODE

    val arrowHandles = NodeArrowHandles(
        top = Rect(
            offset = Offset(
                x = centerX - arrowSize,
                y = nodeHitbox.top - arrowSpacing*2 - arrowSize*2
            ),
            Size(width = arrowSize*2, height = arrowSize*2)
        ),

        bottom = Rect(
            Offset(
                x = centerX - arrowSize,
                y = nodeHitbox.bottom + arrowSpacing*2
            ),
            Size(width = arrowSize*2, height = arrowSize*2)
        ),

        left = Rect(
            Offset(
                x = nodeHitbox.left - arrowSpacing*2 - arrowSize*2,
                y = centerY - arrowSize
            ),
            Size(width = arrowSize*2, height = arrowSize*2)
        ),

        right = Rect(
            Offset(
                x = nodeHitbox.right + arrowSpacing*2,
                y = centerY - arrowSize
            ),
            Size(width = arrowSize*2,  height = arrowSize*2)
        )
    )
    return NodeLayout(
        node = this,
        nodeHitbox,
        resizeHandles,
        arrowHandles,
        nodeOutlineWidth,
        cornerRadius,
        textPadding
    )
}

fun MapItemObject.SpaceNode.getResizeHandleRects(
    camera: SpaceCameraState
): NodeResizeHandles {

    val scaledNodeOffset = (this.offset * camera.scale) + camera.offset
    val scaledNodeWidth = this.width * camera.scale
    val scaledNodeHeight = this.height * camera.scale

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

    return NodeResizeHandles(topLeft, topRight, bottomLeft, bottomRight)
}

fun NodeLayout.getActiveResizeHandleOrNull(
    tap: Offset
): Rect? {

    val topLeft = this.resizeHandles.topLeft
    val topRight = this.resizeHandles.topRight
    val bottomLeft = this.resizeHandles.bottomLeft
    val bottomRight = this.resizeHandles.bottomRight

    //there's probably a better way to do this but at least it is readable 😊👍
    if(tap.x in topLeft.right..topLeft.left &&
        tap.y in topLeft.top..topLeft.bottom) {
        return topLeft
    }

    if(tap.x in topRight.right..topRight.left &&
       tap.y in topRight.top..topRight.bottom) {
        return topRight
    }

    if(tap.x in bottomLeft.right..bottomLeft.left &&
        tap.y in bottomLeft.top..bottomLeft.bottom) {
        return bottomLeft
    }

    if(tap.x in bottomRight.right..bottomRight.left &&
        tap.y in bottomRight.top..bottomRight.bottom) {
        return bottomRight
    }

    return null
}