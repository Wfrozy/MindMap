package com.frozy.mindmap.constants

//The key principle: any serialized data you plan to store long-term should have a version so you can handle migrations safely.
//Comment author: ChatGPT

object FeatureVersions {
    const val SPACE_NODE_DATA_VERSION = 1
    const val SPACE_DATA_VERSION = 1
    const val NOTE_DATA_VERSION = 1
    const val IMAGE_DATA_VERSION = 1
    const val MAP_JSON_FORMAT_VERSION = 1
}