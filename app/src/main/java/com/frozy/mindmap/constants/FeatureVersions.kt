package com.frozy.mindmap.constants

//The key principle: any serialized data you plan to store long-term should have a version so you can handle migrations safely.
//Comment author: ChatGPT lol

object FeatureVersions {
    const val MAP_FILE_VERSION = 1

    const val NOTE_DATA_VERSION = 1
    const val SPACE_DATA_VERSION = 1

    const val SPACE_CAMERA_VERSION = 1
    const val TEXT_NODE_DATA_VERSION = 1
    const val IMAGE_NODE_DATA_VERSION = 1
    const val NODE_LINK_DATA_VERSION = 1
}