package com.frozy.mindmap.mapeditor.space.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.frozy.mindmap.mapeditor.space.camera.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.constants.models.NodeResizeHandleValues.NODE_RESIZE_HANDLE_HEIGHT
import com.frozy.mindmap.mapeditor.space.constants.models.NodeResizeHandleValues.NODE_RESIZE_HANDLE_WIDTH
import com.frozy.mindmap.mapeditor.space.input.nodeLinkMidpoint
import com.frozy.mindmap.mapeditor.space.input.nodeSidePosition
import com.frozy.mindmap.mapeditor.space.models.SpaceObject
import com.frozy.mindmap.mapeditor.space.node.arrowhandle.NodeArrowHandleValues
import com.frozy.mindmap.mapeditor.space.node.layout.NodeLayout
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType
import com.frozy.mindmap.mapeditor.space.nodelink.NodeLinkValues.NODE_LINK_DELETE_BUTTON_RADIUS
import com.frozy.mindmap.mapeditor.space.nodelink.NodeLinkValues.NODE_LINK_HEAD_ANGLE
import com.frozy.mindmap.mapeditor.space.nodelink.NodeLinkValues.NODE_LINK_HEAD_LINES_LENGTH
import com.frozy.mindmap.mapeditor.space.nodelink.NodeLinkValues.NODE_LINK_THICKNESS
import com.frozy.mindmap.mapeditor.space.nodelink.PendingNodeLink
import com.frozy.mindmap.ui.utils.strengthen
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

