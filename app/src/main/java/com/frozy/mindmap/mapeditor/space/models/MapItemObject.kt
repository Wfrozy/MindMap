package com.frozy.mindmap.mapeditor.space.models

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import java.util.UUID

sealed class MapItemObject(
    open val uuid: UUID,
    open val offset: Offset
) {

    data class Image(
        override val uuid: UUID = UUID.randomUUID(),
        override val offset: Offset,
        val width: Float,
        val height: Float,
        val imageUri: Uri
    ) : MapItemObject(uuid, offset)

    data class SpaceNode(
        override val uuid: UUID = UUID.randomUUID(),
        override val offset: Offset,
        val width: Float,
        val height: Float,
        val isSelected: Boolean = false,
        val borderColor: Color?, //leave null for no border
        val backgroundColor: Color?,
        val text: String,
        val fontSize: TextUnit
    ) : MapItemObject(uuid, offset)
}