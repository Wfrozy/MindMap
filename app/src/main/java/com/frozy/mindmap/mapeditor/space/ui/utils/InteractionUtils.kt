package com.frozy.mindmap.mapeditor.space.ui.utils

import androidx.compose.ui.geometry.Offset
import com.frozy.mindmap.mapeditor.space.models.HitAt
import com.frozy.mindmap.mapeditor.space.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.models.NodeLayout
import kotlin.collections.asReversed

fun categorizeHitAtType(
    layouts: List<NodeLayout>,
    pointerPos: Offset
): HitAt {

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
                    arrowType = arrowType,
                )
            }
        }
    }

    return HitAt.HitCanvas
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