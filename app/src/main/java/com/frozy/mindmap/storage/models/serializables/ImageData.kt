package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class ImageData(
    val version: Int = FeatureVersions.IMAGE_DATA_VERSION,
    val uuid: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val uri: String
)