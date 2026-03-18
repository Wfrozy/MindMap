package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class SpaceData (
    val version: Int = FeatureVersions.SPACE_DATA_VERSION,
    val uuid: String,
    val spaceNodeData: List<SpaceNodeData>,
    val imageData: List<ImageData>
)