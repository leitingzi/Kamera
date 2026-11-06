package com.kashif.cameraK.web

import com.kashif.cameraK.web.device.BrowserMediaDevices
import com.kashif.cameraK.web.interop.runPlatformInteropDemo
import kotlinx.browser.document

fun cameraKotlinGreeting(): String = "Hello from a Kotlin function!"

fun cameraSupportMessage(isSupported: Boolean): String = if (isSupported) {
    "Camera API is supported by this browser."
} else {
    "Camera API is not supported. Use a secure context (HTTPS or localhost)."
}

fun main() {
    val result = runPlatformInteropDemo()
    val browserMediaDevices = BrowserMediaDevices()
    val cameraMessage = cameraSupportMessage(browserMediaDevices.isCameraSupported)

    document.getElementById("output")?.textContent = result.interopMessage
    document.getElementById("camera-support")?.textContent = cameraMessage

    if (browserMediaDevices.isCameraSupported) {
        browserMediaDevices.getCameraDevices(
            onSuccess = { devices ->
                val message =
                    if (devices.isEmpty()) {
                        "No cameras are available. Grant camera permission and try again if a camera is connected."
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
                val message = "Could not enumerate cameras: $error"
                document.getElementById("media-devices")?.textContent = message
                println("ERROR: $message")
            },
        )
    } else {
        val message = "Camera access is not supported in this browser context."
        document.getElementById("media-devices")?.textContent = message
        println(message)
    }

    println(result.interopMessage)
    println(cameraMessage)
}
