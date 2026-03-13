package com.frozy.mindmap.mapeditor.models

import androidx.compose.ui.geometry.Rect

sealed class HitAt {

    data class HitNodeBody(
        val layout: NodeLayout
    ) : HitAt()

    data class HitNodeResizeHandle(
        val layout: NodeLayout,
        val handleHitbox: Rect
    ) : HitAt()

    data class HitNodeArrow(
        val layout: NodeLayout,
        val arrowHitbox: Rect
    ) : HitAt()

    data object HitCanvas : HitAt()
}