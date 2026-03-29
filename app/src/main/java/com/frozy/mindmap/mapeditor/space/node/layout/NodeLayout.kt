package com.frozy.mindmap.mapeditor.space.node.layout

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import com.frozy.mindmap.mapeditor.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.node.arrowhandle.NodeArrowHandles
import com.frozy.mindmap.mapeditor.space.node.resizehandle.NodeResizeHandles

/**
 * NodeLayout is a class that caches geometry calculations.
 *
 * NodeLayout objects are derived data: they are rebuilt whenever nodes or the camera
 * change and should not be persisted as part of the node's data model.
 */

data class NodeLayout(
    val node: MapItemObject.SpaceNode,
    val nodeHitbox: Rect,
    val resizeHandles: NodeResizeHandles,
    val arrowHandles: NodeArrowHandles,
    val nodeOutlineWidth: Float,
    val cornerRadius: CornerRadius,
    val textPadding: Int
)
