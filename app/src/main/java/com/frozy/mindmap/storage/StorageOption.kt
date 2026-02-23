package com.frozy.mindmap.storage

import com.frozy.mindmap.R

enum class StorageOption(val label: Int, val description: Int) {
    DEVICE(label = R.string.create_new_file_device_storage_label, description = R.string.create_new_file_device_storage_description),
    APP(label = R.string.create_new_file_app_storage_label,  description = R.string.create_new_file_app_storage_description)
}