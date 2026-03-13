package com.frozy.mindmap.mapeditor.space.ui.utils

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.frozy.mindmap.mapeditor.models.HitAt
import com.frozy.mindmap.mapeditor.models.MapItemObject
import com.frozy.mindmap.mapeditor.models.NodeLayout
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

        layout.resizeHandles.entries.forEach { handle ->
            if (handle.contains(pointerPos)) {
                return HitAt.HitNodeResizeHandle(layout, handleHitbox = handle)
            }
        }

        layout.arrowHandles.entries.forEach { arrow ->
            if (arrow.contains(pointerPos)) {
                return HitAt.HitNodeArrow(layout, arrowHitbox = arrow)
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