package com.frozy.mindmap.mapeditor.space.input

import androidx.compose.ui.geometry.Rect
import com.frozy.mindmap.mapeditor.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.node.layout.NodeLayout
import com.frozy.mindmap.mapeditor.space.node.resizehandle.NodeResizeHandleType
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType

sealed class HitAt {

    data class HitNodeBody(
        val layout: NodeLayout
    ) : HitAt()

    data class HitNodeResizeHandle(
        val layout: NodeLayout,
        val handleType: NodeResizeHandleType,
        val handleHitbox: Rect
    ) : HitAt()

    data class HitNodeArrow(
        val layout: NodeLayout,
        val sideType: NodeSideType,
        val arrowHitbox: Rect
    ) : HitAt()

    data class HitNodeLink(
        val link: MapItemObject.SpaceNodeLink
    ) : HitAt()

    data class HitNodeLinkDeleteButton(
        val link: MapItemObject.SpaceNodeLink
    ) : HitAt()

    data object HitCanvas : HitAt()
}