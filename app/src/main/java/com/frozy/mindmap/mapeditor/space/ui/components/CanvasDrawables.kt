package com.frozy.mindmap.mapeditor.space.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.mapeditor.model.MapItemObject
import com.frozy.mindmap.mapeditor.model.SpaceCameraState
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
    val startColX = floor(x = leftEdge / dotSpacing).toInt() - 1
    val startColY = floor(x = topEdge / dotSpacing).toInt() - 1
    val endColX = ceil(x = rightEdge / dotSpacing).toInt() + 1
    val endColY = ceil(x = bottomEdge / dotSpacing).toInt() + 1

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
                center = Offset(x = screenX, y = screenY)
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
    camera: SpaceCameraState,
    selectedNodeBorderColor: Color,
    arrowUpPainter: VectorPainter,
    arrowDownPainter: VectorPainter,
    arrowLeftPainter: VectorPainter,
    arrowRightPainter: VectorPainter,
    textMeasurer: TextMeasurer
) {
    val scaledNodeOffset = (node.offset * camera.scale) + camera.offset
    val scaledNodeWidth = node.width * camera.scale
    val scaledNodeHeight = node.height * camera.scale
    val scaledStrokeWidth = 5f * camera.scale

    val cornerRadiusX = 26f * camera.scale
    val cornerRadiusY = 26f * camera.scale
    val cornerRadius = CornerRadius(x = cornerRadiusX, y = cornerRadiusY)

    val textPadding = (12 * camera.scale).toInt()

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(node.text),
        style = TextStyle(
            fontSize = node.fontSize * camera.scale,
            color = Color.White
        ),
        constraints = Constraints(
            maxWidth = (scaledNodeWidth - textPadding * 2).toInt()
        )
    )

    val textX = scaledNodeOffset.x + textPadding + (scaledNodeWidth - textPadding * 2 - textLayoutResult.size.width) / 2f
    val textY = scaledNodeOffset.y + (scaledNodeHeight - textLayoutResult.size.height) / 2f

    //node background part
    drawRoundRect(
        color = node.backgroundColor ?: Color.Transparent,
        topLeft = scaledNodeOffset,
        size = Size(scaledNodeWidth, scaledNodeHeight),
        cornerRadius = cornerRadius
    )

    //node border part
    drawRoundRect(
        color = node.borderColor ?: Color.Transparent,
        style = Stroke(width = scaledStrokeWidth),
        topLeft = scaledNodeOffset,
        size = Size(scaledNodeWidth, scaledNodeHeight),
        cornerRadius = cornerRadius
    )

    clipPath(
        Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        scaledNodeOffset,
                        Size(scaledNodeWidth, scaledNodeHeight)
                    ),
                    cornerRadius
                )
            )
        }
    ) {
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = textX,
                y = textY
            )
        )
    }

    if(node.isSelected){
        //outline border
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = scaledStrokeWidth * 2f),
            topLeft = scaledNodeOffset,
            size = Size(scaledNodeWidth, scaledNodeHeight),
            cornerRadius = cornerRadius
        )

        val cornerWidth = 45f
        val cornerHeight = 45f
        val cornerStrokeWidth = scaledStrokeWidth / 1.5f
        val cornerCornerRadius = CornerRadius(x = cornerRadiusX / 2f, y = cornerRadiusY / 2f)


        val topLeftCornerOffset = Offset(
            x = scaledNodeOffset.x - (cornerWidth / 2.5f),
            y = scaledNodeOffset.y - (cornerHeight / 2.5f)
        )

        //top left corner outline
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = cornerStrokeWidth),
            topLeft = topLeftCornerOffset,
            size = Size(cornerWidth, cornerHeight),
            cornerRadius = cornerCornerRadius
        )

        //top left corner fill
        drawRoundRect(
            color = node.backgroundColor ?: Color.Transparent,
            topLeft = topLeftCornerOffset,
            size = Size(cornerWidth, cornerHeight),
            cornerRadius = cornerCornerRadius
        )


        val topRightCornerOffset = Offset(
            x = (scaledNodeOffset.x + scaledNodeWidth) - (cornerHeight / 2.5f),
            y = scaledNodeOffset.y - (cornerHeight / 2.5f)
        )

        //top right corner outline
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = cornerStrokeWidth),
            topLeft = topRightCornerOffset,
            size = Size(cornerWidth, cornerHeight),
            cornerRadius = cornerCornerRadius
        )

        //top right corner fill
        drawRoundRect(
            color = node.backgroundColor ?: Color.Transparent,
            topLeft = topRightCornerOffset,
            size = Size(cornerWidth, cornerHeight),
            cornerRadius = cornerCornerRadius
        )


        val bottomLeftCornerOffset = Offset(
            x = scaledNodeOffset.x - (cornerWidth / 2.5f),
            y = (scaledNodeOffset.y + scaledNodeHeight) - (cornerWidth / 2.5f)
        )

        //bottom left corner outline
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = cornerStrokeWidth),
            topLeft = bottomLeftCornerOffset,
            size = Size(cornerWidth, cornerHeight),
            cornerRadius = cornerCornerRadius
        )

        //bottom left corner fill
        drawRoundRect(
            color = node.backgroundColor ?: Color.Transparent,
            topLeft = bottomLeftCornerOffset,
            size = Size(cornerWidth, cornerHeight),
            cornerRadius = cornerCornerRadius
        )


        val bottomRightCornerOffset = Offset(
            x = (scaledNodeOffset.x + scaledNodeWidth) - (cornerWidth / 2.5f),
            y = (scaledNodeOffset.y + scaledNodeHeight) - (cornerWidth / 2.5f)
        )

        //bottom right corner outline
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = cornerStrokeWidth),
            topLeft = bottomRightCornerOffset,
            size = Size(cornerWidth, cornerHeight),
            cornerRadius = cornerCornerRadius
        )

        //bottom right corner fill
        drawRoundRect(
            color = node.backgroundColor ?: Color.Transparent,
            topLeft = bottomRightCornerOffset,
            size = Size(cornerWidth, cornerHeight),
            cornerRadius = cornerCornerRadius
        )

        val centerX = scaledNodeOffset.x + scaledNodeWidth / 2
        val centerY = scaledNodeOffset.y + scaledNodeHeight / 2

        val arrowSize = 36.dp.toPx()
        val arrowOffset = 12.dp.toPx()

        val topArrowOffset = Offset(
            x = centerX - arrowSize / 2,
            y = scaledNodeOffset.y - arrowOffset - arrowSize
        )
        val bottomArrowOffset = Offset(
            x = centerX - arrowSize / 2,
            y = scaledNodeOffset.y + scaledNodeHeight + arrowOffset
        )
        val leftArrowOffset = Offset(
            x = scaledNodeOffset.x - arrowOffset - arrowSize,
            y = centerY - arrowSize / 2
        )
        val rightArrowOffset = Offset(
            x = scaledNodeOffset.x + scaledNodeWidth + arrowOffset,
            y = centerY - arrowSize / 2
        )

        //top arrow
        withTransform(
            transformBlock = { translate(left = topArrowOffset.x, top = topArrowOffset.y) }
        ) {
            with(receiver = arrowUpPainter) {
                draw(
                    size = Size(width = arrowSize, height = arrowSize),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
        //bottom arrow
        withTransform(
            transformBlock = { translate(left = bottomArrowOffset.x, top = bottomArrowOffset.y) }
        ) {
            with(receiver = arrowDownPainter) {
                draw(
                    size = Size(width = arrowSize, height = arrowSize),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
        //left arrow
        withTransform(
            transformBlock = { translate(left = leftArrowOffset.x, top = leftArrowOffset.y) }
        ) {
            with(receiver = arrowLeftPainter) {
                draw(
                    size = Size(width = arrowSize, height = arrowSize),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
        //right arrow
        withTransform(
            transformBlock = { translate(left = rightArrowOffset.x, top = rightArrowOffset.y) }
        ) {
            with(receiver = arrowRightPainter) {
                draw(
                    size = Size(width = arrowSize, height = arrowSize),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

    }
}