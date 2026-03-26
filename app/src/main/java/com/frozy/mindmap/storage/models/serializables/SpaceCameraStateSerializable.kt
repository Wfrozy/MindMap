package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class SpaceCameraStateSerializable (
    val version: Int = FeatureVersions.SPACE_CAMERA_VERSION,
    val x: Float,
    val y: Float,
    val scale: Float
)