package com.frozy.mindmap.mapeditor.space.node.layout

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.frozy.mindmap.mapeditor.space.camera.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_CORNER_RADIUS_X
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_CORNER_RADIUS_Y
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_OUTLINE_WIDTH
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_TEXT_PADDING
import com.frozy.mindmap.mapeditor.space.constants.models.NodeResizeHandleValues.NODE_RESIZE_HANDLE_HEIGHT
import com.frozy.mindmap.mapeditor.space.constants.models.NodeResizeHandleValues.NODE_RESIZE_HANDLE_WIDTH
import com.frozy.mindmap.mapeditor.space.models.SpaceObject
import com.frozy.mindmap.mapeditor.space.node.arrowhandle.NodeArrowHandleValues.ARROW_HANDLE_SPACING_FROM_NODE
import com.frozy.mindmap.mapeditor.space.node.arrowhandle.NodeArrowHandleValues.ARROW_HANDLE_WIDTH_AND_HEIGHT
import com.frozy.mindmap.mapeditor.space.node.arrowhandle.NodeArrowHandles
import com.frozy.mindmap.mapeditor.space.node.resizehandle.NodeResizeHandles

fun SpaceObject.Node.buildNodeLayout(
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
    val textPadding = when(this) {
        is SpaceObject.Node.TextNode -> (NODE_TEXT_PADDING * camera.scale).toInt()
        is SpaceObject.Node.ImageNode -> null
    }

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