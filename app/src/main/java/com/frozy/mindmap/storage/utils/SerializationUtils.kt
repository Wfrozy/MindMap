package com.frozy.mindmap.storage.utils

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.frozy.mindmap.mapeditor.space.models.MapItem
import com.frozy.mindmap.mapeditor.space.models.MapItemObject
import com.frozy.mindmap.mapeditor.space.models.SpaceCameraState
import com.frozy.mindmap.storage.models.serializables.ImageSerializable
import com.frozy.mindmap.storage.models.serializables.NoteSerializable
import com.frozy.mindmap.storage.models.serializables.SpaceCameraStateSerializable
import com.frozy.mindmap.storage.models.serializables.SpaceSerializable
import com.frozy.mindmap.storage.models.serializables.SpaceNodeSerializable
import java.util.UUID



/*
    Note and NoteData stuff ----------------------------------------------
*/

fun MapItem.Note.toSerializable(): NoteSerializable {
    return NoteSerializable(
        uuid = this.uuid.toString(),
        titleText = this.titleText,
        contentText = this.contentText
    )
}

fun NoteSerializable.toDeserialized(): MapItem.Note {
    return MapItem.Note(
        uuid = UUID.fromString(this.uuid),
        titleText = this.titleText,
        contentText = this.contentText
    )
}



/*
    SpaceNode and SpaceNodeData ----------------------------------------------
*/

fun MapItemObject.SpaceNode.toSerializable(): SpaceNodeSerializable {
    return SpaceNodeSerializable(
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

fun SpaceNodeSerializable.toDeserialized(): MapItemObject.SpaceNode {
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
    SpaceCameraState and SpaceCameraStateData ----------------------------------------------
*/

fun SpaceCameraState.toSerializable(): SpaceCameraStateSerializable {
    return SpaceCameraStateSerializable(
        x = this.offset.x,
        y = this.offset.y,
        scale = this.scale
    )
}

fun SpaceCameraStateSerializable.toDeserialized(): SpaceCameraState {
    return SpaceCameraState(
        offset = Offset(
            x = this.x,
            y = this.y
        ),
        overscrollX = 0f,
        scale = this.scale
    )
}



/*
    Image and ImageData ----------------------------------------------
*/

fun MapItemObject.Image.toSerializable(): ImageSerializable {
    return ImageSerializable(
        uuid = this.uuid.toString(),
        x = this.offset.x,
        y = this.offset.y,
        width = this.width,
        height = this.height,
        uri = this.imageUri.toString()
    )
}

fun ImageSerializable.toDeserialized(): MapItemObject.Image {
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

fun MapItem.Space.toSerializable(): SpaceSerializable {
    return SpaceSerializable(
        uuid = this.uuid.toString(),
        serializedSpaceNodeData = this.spaceNodeInfo.map { node ->
            node.toSerializable()
        },
        serializedImageData = this.imageInfo.map { image ->
            image.toSerializable()
        },
        serializedSpaceCameraState = this.cameraState.toSerializable()
    )
}

fun SpaceSerializable.toDeserialized(): MapItem.Space {
    return MapItem.Space(
        uuid = UUID.fromString(this.uuid),
        spaceNodeInfo = this.serializedSpaceNodeData.map { node ->
            node.toDeserialized()
        },
        imageInfo = this.serializedImageData.map { image ->
            image.toDeserialized()
        },
        cameraState = this.serializedSpaceCameraState.toDeserialized()
    )
}