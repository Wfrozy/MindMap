package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class MapFileSerializable(
    val version: Int = FeatureVersions.MAP_FILE_VERSION,
    val serializedItems: List<MapItemSerializable>,
    val lastPageIndex: Int = 0
)