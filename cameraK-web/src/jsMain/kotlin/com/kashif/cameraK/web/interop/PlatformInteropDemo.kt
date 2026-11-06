package com.kashif.cameraK.web.interop

import com.kashif.cameraK.web.cameraKotlinGreeting

internal actual fun runPlatformInteropDemo(): CameraWebDemoResult {
    val browser = js("globalThis")
    browser.cameraKotlinGreeting = { cameraKotlinGreeting() }

    return CameraWebDemoResult(
        interopMessage = browser.cameraKWebExample() as String,
    )
}

internal actual fun isCameraSecureContext(): Boolean = js("globalThis.isSecureContext === true")

internal actual fun hasMediaDevicesApi(): Boolean = js(
    "typeof navigator !== 'undefined' && navigator.mediaDevices != null",
)

internal actual fun hasGetUserMediaApi(): Boolean = js(
    "typeof navigator !== 'undefined' && navigator.mediaDevices != null && typeof navigator.mediaDevices.getUserMedia === 'function'",
)

internal actual fun hasEnumerateDevicesApi(): Boolean = js(
    "typeof navigator !== 'undefined' && navigator.mediaDevices != null && typeof navigator.mediaDevices.enumerateDevices === 'function'",
)
