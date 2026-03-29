package com.frozy.mindmap.storage.models

import com.frozy.mindmap.mapeditor.models.MapItem

data class LoadedMapData(
    val items: List<MapItem>,
    val lastPageIndex: Int
)