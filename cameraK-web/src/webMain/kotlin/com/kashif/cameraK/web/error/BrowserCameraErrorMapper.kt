package com.kashif.cameraK.web.error

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsPromiseError

/** Browser exception details extracted by each platform interop implementation. */
internal data class BrowserCameraErrorDetails(
    val name: String?,
    val message: String,
    val constraint: String?,
    val cause: Throwable?,
)

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun extractBrowserCameraErrorDetails(error: JsPromiseError): BrowserCameraErrorDetails

/** Converts browser media errors without dropping their native name, message, or constraint. */
@OptIn(ExperimentalWasmJsInterop::class)
internal fun JsPromiseError.toBrowserCameraException(): BrowserCameraException {
    val details = extractBrowserCameraErrorDetails(this)
    val code = when (details.name) {
        "NotAllowedError" -> BrowserCameraErrorCode.PERMISSION_DENIED
        "NotFoundError" -> BrowserCameraErrorCode.DEVICE_NOT_FOUND
        "NotReadableError" -> BrowserCameraErrorCode.DEVICE_UNAVAILABLE
        "OverconstrainedError" -> BrowserCameraErrorCode.CONSTRAINT_NOT_SATISFIED
        "SecurityError" -> BrowserCameraErrorCode.UNSUPPORTED
        else -> BrowserCameraErrorCode.UNKNOWN
    }

    return BrowserCameraException(
        code = code,
        message = details.message,
        browserErrorName = details.name,
        constraint = details.constraint,
        cause = details.cause,
    )
}
