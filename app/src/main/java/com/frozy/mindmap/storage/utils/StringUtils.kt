package com.frozy.mindmap.storage.utils

import com.frozy.mindmap.constants.FileExtension.APP_FILE_EXTENSION
import com.frozy.mindmap.main.models.MainActivityValues.MAX_MAP_NAME_LENGTH

//ensure filename ends with the correct extension and does not contain invalid chars in the file name
fun String.sanitizeAndEnsureExtension(): String {
    val trimmed = this.trim().ifBlank { this }

    //replace illegal file chars with underscore
    val sanitized = trimmed.replace(Regex(pattern = "[/\\\\:*?\"<>|]"), replacement = "_")

    return if (sanitized.endsWith(APP_FILE_EXTENSION, ignoreCase = true)) {
        sanitized
    } else {
        sanitized + APP_FILE_EXTENSION
    }
}

fun String.checkIfFileNameIsInvalid(
    charCheck: Boolean = true,
    blankCheck: Boolean = true,
    lengthCheck: Boolean = true,
): Boolean {

    val hasInvalidChars = charCheck && this.any { it in "/\\:*?\"<>|" }
    val isBlank = blankCheck && this.isBlank()
    val isLengthInvalid = lengthCheck && this.length > MAX_MAP_NAME_LENGTH

    return hasInvalidChars || isBlank || isLengthInvalid
}