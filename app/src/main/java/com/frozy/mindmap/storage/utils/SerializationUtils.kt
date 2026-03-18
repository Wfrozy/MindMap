package com.frozy.mindmap.storage.utils

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.mapeditor.models.MapItemObject
import com.frozy.mindmap.storage.models.serializables.ImageData
import com.frozy.mindmap.storage.models.serializables.NoteData
import com.frozy.mindmap.storage.models.serializables.SpaceData
import com.frozy.mindmap.storage.models.serializables.SpaceNodeData
import java.util.UUID



/*
    Note and NoteData stuff ----------------------------------------------
*/

fun MapItem.Note.toSerializable(): NoteData {
    return NoteData(
        uuid = this.uuid.toString(),
        titleText = this.titleText,
        contentText = this.contentText
    )
}

fun NoteData.toDeserializable(): MapItem.Note {
    return MapItem.Note(
        uuid = UUID.fromString(this.uuid),
        titleText = this.titleText,
        contentText = this.contentText
    )
}



/*
    SpaceNode and SpaceNodeData ----------------------------------------------
*/

fun MapItemObject.SpaceNode.toSerializable(): SpaceNodeData {
    return SpaceNodeData(
        uuid = this.uuid.toString(),
        x = this.offset.x,
        y = this.offset.y,
        width = this.width,
        height = this.height,
        borderColor = this.borderColor?.value,
        backgroundColor = this.backgroundColor?.value,
        text = this.text,
        fontSize = this.fontSize.value
    )
}

fun SpaceNodeData.toDeserializable(): MapItemObject.SpaceNode {
    return MapItemObject.SpaceNode(
        uuid = UUID.fromString(this.uuid),
        offset = Offset(x = this.x, y = this.y),
        width = this.width,
        height = this.height,
        isSelected = false,
        borderColor = this.borderColor?.let { Color(value = it) },
        backgroundColor = this.backgroundColor?.let { Color(value = it) },
        text = this.text,
        fontSize = this.fontSize.sp
    )
}



/*
    Image and ImageData ----------------------------------------------
*/

fun MapItemObject.Image.toSerializable(): ImageData {
    return ImageData(
        uuid = this.uuid.toString(),
        x = this.offset.x,
        y = this.offset.y,
        width = this.width,
        height = this.height,
        uri = this.imageUri.toString()
    )
}

fun ImageData.toDeserializable(): MapItemObject.Image {
    return MapItemObject.Image(
        uuid = UUID.fromString(this.uuid),
        offset = Offset(x = this.x, y = this.y),
        width = this.width,
        height = this.height,
        imageUri = Uri.parse(this.uri)
    )
}



/*
    Space and SpaceData ----------------------------------------------
*/

fun MapItem.Space.toSerializable(): SpaceData {
    return SpaceData(
        uuid = this.uuid.toString(),
        spaceNodeData = this.spaceNodeInfo.map { node ->
            node.toSerializable()
        },
        imageData = this.imageInfo.map { image ->
            image.toSerializable()
        }
    )
}

fun SpaceData.toDeserializable(): MapItem.Space {
    return MapItem.Space(
        uuid = UUID.fromString(this.uuid),
        spaceNodeInfo = this.spaceNodeData.map { node ->
            node.toDeserializable()
        },
        imageInfo = this.imageData.map { image ->
            image.toDeserializable()
        }
    )
}