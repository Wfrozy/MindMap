package com.frozy.mindmap.storage.models.serializables

import com.frozy.mindmap.constants.FeatureVersions
import kotlinx.serialization.Serializable

@Serializable
data class NoteData (
    val version: Int = FeatureVersions.NOTE_DATA_VERSION,
    val uuid: String,
    val titleText: String,
    val contentText: String
)