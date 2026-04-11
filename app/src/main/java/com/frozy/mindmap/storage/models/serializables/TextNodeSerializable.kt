package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class TextNodeSerializable (
    val version: Int = FeatureVersions.TEXT_NODE_DATA_VERSION,
    val uuid: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val borderColor: ULong?,
    val backgroundColor: ULong?,
    val text: String,
    val fontSize: Float
)