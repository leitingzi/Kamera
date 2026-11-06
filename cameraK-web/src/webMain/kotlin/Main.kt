import kotlinx.browser.document
import kotlinx.browser.window

fun cameraKotlinGreeting(): String = "Hello from a Kotlin function!"

fun cameraSupportMessage(isSupported: Boolean): String = if (isSupported) {
    "Camera API is supported by this browser."
} else {
    "Camera API is not supported. Use a secure context (HTTPS or localhost)."
}

internal data class CameraWebDemoResult(val interopMessage: String, val isCameraSupported: Boolean)

internal expect fun runPlatformInteropDemo(): CameraWebDemoResult

fun main() {
    val result = runPlatformInteropDemo()
    val cameraMessage = cameraSupportMessage(result.isCameraSupported)

    document.getElementById("output")?.textContent = result.interopMessage
    document.getElementById("camera-support")?.textContent = cameraMessage
    if (result.isCameraSupported) {
        val mediaDevices = BrowserMediaDevices(window.navigator.mediaDevices)
        val constraints = mediaDevices.getSupportedConstraints()
        val constraintsMessage =
            if (constraints.isEmpty()) {
                "No supported media constraints were reported."
            } else {
                constraints.joinToString(separator = "\n")
            }
        document.getElementById("supported-constraints")?.textContent = constraintsMessage
        println("Supported media constraints:\n$constraintsMessage")

        mediaDevices.enumerateDevices(
            onSuccess = { devices ->
                val message = if (devices.isEmpty()) "No media devices are available." else devices.joinToString("\n")
                document.getElementById("media-devices")?.textContent = message
                println("Available media devices:\n$message")
            },
            onFailure = { error ->
                val message = "Could not enumerate media devices: $error"
                document.getElementById("media-devices")?.textContent = message
                println("ERROR: $message")
            },
        )
    } else {
        val message = "Media device enumeration is not supported in this browser context."
        document.getElementById("media-devices")?.textContent = message
        document.getElementById("supported-constraints")?.textContent = message
        println(message)
    }

    println(result.interopMessage)
    println(cameraMessage)
}
