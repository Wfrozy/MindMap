package com.frozy.mindmap.storage.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.frozy.mindmap.mapeditor.models.MapItem
import com.frozy.mindmap.mapeditor.space.camera.SpaceCameraState
import com.frozy.mindmap.mapeditor.space.models.SpaceObject
import com.frozy.mindmap.mapeditor.space.node.side.NodeSideType
import com.frozy.mindmap.storage.models.serializables.ImageNodeSerializable
import com.frozy.mindmap.storage.models.serializables.NodeLinkSerializable
import com.frozy.mindmap.storage.models.serializables.NoteSerializable
import com.frozy.mindmap.storage.models.serializables.SpaceCameraStateSerializable
import com.frozy.mindmap.storage.models.serializables.SpaceObjectSerializable
import com.frozy.mindmap.storage.models.serializables.SpaceSerializable
import com.frozy.mindmap.storage.models.serializables.TextNodeSerializable
import java.util.UUID


/*
    Note and NoteSerializable stuff ----------------------------------------------
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
    NodeLink and NodeLinkSerializable ----------------------------------------------
*/

fun SpaceObject.NodeLink.toSerializable(): NodeLinkSerializable {
    return NodeLinkSerializable(
        uuid = this.uuid.toString(),
        fromNodeUUID = this.fromNodeUUID.toString(),
        fromNodeSide = this.fromNodeSide.name,
        toNodeUUID = this.toNodeUUID.toString(),
        toNodeSide = this.toNodeSide.name
    )
}

fun NodeLinkSerializable.toDeserialized(): SpaceObject.NodeLink {
    return SpaceObject.NodeLink(
        uuid = UUID.fromString(this.uuid),
        fromNodeUUID = UUID.fromString(this.fromNodeUUID),
        fromNodeSide = NodeSideType.valueOf(this.fromNodeSide),
        toNodeUUID = UUID.fromString(this.toNodeUUID),
        toNodeSide = NodeSideType.valueOf(this.toNodeSide)
    )
}




/*
    TextNodeSerializable and TextNodeSerializable ----------------------------------------------
*/

fun SpaceObject.Node.TextNode.toSerializable(): TextNodeSerializable {
    return TextNodeSerializable(
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

fun TextNodeSerializable.toDeserialized(): SpaceObject.Node.TextNode {
    return SpaceObject.Node.TextNode(
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
    SpaceCameraState and SpaceCameraStateSerializable ----------------------------------------------
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
    ImageNode and ImageNodeSerializable ----------------------------------------------
*/

fun SpaceObject.Node.ImageNode.toSerializable(): ImageNodeSerializable {
    return ImageNodeSerializable(
        uuid = this.uuid.toString(),
        x = this.offset.x,
        y = this.offset.y,
        width = this.width,
        height = this.height,
        borderColor = this.borderColor?.value,
        backgroundColor = this.backgroundColor?.value,
        bitmapBytes = this.bitmapBytes
    )
}

fun ImageNodeSerializable.toDeserialized(): SpaceObject.Node.ImageNode {
    return SpaceObject.Node.ImageNode(
        uuid = UUID.fromString(this.uuid),
        offset = Offset(x = this.x, y = this.y),
        width = this.width,
        height = this.height,
        borderColor = this.borderColor?.let { Color(value = it) },
        backgroundColor = this.backgroundColor?.let { Color(value = it) },
        bitmapBytes = this.bitmapBytes
    )
}



/*
    Space and SpaceSerializable ----------------------------------------------
*/

fun MapItem.Space.toSerializable(): SpaceSerializable {
    return SpaceSerializable(
        uuid = this.uuid.toString(),
        serializedCameraState = this.cameraState.toSerializable(),
        serializedSpaceObjectData = this.objectInfo.map { obj ->
            when(obj){
                is SpaceObject.Node.TextNode -> {
                    SpaceObjectSerializable.TextNode(data = obj.toSerializable())
                }
                is SpaceObject.Node.ImageNode -> {
                    SpaceObjectSerializable.ImageNode(data = obj.toSerializable())
                }
                is SpaceObject.NodeLink -> {
                    SpaceObjectSerializable.NodeLink(data = obj.toSerializable())
                }
            }
        }
    )
}

fun SpaceSerializable.toDeserialized(): MapItem.Space {
    return MapItem.Space(
        uuid = UUID.fromString(uuid),
        cameraState = this.serializedCameraState.toDeserialized(),
        objectInfo = serializedSpaceObjectData.map { spaceObject ->
            when (spaceObject) {
                is SpaceObjectSerializable.TextNode  -> spaceObject.data.toDeserialized()
                is SpaceObjectSerializable.ImageNode -> spaceObject.data.toDeserialized()
                is SpaceObjectSerializable.NodeLink  -> spaceObject.data.toDeserialized()
            }
        }
    )
}