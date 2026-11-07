package com.kashif.cameraK.web

import com.kashif.cameraK.web.device.BrowserCameraOptions
import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.device.BrowserCameraSessionEvent
import com.kashif.cameraK.web.device.BrowserCameraSessionState
import com.kashif.cameraK.web.device.BrowserCameraSubscription
import com.kashif.cameraK.web.device.BrowserMediaDevices
import com.kashif.cameraK.web.device.CameraDeviceInfo
import com.kashif.cameraK.web.interop.runPlatformInteropDemo
import com.kashif.cameraK.web.preview.BrowserCameraPreview
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLVideoElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun cameraKotlinGreeting(): String = "Hello from a Kotlin function!"

fun main() {
    val result = runPlatformInteropDemo()
    val browserMediaDevices = BrowserMediaDevices()
    val support = browserMediaDevices.support
    val cameraMessage = support.unsupportedReason ?: "Camera API is supported by this browser."
    val cameraSelect = document.getElementById("camera-select") as? HTMLSelectElement
    val cameraVideo = document.getElementById("camera-preview") as? HTMLVideoElement
    val cameraSessionMessage = document.getElementById("camera-session")
    var session: BrowserCameraSession? = null
    var deviceChanges: BrowserCameraSubscription? = null
    val preview = BrowserCameraPreview.create()
    preview.observePlaybackErrors { error ->
        cameraSessionMessage?.textContent = "Preview playback failed: ${error.message}"
    }

    document.getElementById("output")?.textContent = result.interopMessage
    document.getElementById("camera-support")?.textContent = cameraMessage

    if (support.canEnumerateDevices) {
        val refreshCameras = {
            browserMediaDevices.getCameraDevices(
                onSuccess = { devices ->
                    renderCameraDevices(devices, cameraSelect)
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
        deviceChanges = browserMediaDevices.observeDeviceChanges { refreshCameras() }
        refreshCameras()
    } else {
        document.getElementById("media-devices")?.textContent =
            support.unsupportedReason ?: "Camera device enumeration is not available."
        println(cameraMessage)
    }

    window.addEventListener(
        "pagehide",
        {
            deviceChanges?.cancel()
            preview.close()
            session?.let { activeSession -> launchCameraOperation({ activeSession.close() }, {}) }
        },
        options = true,
    )

    document.getElementById("camera-start")?.addEventListener("click") {
        val activeSession = session ?: BrowserCameraSession.create(
            BrowserCameraOptions(deviceId = cameraSelect?.value?.takeIf(String::isNotBlank)),
        ).also { created ->
            session = created
            created.observeState { state ->
                cameraSessionMessage?.textContent = "Camera state: $state"
            }
            created.observeEvents { event ->
                if (event is BrowserCameraSessionEvent.DevicesChanged) {
                    renderCameraDevices(event.devices, cameraSelect)
                }
            }
        }
        launchCameraOperation(
            block = {
                activeSession.start()
                cameraVideo?.let { preview.attach(activeSession, it) }
            },
            onResult = { error ->
                cameraSessionMessage?.textContent =
                    error?.let { "Camera or preview failed: ${it.message}" } ?: "Camera started and preview is playing."
            },
        )
    }

    document.getElementById("camera-select")?.addEventListener("change") {
        val activeSession = session ?: return@addEventListener
        if (activeSession.state is BrowserCameraSessionState.Running) {
            val options = BrowserCameraOptions(deviceId = cameraSelect?.value?.takeIf(String::isNotBlank))
            launchCameraOperation(
                block = { activeSession.switchCamera(options) },
                onResult = { error ->
                    cameraSessionMessage?.textContent =
                        error?.let { "Camera switch failed: ${it.message}" } ?: "Camera switched."
                },
            )
        }
    }

    document.getElementById("camera-stop")?.addEventListener("click") {
        val activeSession = session ?: return@addEventListener
        launchCameraOperation(
            block = { activeSession.stop() },
            onResult = { error ->
                cameraSessionMessage?.textContent =
                    error?.let { "Camera stop failed: ${it.message}" } ?: "Camera stopped."
            },
        )
    }

    document.getElementById("camera-close")?.addEventListener("click") {
        val activeSession = session ?: return@addEventListener
        launchCameraOperation(
            block = { activeSession.close() },
            onResult = { error ->
                session = null
                cameraSessionMessage?.textContent =
                    error?.let { "Camera close failed: ${it.message}" } ?: "Camera closed and tracks released."
            },
        )
    }

    println(result.interopMessage)
    println(cameraMessage)
}

private fun renderCameraDevices(devices: List<CameraDeviceInfo>, cameraSelect: HTMLSelectElement?) {
    val selectedId = cameraSelect?.value.orEmpty()
    cameraSelect?.innerHTML = ""
    val defaultOption = document.createElement("option") as HTMLOptionElement
    defaultOption.value = ""
    defaultOption.textContent = "Browser default camera"
    cameraSelect?.appendChild(defaultOption)
    devices.forEach { device ->
        val option = document.createElement("option") as HTMLOptionElement
        option.value = device.deviceId
        option.textContent = device.label.ifBlank { "Camera (${device.deviceId.take(8)})" }
        cameraSelect?.appendChild(option)
    }
    cameraSelect?.value = selectedId

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
}

private fun launchCameraOperation(
    block: suspend () -> Unit,
    onResult: (Throwable?) -> Unit,
) {
    block.startCoroutine(
        object : Continuation<Unit> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                onResult(result.exceptionOrNull())
            }
        },
    )
}
