package com.monster.literaryflow.core.result

sealed class FlowResult<out T> {
    data class Success<T>(val value: T) : FlowResult<T>()
    data class Failure(val error: FlowError) : FlowResult<Nothing>()
}

data class FlowError(
    val code: ErrorCode,
    val message: String,
    val cause: Throwable? = null
)

enum class ErrorCode {
    INVALID_IMPORT_FORMAT,
    UNSUPPORTED_SCHEMA,
    LEGACY_CONVERSION_FAILED,
    PERMISSION_MISSING,
    SCREENSHOT_FAILED,
    OCR_FAILED,
    ACTION_FAILED,
    TASK_RECURSION_DETECTED,
    TASK_CANCELLED
}
