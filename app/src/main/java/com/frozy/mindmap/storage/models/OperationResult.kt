package com.frozy.mindmap.storage.models

sealed class OperationResult {
    data object Success : OperationResult()
    data class Error(
        val e: Exception
    ) : OperationResult()
}