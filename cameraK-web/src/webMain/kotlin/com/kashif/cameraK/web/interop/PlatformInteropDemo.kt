package com.kashif.cameraK.web.interop

internal expect fun runPlatformInteropDemo(): CameraWebDemoResult

internal expect fun isCameraSecureContext(): Boolean

internal expect fun hasMediaDevicesApi(): Boolean

internal expect fun hasGetUserMediaApi(): Boolean

internal expect fun hasEnumerateDevicesApi(): Boolean
