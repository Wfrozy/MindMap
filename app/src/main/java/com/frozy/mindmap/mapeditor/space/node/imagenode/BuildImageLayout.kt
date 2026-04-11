package com.frozy.mindmap.mapeditor.space.node.imagenode

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.frozy.mindmap.mapeditor.space.models.SpaceObject
import com.frozy.mindmap.mapeditor.space.camera.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_CORNER_RADIUS_X
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_CORNER_RADIUS_Y
import com.frozy.mindmap.mapeditor.space.constants.models.NodeLayoutValues.NODE_OUTLINE_WIDTH
import com.frozy.mindmap.mapeditor.space.constants.models.NodeResizeHandleValues.NODE_RESIZE_HANDLE_HEIGHT
import com.frozy.mindmap.mapeditor.space.constants.models.NodeResizeHandleValues.NODE_RESIZE_HANDLE_WIDTH
import com.frozy.mindmap.mapeditor.space.node.resizehandle.NodeResizeHandles

//fun SpaceObject.ImageNode.buildImageLayout(
//    camera: SpaceCameraState
//): ImageLayout {
//    val scaledOffset = (this.offset * camera.scale) + camera.offset
//    val scaledImageWidth = this.width * camera.scale
//    val scaledImageHeight = this.height * camera.scale
//    val outlineWidth = NODE_OUTLINE_WIDTH * camera.scale
//    val cornerRadius = CornerRadius(
//        x = NODE_CORNER_RADIUS_X * camera.scale,
//        y = NODE_CORNER_RADIUS_Y * camera.scale
//    )
//
//    val imageHitbox = Rect(
//        offset = scaledOffset,
//        size   = Size(scaledImageWidth, scaledImageHeight)
//    )
//
//    val handleWidth  = NODE_RESIZE_HANDLE_WIDTH
//    val handleHeight = NODE_RESIZE_HANDLE_HEIGHT
//    val imageTopLeft = imageHitbox.topLeft
//
//    val resizeHandles = NodeResizeHandles(
//        topLeft = Rect(
//            Offset(
//                x = imageTopLeft.x - handleWidth / 2.5f,
//                y = imageTopLeft.y - handleHeight / 2.5f
//            ),
//            Size(width = handleWidth, height = handleHeight)
//        ),
//        topRight = Rect(
//            Offset(
//                x = imageHitbox.right - handleWidth / 2.5f,
//                y = imageHitbox.top - handleHeight / 2.5f
//            ),
//            Size(width = handleWidth, height = handleHeight)
//        ),
//        bottomLeft = Rect(
//            Offset(
//                x = imageHitbox.left - handleWidth / 2.5f,
//                y = imageHitbox.bottom - handleHeight / 2.5f
//            ),
//            Size(width = handleWidth, height = handleHeight)
//        ),
//        bottomRight = Rect(
//            Offset(
//                x = imageHitbox.right - handleWidth / 2.5f,
//                y = imageHitbox.bottom - handleHeight / 2.5f
//            ),
//            Size(width = handleWidth, height = handleHeight)
//        )
//    )
//
//    return ImageLayout(
//        imageNode = this,
//        imageHitbox = imageHitbox,
//        resizeHandles = resizeHandles,
//        outlineWidth = outlineWidth,
//        cornerRadius = cornerRadius
//    )
//}

