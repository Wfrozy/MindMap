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
import com.frozy.mindmap.mapeditor.models.NodeArrowHandleValues
import com.frozy.mindmap.mapeditor.models.NodeLayout
import com.frozy.mindmap.mapeditor.models.NodeResizeHandleValues
import com.frozy.mindmap.mapeditor.models.SpaceCameraState
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
    layout: NodeLayout,
    camera: SpaceCameraState,
    selectedNodeBorderColor: Color,
    arrowUpPainter: VectorPainter,
    arrowDownPainter: VectorPainter,
    arrowLeftPainter: VectorPainter,
    arrowRightPainter: VectorPainter,
    textMeasurer: TextMeasurer
) {
    val nodeTopLeft = layout.nodeHitbox.topLeft
    val nodeWidth = layout.nodeHitbox.width
    val nodeHeight = layout.nodeHitbox.height
    val outlineWidth = layout.nodeOutlineWidth
    val cornerRadius = layout.cornerRadius
    val textPadding = layout.textPadding

    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(layout.node.text),
        style = TextStyle(
            fontSize = layout.node.fontSize * camera.scale,
            color = Color.White
        ),
        constraints = Constraints(
            maxWidth = (nodeWidth - textPadding * 2).toInt()
        )
    )

    val textX = nodeTopLeft.x + textPadding + (nodeWidth - textPadding * 2 - textLayoutResult.size.width) / 2f
    val textY = nodeTopLeft.y + (nodeHeight - textLayoutResult.size.height) / 2f

    //node background part
    drawRoundRect(
        color = layout.node.backgroundColor ?: Color.Transparent,
        topLeft = nodeTopLeft,
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = cornerRadius
    )

    //node border part
    drawRoundRect(
        color = layout.node.borderColor ?: Color.Transparent,
        style = Stroke(width = outlineWidth),
        topLeft = nodeTopLeft,
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = cornerRadius
    )

    //position to draw text
    clipPath(
        Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        offset = nodeTopLeft,
                        Size(nodeWidth, nodeHeight)
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

    if(layout.node.isSelected){
        //outline border
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = outlineWidth * 2f),
            topLeft = nodeTopLeft,
            size = Size(nodeWidth, nodeHeight),
            cornerRadius = cornerRadius
        )

        val handleWidth = NodeResizeHandleValues.WIDTH
        val handleHeight = NodeResizeHandleValues.HEIGHT
        val handleStrokeWidth = outlineWidth / 1.5f
        val handleCornerRadius = CornerRadius(x = cornerRadius.x / 2f, y = cornerRadius.y / 2f)


        //top left corner outline
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = handleStrokeWidth),
            topLeft = layout.resizeHandles.topLeft.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        //top left corner fill
        drawRoundRect(
            color = layout.node.backgroundColor ?: Color.Transparent,
            topLeft = layout.resizeHandles.topLeft.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )



        //top right corner outline
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = handleStrokeWidth),
            topLeft = layout.resizeHandles.topRight.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        //top right corner fill
        drawRoundRect(
            color = layout.node.backgroundColor ?: Color.Transparent,
            topLeft = layout.resizeHandles.topRight.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )



        //bottom left corner outline
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = handleStrokeWidth),
            topLeft = layout.resizeHandles.bottomLeft.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        //bottom left corner fill
        drawRoundRect(
            color = layout.node.backgroundColor ?: Color.Transparent,
            topLeft = layout.resizeHandles.bottomLeft.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )



        //bottom right corner outline
        drawRoundRect(
            color = selectedNodeBorderColor,
            style = Stroke(width = handleStrokeWidth),
            topLeft = layout.resizeHandles.bottomRight.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        //bottom right corner fill
        drawRoundRect(
            color = layout.node.backgroundColor ?: Color.Transparent,
            topLeft = layout.resizeHandles.bottomRight.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        val centerX = nodeTopLeft.x + nodeWidth / 2
        val centerY = nodeTopLeft.y + nodeHeight / 2

        val arrowSize = NodeArrowHandleValues.WIDTH_AND_HEIGHT.dp.toPx()
        val arrowOffset = NodeArrowHandleValues.PADDING_FROM_NODE.dp.toPx()

        val topArrowOffset = Offset(
            x = centerX - arrowSize / 2,
            y = nodeTopLeft.y - arrowOffset - arrowSize
        )
        val bottomArrowOffset = Offset(
            x = centerX - arrowSize / 2,
            y = nodeTopLeft.y + nodeHeight + arrowOffset
        )
        val leftArrowOffset = Offset(
            x = nodeTopLeft.x - arrowOffset - arrowSize,
            y = centerY - arrowSize / 2
        )
        val rightArrowOffset = Offset(
            x = nodeTopLeft.x + nodeWidth + arrowOffset,
            y = centerY - arrowSize / 2
        )

        //top arrow
        withTransform(
            transformBlock = {
                translate(
                    left = layout.arrowHandles.top.topLeft.x,
                    top = layout.arrowHandles.top.topLeft.y
                )
            }
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
            transformBlock = {
                translate(
                    left = layout.arrowHandles.bottom.topLeft.x,
                    top = layout.arrowHandles.bottom.topLeft.y
                )
            }
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
            transformBlock = {
                translate(
                    left = layout.arrowHandles.left.topLeft.x,
                    top = layout.arrowHandles.left.topLeft.y
                )
            }
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
            transformBlock = {
                translate(
                    left = layout.arrowHandles.right.topLeft.x,
                    top = layout.arrowHandles.right.topLeft.y
                )
            }
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