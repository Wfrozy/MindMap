package com.frozy.mindmap.storage

//returns whether the operation was successful or not
//optionally has the data being operated on
//optionally has some error info
//optionally has an index value for logging
data class OperationResult<T>(
    val isSuccess: Boolean,
    val data: T? = null,
    val errorInfo: Throwable? = null,
    val index: Int? = null
)