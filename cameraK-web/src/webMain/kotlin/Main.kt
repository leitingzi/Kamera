import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toList

fun cameraKotlinGreeting(): String = "Hello from a Kotlin function!"

fun cameraSupportMessage(isSupported: Boolean): String = if (isSupported) {
    "Camera API is supported by this browser."
} else {
    "Camera API is not supported. Use a secure context (HTTPS or localhost)."
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun enumerateMediaDevices() {
    window.navigator.mediaDevices.enumerateDevices()
        .then { devices ->
            val deviceDescriptions = devices.toList().map { device ->
                val label = device.label.ifBlank { "Label hidden until permission is granted" }
                "${device.kind}: $label"
            }
            val message =
                if (deviceDescriptions.isEmpty()) {
                    "No media devices are available."
                } else {
                    deviceDescriptions.joinToString(separator = "\n")
                }

            document.getElementById("media-devices")?.textContent = message
            println("Available media devices:\n$message")
            null
        }
        .catch { error ->
            val message = "Could not enumerate media devices: $error"
            document.getElementById("media-devices")?.textContent = message
            println("ERROR: $message")
            null
        }
}

internal data class CameraWebDemoResult(val interopMessage: String, val isCameraSupported: Boolean)

internal expect fun runPlatformInteropDemo(): CameraWebDemoResult

fun main() {
    val result = runPlatformInteropDemo()
    val cameraMessage = cameraSupportMessage(result.isCameraSupported)

    document.getElementById("output")?.textContent = result.interopMessage
    document.getElementById("camera-support")?.textContent = cameraMessage
    if (result.isCameraSupported) {
        enumerateMediaDevices()
    } else {
        val message = "Media device enumeration is not supported in this browser context."
        document.getElementById("media-devices")?.textContent = message
        println(message)
    }

    println(result.interopMessage)
    println(cameraMessage)
}
