package com.frozy.mindmap.mapeditor.space.models

import java.util.UUID

sealed class MapItem(open val uuid: UUID) {
    data class Note(
        override val uuid: UUID = UUID.randomUUID(),
        val titleText: String = "",
        val contentText: String = ""
    ) : MapItem(uuid)

    data class Space(
        override val uuid: UUID = UUID.randomUUID(),
        val spaceNodeInfo: List<MapItemObject.SpaceNode> = emptyList(),
        val imageInfo: List<MapItemObject.Image> = emptyList()
    ) : MapItem(uuid)
}