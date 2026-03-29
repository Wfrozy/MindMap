package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class NodeLinkSerializable(
    val version: Int = FeatureVersions.NODE_LINK_DATA_VERSION,
    val uuid: String,
    val fromNodeUUID: String,
    val fromNodeSide: String,
    val toNodeUUID: String,
    val toNodeSide: String
)
