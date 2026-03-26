package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class SpaceSerializable (
    val version: Int = FeatureVersions.SPACE_DATA_VERSION,
    val uuid: String,
    val serializedSpaceNodeData: List<SpaceNodeSerializable>,
    val serializedSpaceCameraState: SpaceCameraStateSerializable,
    val serializedImageData: List<ImageSerializable>
)