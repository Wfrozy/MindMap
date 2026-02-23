package com.frozy.mindmap.storage.util
const val MAX_MAP_NAME_LENGTH = 16

//ensure filename ends with .json and does not contain invalid chars in the file name
fun String.sanitizeAndEnsureJsonExtension(fallbackString: String): String {
    val trimmed = this.trim().ifBlank { fallbackString }

    //replace illegal file chars with underscore
    val sanitized = trimmed.replace(Regex(pattern = "[/\\\\:*?\"<>|]"), replacement = "_")

    return if (sanitized.endsWith(".json", ignoreCase = true)) sanitized
    else "$sanitized.json"
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