package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class SpaceSerializable (
    val version: Int = FeatureVersions.SPACE_DATA_VERSION,
    val uuid: String,
    val serializedCameraState: SpaceCameraStateSerializable,
    val serializedSpaceObjectData: List<SpaceObjectSerializable>
)