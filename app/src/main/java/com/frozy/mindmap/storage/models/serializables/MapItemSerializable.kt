package com.frozy.mindmap.storage.models.serializables

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MapItemSerializable {
    @Serializable
    @SerialName(value = "note")
    data class Note(val data: NoteSerializable) : MapItemSerializable()

    @Serializable
    @SerialName(value = "space")
    data class Space(val data: SpaceSerializable) : MapItemSerializable()
}