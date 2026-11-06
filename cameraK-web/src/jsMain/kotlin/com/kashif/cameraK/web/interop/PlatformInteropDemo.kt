package com.kashif.cameraK.web.interop

import com.kashif.cameraK.web.cameraKotlinGreeting

internal actual fun runPlatformInteropDemo(): CameraWebDemoResult {
    val browser = js("globalThis")
    browser.cameraKotlinGreeting = { cameraKotlinGreeting() }

    return CameraWebDemoResult(
        interopMessage = browser.cameraKWebExample() as String,
    )
}

internal actual fun isCameraMediaDevicesSupported(): Boolean = js(
    "Boolean(navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function' && typeof navigator.mediaDevices.enumerateDevices === 'function')",
)
