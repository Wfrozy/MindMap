package com.frozy.mindmap.storage.models.serializables

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SpaceObjectSerializable {
    @Serializable
    @SerialName(value = "textNode")
    data class TextNode(val data: TextNodeSerializable) : SpaceObjectSerializable()

    @Serializable
    @SerialName(value = "imageNode")
    data class ImageNode(val data: ImageNodeSerializable) : SpaceObjectSerializable()

    @Serializable
    @SerialName(value = "nodeLink")
    data class NodeLink(val data: NodeLinkSerializable) : SpaceObjectSerializable()
}
