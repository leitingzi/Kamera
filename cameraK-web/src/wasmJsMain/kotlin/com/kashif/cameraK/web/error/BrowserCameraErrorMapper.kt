package com.kashif.cameraK.web.error

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.asJsException

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(error) => typeof error?.name === 'string' ? error.name : ''")
private external fun browserErrorName(error: JsAny?): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(error) => typeof error?.message === 'string' ? error.message : ''")
private external fun browserErrorMessage(error: JsAny?): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(error) => typeof error?.constraint === 'string' ? error.constraint : ''")
private external fun browserErrorConstraint(error: JsAny?): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(error) => error == null ? '' : String(error)")
private external fun browserErrorToString(error: JsAny?): String

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun extractBrowserCameraErrorDetails(error: JsAny): BrowserCameraErrorDetails {
    val browserError: JsAny? = error
    val name = browserErrorName(browserError)?.takeIf(String::isNotBlank)
    val message = browserErrorMessage(browserError)?.takeIf(String::isNotBlank)
        ?: browserErrorToString(browserError)
    val constraint = browserErrorConstraint(browserError)?.takeIf(String::isNotBlank)

    return BrowserCameraErrorDetails(
        name = name,
        message = message,
        constraint = constraint,
        cause = error.asJsException(),
    )
}
