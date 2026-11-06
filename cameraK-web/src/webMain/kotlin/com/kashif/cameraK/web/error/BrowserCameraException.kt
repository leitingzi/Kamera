package com.kashif.cameraK.web.error

/**
 * A browser camera failure with a stable facade-level [code] and, when available, the browser's
 * original exception name and failed constraint. Implementations should preserve the original
 * cause rather than replacing it with an untyped message.
 */
class BrowserCameraException(
    val code: BrowserCameraErrorCode,
    override val message: String,
    val browserErrorName: String? = null,
    val constraint: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
