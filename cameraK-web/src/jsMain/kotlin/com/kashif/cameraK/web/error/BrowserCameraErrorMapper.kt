package com.kashif.cameraK.web.error

import kotlin.js.asDynamic

internal actual fun extractBrowserCameraErrorDetails(error: Throwable): BrowserCameraErrorDetails {
    val browserError = error.asDynamic()
    val fallbackText = error.toString()
    val name = (browserError.name as? String) ?: fallbackText.substringBefore(':').takeIf {
        it.endsWith("Error")
    }
    val message = (browserError.message as? String)?.takeIf(String::isNotBlank)
        ?: error.message?.takeIf(String::isNotBlank)
        ?: fallbackText

    return BrowserCameraErrorDetails(
        name = name,
        message = message,
        constraint = browserError.constraint as? String,
        cause = error,
    )
}
