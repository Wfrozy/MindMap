package com.frozy.mindmap.storage.models

import com.frozy.mindmap.mapeditor.space.models.MapItem

data class LoadedMapData(
    val items: List<MapItem>,
    val lastPageIndex: Int
)