package com.kashif.cameraK.web.interop

import com.kashif.cameraK.web.cameraKotlinGreeting
import kotlin.JsFun
import kotlin.js.ExperimentalWasmJsInterop

internal actual fun runPlatformInteropDemo(): CameraWebDemoResult = CameraWebDemoResult(
    interopMessage = cameraKotlinGreeting(),
)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(feature) => { const md = globalThis.navigator && navigator.mediaDevices; if (feature === 'secure') return globalThis.isSecureContext === true; if (feature === 'mediaDevices') return !!md; if (feature === 'getUserMedia') return !!md && typeof md.getUserMedia === 'function'; return !!md && typeof md.enumerateDevices === 'function'; }",
)
private external fun hasCameraFeatureImpl(feature: String): Boolean

internal actual fun isCameraSecureContext(): Boolean = hasCameraFeatureImpl("secure")

internal actual fun hasMediaDevicesApi(): Boolean = hasCameraFeatureImpl("mediaDevices")

internal actual fun hasGetUserMediaApi(): Boolean = hasCameraFeatureImpl("getUserMedia")

internal actual fun hasEnumerateDevicesApi(): Boolean = hasCameraFeatureImpl("enumerateDevices")
