package com.frozy.mindmap.mapeditor.space.models

import androidx.compose.ui.geometry.Rect

sealed class HitAt {

    data class HitNodeBody(
        val layout: NodeLayout
    ) : HitAt()

    data class HitNodeResizeHandle(
        val layout: NodeLayout,
        val handleType: ResizeHandleType,
        val handleHitbox: Rect
    ) : HitAt()

    data class HitNodeArrow(
        val layout: NodeLayout,
        val arrowType: ArrowHandleType,
        val arrowHitbox: Rect
    ) : HitAt()

    data object HitCanvas : HitAt()
}