fun DrawScope.drawTextNode(
    layout: NodeLayout,
    camera: SpaceCameraState,
    fallbackSelectedBorderColor: Color,
    arrowUpPainter: VectorPainter,
    arrowDownPainter: VectorPainter,
    arrowLeftPainter: VectorPainter,
    arrowRightPainter: VectorPainter,
    textMeasurer: TextMeasurer
) {
    //make sure it is a TextNode
    if(layout.node !is SpaceObject.Node.TextNode) return

    //TextNodes have non-null textPadding values in their layout, this is so the smart cast gets applied
    if(layout.textPadding == null) return

    val fadedFallbackSelectedBorderColor = fallbackSelectedBorderColor.copy(
        alpha = fallbackSelectedBorderColor.alpha*0.2f
    )

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
            color = layout.node.borderColor?.strengthen() ?: fadedFallbackSelectedBorderColor,
            style = Stroke(width = outlineWidth * 2f),
            topLeft = nodeTopLeft,
            size = Size(nodeWidth, nodeHeight),
            cornerRadius = cornerRadius
        )

        val handleWidth = NODE_RESIZE_HANDLE_WIDTH
        val handleHeight = NODE_RESIZE_HANDLE_HEIGHT
        val handleStrokeWidth = outlineWidth / 1.5f
        val handleCornerRadius = CornerRadius(x = cornerRadius.x / 2f, y = cornerRadius.y / 2f)

        //top left resize handle outline
        drawRoundRect(
            color = layout.node.borderColor?.strengthen() ?: fadedFallbackSelectedBorderColor,
            style = Stroke(width = handleStrokeWidth),
            topLeft = layout.resizeHandles.topLeft.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        //top left resize handle fill
        drawRoundRect(
            color = layout.node.backgroundColor ?: Color.Transparent,
            topLeft = layout.resizeHandles.topLeft.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )



        //top right resize handle outline
        drawRoundRect(
            color = layout.node.borderColor?.strengthen() ?: fadedFallbackSelectedBorderColor,
            style = Stroke(width = handleStrokeWidth),
            topLeft = layout.resizeHandles.topRight.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        //top right resize handle fill
        drawRoundRect(
            color = layout.node.backgroundColor ?: Color.Transparent,
            topLeft = layout.resizeHandles.topRight.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )



        //bottom left resize handle outline
        drawRoundRect(
            color = layout.node.borderColor?.strengthen() ?: fadedFallbackSelectedBorderColor,
            style = Stroke(width = handleStrokeWidth),
            topLeft = layout.resizeHandles.bottomLeft.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        //bottom left resize handle fill
        drawRoundRect(
            color = layout.node.backgroundColor ?: Color.Transparent,
            topLeft = layout.resizeHandles.bottomLeft.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )



        //bottom right resize handle outline
        drawRoundRect(
            color = layout.node.borderColor?.strengthen() ?: fadedFallbackSelectedBorderColor,
            style = Stroke(width = handleStrokeWidth),
            topLeft = layout.resizeHandles.bottomRight.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        //bottom right resize handle fill
        drawRoundRect(
            color = layout.node.backgroundColor ?: Color.Transparent,
            topLeft = layout.resizeHandles.bottomRight.topLeft,
            size = Size(handleWidth, handleHeight),
            cornerRadius = handleCornerRadius
        )

        val arrowSize = NodeArrowHandleValues.ARROW_HANDLE_WIDTH_AND_HEIGHT.dp.toPx()

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

fun DrawScope.drawImageNode(
    layout: NodeLayout,
    imageBitmap: ImageBitmap,
    fallbackSelectedBorderColor: Color,
    arrowUpPainter: VectorPainter,
    arrowDownPainter: VectorPainter,
    arrowLeftPainter: VectorPainter,
    arrowRightPainter: VectorPainter,
) {
    val topLeft = layout.nodeHitbox.topLeft
    val width = layout.nodeHitbox.width
    val height = layout.nodeHitbox.height
    val cornerRadius = layout.cornerRadius

    val nodeTopLeft = layout.nodeHitbox.topLeft
    val nodeWidth = layout.nodeHitbox.width
    val nodeHeight = layout.nodeHitbox.height
    val outlineWidth = layout.nodeOutlineWidth

    val fadedFallbackSelectedBorderColor = fallbackSelectedBorderColor.copy(
        alpha = fallbackSelectedBorderColor.alpha*0.2f
    )

    clipPath(
        Path().apply {
            addRoundRect(RoundRect(
                rect = Rect(offset = topLeft, size = Size(width, height)),
                cornerRadius = cornerRadius
            ))
        }
    ) {
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(x = topLeft.x.toInt(), y = topLeft.y.toInt()),
            dstSize   = IntSize(width.toInt(), height.toInt())
        )
    }

    //border
    drawRoundRect(
        color = layout.node.borderColor ?: Color.Transparent,
        style = Stroke(width = layout.nodeOutlineWidth),
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = cornerRadius
    )

    if (layout.node.isSelected) {
        val handleWidth  = NODE_RESIZE_HANDLE_WIDTH
        val handleHeight = NODE_RESIZE_HANDLE_HEIGHT
        val handleStrokeWidth  = layout.nodeOutlineWidth / 1.5f
        val handleCornerRadius = CornerRadius(
            x = cornerRadius.x / 2f,
            y = cornerRadius.y / 2f
        )

        //selected border
        drawRoundRect(
            color = layout.node.borderColor?.strengthen() ?: fadedFallbackSelectedBorderColor,
            style = Stroke(width = outlineWidth * 2f),
            topLeft = nodeTopLeft,
            size = Size(nodeWidth, nodeHeight),
            cornerRadius = cornerRadius
        )

        //handles
        listOf(
            layout.resizeHandles.topLeft,
            layout.resizeHandles.topRight,
            layout.resizeHandles.bottomLeft,
            layout.resizeHandles.bottomRight
        ).forEach { handle ->
            drawRoundRect(
                color = layout.node.backgroundColor ?: Color.Transparent,
                topLeft = handle.topLeft,
                size = Size(handleWidth, handleHeight),
                cornerRadius = handleCornerRadius
            )
            drawRoundRect(
                color = layout.node.borderColor?.strengthen() ?: fadedFallbackSelectedBorderColor,
                style = Stroke(width = handleStrokeWidth),
                topLeft = handle.topLeft,
                size = Size(handleWidth, handleHeight),
                cornerRadius = handleCornerRadius
            )
        }

        val arrowSize = NodeArrowHandleValues.ARROW_HANDLE_WIDTH_AND_HEIGHT.dp.toPx()

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

fun DrawScope.drawAllNodeLinks(
    links: List<SpaceObject.NodeLink>,
    layouts: List<NodeLayout>,
    color: Color,
    selectedLink: SpaceObject.NodeLink?
) {
    val layoutMap = layouts.associateBy { it.node.uuid }
    val strokeWidth = NODE_LINK_THICKNESS.dp.toPx()
    val headLinesLength = NODE_LINK_HEAD_LINES_LENGTH.dp.toPx()

    links.forEach { link ->
        val fromLayout = layoutMap[link.fromNodeUUID] ?: return@forEach
        val toLayout = layoutMap[link.toNodeUUID] ?: return@forEach

        val start = nodeSidePosition(nodeSide = link.fromNodeSide, nodeHitbox = fromLayout.nodeHitbox)
        val end = nodeSidePosition(nodeSide = link.toNodeSide, nodeHitbox = toLayout.nodeHitbox)

        val linkColor = if(selectedLink != null && link.uuid == selectedLink.uuid) {
            color.strengthen()
        } else {
            color
        }

        drawLine(
            color = linkColor,
            start = start,
            end = end,
            strokeWidth = strokeWidth
        )

        val raw = end - start
        val length = raw.getDistance()
        if (length == 0f) return@forEach
        val direction = raw / length

        drawLinkArrowHead(
            tipPos = end,
            direction = direction,
            linesLength = headLinesLength,
            color = linkColor,
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawLinkArrowHead(
    tipPos: Offset,
    direction: Offset, //normalized direction vector pointing toward the tip
    linesLength: Float,
    color: Color,
    strokeWidth: Float
) {
    val angleRad = Math.toRadians(NODE_LINK_HEAD_ANGLE.toDouble()).toFloat()

    // rotate the direction vector left and right to get the two arrowhead lines
    val cos = kotlin.math.cos(x = angleRad)
    val sin = kotlin.math.sin(x = angleRad)

    val leftWing = Offset(
        x = direction.x * cos - direction.y * sin,
        y = direction.x * sin + direction.y * cos
    )
    val rightWing = Offset(
        x = direction.x * cos + direction.y * sin,
        y = -direction.x * sin + direction.y * cos
    )

    drawLine(
        color = color,
        start = tipPos,
        end = tipPos - leftWing * linesLength,
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color,
        start = tipPos,
        end = tipPos - rightWing * linesLength,
        strokeWidth = strokeWidth
    )
}

fun DrawScope.drawPendingNodeLink(
    pendingNodeLink: PendingNodeLink,
    layouts: List<NodeLayout>,
    arrowColor: Color
) {
    val fromLayout = layouts.firstOrNull { it.node.uuid == pendingNodeLink.fromNodeUUID } ?: return

    val start = when (pendingNodeLink.fromNodeSide) {
        NodeSideType.TOP -> Offset(
            x = fromLayout.nodeHitbox.center.x,
            y = fromLayout.nodeHitbox.top,
        )

        NodeSideType.BOTTOM -> Offset(
            x = fromLayout.nodeHitbox.center.x,
            y = fromLayout.nodeHitbox.bottom
        )

        NodeSideType.LEFT -> Offset(
            x = fromLayout.nodeHitbox.left,
            y = fromLayout.nodeHitbox.center.y
        )

        NodeSideType.RIGHT -> Offset(
            x = fromLayout.nodeHitbox.right,
            y = fromLayout.nodeHitbox.center.y
        )
    }

    drawLine(
        color = arrowColor.copy(alpha = 0.5f),
        start = start,
        end = pendingNodeLink.currentEndPos,
        strokeWidth = NODE_LINK_THICKNESS.dp.toPx(),
    )
}

fun DrawScope.drawSelectedNodeLinkDeleteButton(
    link: SpaceObject.NodeLink,
    layouts: List<NodeLayout>,
    painter: VectorPainter,
    tintColor: Color
) {
    val radius = NODE_LINK_DELETE_BUTTON_RADIUS.dp.toPx()

    val midpoint = nodeLinkMidpoint(
        link = link,
        layouts = layouts
    ) ?: return

    drawCircle(
        color = tintColor,
        radius = radius,
        center = midpoint
    )
    withTransform(
        transformBlock = {
            translate(
                left = midpoint.x - radius,
                top = midpoint.y - radius
            )
        }
    ) {
        with(painter) {
            draw(
                size = Size(radius * 2f, radius * 2f),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
    }
}