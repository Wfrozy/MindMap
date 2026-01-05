package com.frozy.mindmap

data class FileData(
    val fileName: String = "",
    val fileContent: String = "",
    val storage: StorageOption = StorageOption.DEVICE,
    val timeStampID: Long = System.currentTimeMillis()
)
enum class StorageOption(val label: Int, val description: Int) {
    DEVICE(label = R.string.create_new_file_device_storage_label, description = R.string.create_new_file_device_storage_description),
    APP(label = R.string.create_new_file_app_storage_label,  description = R.string.create_new_file_app_storage_description)
}