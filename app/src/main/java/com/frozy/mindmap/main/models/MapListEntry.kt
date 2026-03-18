package com.frozy.mindmap.main.models

import com.frozy.mindmap.storage.models.StorageOption
import java.util.UUID

data class MapListEntry (
    /**
     * [uuid]: this UUID comes from the metadataMap
     **/
    val uuid: UUID,
    val name: String,
    val storedIn: StorageOption,
    val lastModified: Long
)