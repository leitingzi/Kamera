package com.kashif.cameraK.web

import com.kashif.cameraK.web.device.BrowserMediaDevices
import com.kashif.cameraK.web.interop.runPlatformInteropDemo
import kotlinx.browser.document
import kotlinx.browser.window

fun cameraKotlinGreeting(): String = "Hello from a Kotlin function!"

fun main() {
    val result = runPlatformInteropDemo()
    val browserMediaDevices = BrowserMediaDevices()
    val support = browserMediaDevices.support
    val cameraMessage = support.unsupportedReason ?: "Camera API is supported by this browser."

    document.getElementById("output")?.textContent = result.interopMessage
    document.getElementById("camera-support")?.textContent = cameraMessage

    if (support.canEnumerateDevices) {
        val refreshCameras = {
            browserMediaDevices.getCameraDevices(
                onSuccess = { devices ->
                    val message =
                        if (devices.isEmpty()) {
                            "No video input devices are currently visible to this page."
                        } else {
                            devices.mapIndexed { index, device ->
                                val label = device.label.ifBlank { "Camera label hidden until permission is granted" }
                                "${index + 1}. $label"
                            }.joinToString("\n")
                        }
                    document.getElementById("media-devices")?.textContent = message
                    println("Available cameras:\n$message")
                },
                onFailure = { error ->
                    val message = buildString {
                        append("Could not enumerate cameras [${error.code}]")
                        error.browserErrorName?.let { append(" ($it)") }
                        error.constraint?.let { append(" [constraint: $it]") }
                        append(": ${error.message}")
                    }
                    document.getElementById("media-devices")?.textContent = message
                    println("ERROR: $message")
                },
            )
        }
        val deviceChanges = browserMediaDevices.observeDeviceChanges { refreshCameras() }
        window.addEventListener("pagehide", { deviceChanges.cancel() }, options = true)
        refreshCameras()
    } else {
        document.getElementById("media-devices")?.textContent =
            support.unsupportedReason ?: "Camera device enumeration is not available."
        println(cameraMessage)
    }

    println(result.interopMessage)
    println(cameraMessage)
}
