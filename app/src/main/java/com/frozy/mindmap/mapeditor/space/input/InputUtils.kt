package com.frozy.mindmap.mapeditor.space.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.frozy.mindmap.mapeditor.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.node.layout.NodeLayout
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType
import com.frozy.mindmap.mapeditor.space.nodelink.NodeLinkValues.NODE_LINK_DELETE_BUTTON_RADIUS
import com.frozy.mindmap.mapeditor.space.nodelink.NodeLinkValues.NODE_LINK_HITBOX_TOLERANCE
import kotlin.collections.asReversed

fun categorizeHitAtType(
    layouts: List<NodeLayout>,
    nodeLinks: List<MapItemObject.SpaceNodeLink>,
    selectedNodeLink: MapItemObject.SpaceNodeLink?,
    pointerPos: Offset
): HitAt {
    val deleteButtonRadius = NODE_LINK_DELETE_BUTTON_RADIUS

    //asReversed() is necessary so that nodes at the top are the ones that get detected first
    for (layout in layouts.asReversed()) {

        if (layout.nodeHitbox.contains(pointerPos)) {
            return HitAt.HitNodeBody(layout)
        }

        //if node no nodes are selected, then no handles will be present, so just skip!
        if(!layout.node.isSelected){ continue }

        layout.resizeHandles.entries.forEach { (handleHitbox, handleType) ->
            if (handleHitbox.contains(pointerPos)) {
                return HitAt.HitNodeResizeHandle(
                    layout = layout,
                    handleHitbox = handleHitbox,
                    handleType = handleType
                )
            }
        }

        layout.arrowHandles.entries.forEach { (arrowHitbox, arrowType) ->
            if (arrowHitbox.contains(pointerPos)) {
                return HitAt.HitNodeArrow(
                    layout = layout,
                    arrowHitbox = arrowHitbox,
                    sideType = arrowType,
                )
            }
        }
    }

    //check delete button first if a link is selected
    selectedNodeLink?.let { link ->
        val midpoint = nodeLinkMidpoint(link = link, layouts = layouts)
        if (midpoint != null && (pointerPos - midpoint).getDistance() <= deleteButtonRadius) {
            return HitAt.HitNodeLinkDeleteButton(link)
        }
    }

    val hitLink = returnHitNodeLinkOrNull(
        nodeLinks  = nodeLinks,
        layouts = layouts,
        pointerPos = pointerPos
    )
    if (hitLink != null) return HitAt.HitNodeLink(hitLink)

    return HitAt.HitCanvas
}

fun nodeLinkMidpoint(
    link: MapItemObject.SpaceNodeLink,
    layouts: List<NodeLayout>
): Offset? {
    val layoutMap = layouts.associateBy { it.node.uuid }
    val fromLayout = layoutMap[link.fromNodeUUID] ?: return null
    val toLayout = layoutMap[link.toNodeUUID] ?: return null

    val start = nodeSidePosition(nodeSide = link.fromNodeSide, nodeHitbox = fromLayout.nodeHitbox)
    val end = nodeSidePosition(nodeSide = link.toNodeSide, nodeHitbox = toLayout.nodeHitbox)

    return Offset(
        x = (start.x + end.x) / 2f,
        y = (start.y + end.y) / 2f
    )
}

fun returnHitNodeOrNull(
    layouts: List<NodeLayout>,
    pointerPos: Offset
): MapItemObject.SpaceNode? {
    //asReversed() is necessary so that nodes at the top are the ones that get detected first
    for (layout in layouts.asReversed()){
        if(layout.nodeHitbox.contains(pointerPos)){
            return layout.node
        }
    }
    return null
}

/*
    returns top, bottom, left or right side based on this hit detection:
    +-------+
    |\     /|
    | \ T / |
    |  \ /  |
    |L  X  R|
    |  / \  |
    | / B \ |
    |/     \|
    +-------+
*/
fun returnHitNodeSideOrNull(
    layouts: List<NodeLayout>,
    pointerPos: Offset
): NodeSideType? {
    val hitLayout = layouts.asReversed().firstOrNull { layout ->
        layout.nodeHitbox.contains(pointerPos)
    } ?: return null

    val nodeHitbox = hitLayout.nodeHitbox
    val nodeCenter = nodeHitbox.center

    val dx = pointerPos.x - nodeCenter.x
    val dy = pointerPos.y - nodeCenter.y

    val normalizedX = dx / (nodeHitbox.width  / 2f)
    val normalizedY = dy / (nodeHitbox.height / 2f)

    val side = if (kotlin.math.abs(x = normalizedX) > kotlin.math.abs(x = normalizedY)) {
        if (normalizedX > 0) NodeSideType.RIGHT
        else NodeSideType.LEFT
    } else {
        if (normalizedY > 0) NodeSideType.BOTTOM
        else NodeSideType.TOP
    }

    return side
}

fun returnHitNodeLinkOrNull(
    nodeLinks: List<MapItemObject.SpaceNodeLink>,
    layouts: List<NodeLayout>,
    pointerPos: Offset
): MapItemObject.SpaceNodeLink? {
    val layoutMap = layouts.associateBy { it.node.uuid }

    return nodeLinks.asReversed().firstOrNull { link ->
        val fromLayout = layoutMap[link.fromNodeUUID] ?: return@firstOrNull false
        val toLayout = layoutMap[link.toNodeUUID] ?: return@firstOrNull false

        val start = nodeSidePosition(nodeSide = link.fromNodeSide, nodeHitbox = fromLayout.nodeHitbox)
        val end = nodeSidePosition(nodeSide = link.toNodeSide, nodeHitbox = toLayout.nodeHitbox)

        distanceFromPointToLineSegment(pointerPos, start, end) <= NODE_LINK_HITBOX_TOLERANCE
    }
}

private fun distanceFromPointToLineSegment(
    point: Offset,
    lineStart: Offset,
    lineEnd: Offset
): Float {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val lengthSq = dx * dx + dy * dy

    if (lengthSq == 0f) return (point - lineStart).getDistance()

    val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lengthSq
    val clampedT = t.coerceIn(0f, 1f)

    val closestPoint = Offset(
        x = lineStart.x + clampedT * dx,
        y = lineStart.y + clampedT * dy
    )
    return (point - closestPoint).getDistance()
}

fun nodeSidePosition(nodeSide: NodeSideType, nodeHitbox: Rect): Offset {
    return when (nodeSide) {
        NodeSideType.TOP -> Offset(x = nodeHitbox.center.x, y = nodeHitbox.top)
        NodeSideType.BOTTOM -> Offset(x = nodeHitbox.center.x, y = nodeHitbox.bottom)
        NodeSideType.LEFT -> Offset(x = nodeHitbox.left, y = nodeHitbox.center.y)
        NodeSideType.RIGHT -> Offset(x = nodeHitbox.right, y = nodeHitbox.center.y)
    }
}