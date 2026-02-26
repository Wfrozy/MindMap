package com.frozy.mindmap.mapeditor.space.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.mapeditor.model.MapItemObject
import com.frozy.mindmap.mapeditor.space.SpaceCameraState
import org.jetbrains.annotations.TestOnly
import kotlin.math.ceil
import kotlin.math.floor

fun DrawScope.drawInfiniteDotGrid(
    camera: SpaceCameraState,
    dotRadius: Float,
    dotSpacing: Float,
    dotColor: Color
) {
    val radius = dotRadius * camera.scale

    //calculate camera edges in world space
    val leftEdge = -camera.offset.x / camera.scale
    val topEdge = -camera.offset.y / camera.scale
    val rightEdge = leftEdge + size.width / camera.scale
    val bottomEdge = topEdge + size.height / camera.scale

    //determine which grid columns and rows to draw
    val startColX = floor(leftEdge / dotSpacing).toInt() - 1
    val startColY = floor(topEdge / dotSpacing).toInt() - 1
    val endColX = ceil(rightEdge / dotSpacing).toInt() + 1
    val endColY = ceil(bottomEdge / dotSpacing).toInt() + 1

    //loop over each grid position and draw the circles
    for (x in startColX..endColX) {
        for (y in startColY..endColY) {
            val worldX = x * dotSpacing
            val worldY = y * dotSpacing

            val screenX = worldX * camera.scale + camera.offset.x
            val screenY = worldY * camera.scale + camera.offset.y

            drawCircle(
                color = dotColor,
                radius = radius,
                center = Offset(screenX, screenY)
            )
        }
    }
}
fun DrawScope.drawLeftBoundaryFade(
    color: Color,
    width: Float,
    alpha: Float,
    overscroll: Float
){
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                color.copy(alpha),
                color.copy(alpha = 0f),
            ),
            startX = 0f,
            endX = width + overscroll
        ),
        topLeft = Offset(x = 0f, y = 0f),
        size = Size(width + overscroll, size.height)
    )
}

fun DrawScope.drawRightBoundaryFade(
    color: Color,
    width: Float,
    alpha: Float,
    overscroll: Float
){
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                color.copy(alpha),
                color.copy(alpha = 0f),
            ),
            startX = size.width,
            endX = size.width - width - overscroll
        ),
        topLeft = Offset(x = size.width - width - overscroll, y = 0f),
        size = Size(width + overscroll, size.height)
    )
}


fun DrawScope.drawBoundaryArrow(
    painter: VectorPainter,
    alpha: Float,
    translateLeft: Float,
    translateTop: Float,
    drawSizeWidth: Float,
    drawSizeHeight: Float,
    tintColor: Color
){

    withTransform(transformBlock = { translate(left = translateLeft, top = translateTop) }) {
        with(receiver = painter) {
            draw(
                size = Size(width = drawSizeWidth, height = drawSizeHeight),
                alpha = alpha,
                colorFilter = ColorFilter.tint(tintColor)
            )
        }
    }
}

fun DrawScope.drawNode(
    node: MapItemObject.SpaceNode,
    camera: SpaceCameraState
) {
    val scaledOffset = (node.offset * camera.scale) + camera.offset
    val scaledWidth = node.width * camera.scale
    val scaledHeight = node.height * camera.scale

    drawRoundRect(
        color = node.backgroundColor ?: Color.Transparent,
        topLeft = scaledOffset,
        size = Size(scaledWidth, scaledHeight),
        cornerRadius = CornerRadius(x = 16f, y = 16f)
    )

    drawRoundRect(
        color = node.borderColor ?: Color.Transparent,
        style = Stroke(width = 3f),
        topLeft = scaledOffset,
        size = Size(scaledWidth, scaledHeight),
        cornerRadius = CornerRadius(x = 16f, y = 16f)
    )
}