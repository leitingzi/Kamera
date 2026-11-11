package com.kashif.cameraK.web

import com.kashif.cameraK.web.device.BrowserCameraOptions
import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.device.BrowserCameraSessionEvent
import com.kashif.cameraK.web.device.BrowserCameraSessionState
import com.kashif.cameraK.web.device.BrowserCameraSubscription
import com.kashif.cameraK.web.device.BrowserMediaDevices
import com.kashif.cameraK.web.device.CameraDeviceInfo
import com.kashif.cameraK.web.error.BrowserCameraException
import com.kashif.cameraK.web.interop.runPlatformInteropDemo
import com.kashif.cameraK.web.photo.BrowserPhotoCapture
import com.kashif.cameraK.web.photo.BrowserPhotoFormat
import com.kashif.cameraK.web.photo.BrowserPhotoOptions
import com.kashif.cameraK.web.photo.BrowserPhotoResult
import com.kashif.cameraK.web.preview.BrowserCameraPreview
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.url.URL
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
    val previewPlaceholder = document.getElementById("preview-placeholder")
    val startButton = document.getElementById("camera-start") as? HTMLButtonElement
    val stopButton = document.getElementById("camera-stop") as? HTMLButtonElement
    val closeButton = document.getElementById("camera-close") as? HTMLButtonElement
    val photoCaptureButton = document.getElementById("photo-capture") as? HTMLButtonElement
    val photoFormatSelect = document.getElementById("photo-format") as? HTMLSelectElement
    val photoStatus = document.getElementById("photo-status")
    val photoStatusMessage = photoStatus?.querySelector("p")
    val photoResultImage = document.getElementById("photo-result-image") as? HTMLImageElement
    val photoResultInfo = document.getElementById("photo-result-info")
    val photoDownload = document.getElementById("photo-download") as? HTMLAnchorElement
    val cameraSessionMessage = document.getElementById("camera-session")
    val cameraStatus = document.getElementById("camera-status")
    val cameraSelectHint = document.getElementById("camera-select-hint")
    var session: BrowserCameraSession? = null
    var deviceChanges: BrowserCameraSubscription? = null
    var selectedCameraId = cameraSelect?.value.orEmpty()
    var photoCaptureInProgress = false
    var photoObjectUrl: String? = null
    val preview = BrowserCameraPreview.create()
    val photoCapture = BrowserPhotoCapture.create()

    fun showStatus(message: String, hasError: Boolean = false) {
        cameraSessionMessage?.textContent = message
        cameraStatus?.setAttribute("data-state", if (hasError) "error" else "idle")
    }

    fun showPhotoStatus(message: String, hasError: Boolean = false) {
        photoStatusMessage?.textContent = message
        photoStatus?.setAttribute("data-state", if (hasError) "error" else "idle")
    }

    fun releasePhotoObjectUrl() {
        photoObjectUrl?.let { URL.revokeObjectURL(it) }
        photoObjectUrl = null
        photoResultImage?.removeAttribute("src")
        photoResultImage?.hidden = true
        photoResultInfo?.textContent = "No photo captured yet."
        photoDownload?.removeAttribute("href")
        photoDownload?.removeAttribute("download")
        photoDownload?.hidden = true
    }

    fun showCapturedPhoto(result: BrowserPhotoResult) {
        val nextUrl = URL.createObjectURL(result.blob)
        try {
            photoResultImage?.src = nextUrl
            photoResultImage?.hidden = false
            photoResultInfo?.textContent =
                "${result.width} × ${result.height} · ${result.mimeType} · " +
                    if (result.usedPreviewFrameFallback) "Preview-frame fallback" else "ImageCapture"
            photoDownload?.href = nextUrl
            photoDownload?.download =
                "camerak-photo.${if (result.mimeType == BrowserPhotoFormat.PNG.mimeType) "png" else "jpg"}"
            photoDownload?.hidden = false
        } catch (error: Throwable) {
            URL.revokeObjectURL(nextUrl)
            throw error
        }
        photoObjectUrl?.let { URL.revokeObjectURL(it) }
        photoObjectUrl = nextUrl
    }

    fun updateControls() {
        val cameraState = session?.state
        val isRunning = cameraState is BrowserCameraSessionState.Running
        val isStarting = cameraState is BrowserCameraSessionState.Starting
        cameraSelect?.disabled = session != null
        startButton?.disabled = isRunning || isStarting
        stopButton?.disabled = !isRunning
        closeButton?.disabled = session == null || isStarting
        photoCaptureButton?.disabled = !isRunning || !preview.isAttached || photoCaptureInProgress
        photoFormatSelect?.disabled = photoCaptureInProgress
        cameraStatus?.setAttribute(
            "data-state",
            when (cameraState) {
                is BrowserCameraSessionState.Running -> "running"
                is BrowserCameraSessionState.Failed -> "error"
                else -> "idle"
            },
        )
        cameraSelectHint?.textContent =
            if (session == null) {
                "Camera names may be hidden until you grant permission."
            } else {
                "Close the camera before choosing another device."
            }
        if (isRunning && preview.isAttached && preview.playbackError == null) {
            previewPlaceholder?.setAttribute("hidden", "")
        } else {
            previewPlaceholder?.removeAttribute("hidden")
        }
    }

    fun showSessionState(state: BrowserCameraSessionState) {
        when (state) {
            BrowserCameraSessionState.Idle -> showStatus("Choose a camera and open it to begin.")
            BrowserCameraSessionState.Starting -> showStatus("Requesting camera access…")
            is BrowserCameraSessionState.Running ->
                showStatus("Camera is on. Close it before choosing another camera.")
            BrowserCameraSessionState.Stopped ->
                showStatus("Camera stopped. Close it to choose another device.")
            BrowserCameraSessionState.Closed -> showStatus("Camera closed. Choose a camera to continue.")
            is BrowserCameraSessionState.Failed -> showStatus("Camera error: ${state.error.message}", hasError = true)
        }
        updateControls()
    }

    preview.observePlaybackErrors { error ->
        showStatus("Preview playback failed: ${error.message}", hasError = true)
        updateControls()
    }

    document.getElementById("output")?.textContent = result.interopMessage
    document.getElementById("camera-support")?.textContent = cameraMessage
    updateControls()

    if (support.canEnumerateDevices) {
        val refreshCameras = {
            browserMediaDevices.getCameraDevices(
                onSuccess = { devices ->
                    renderCameraDevices(devices, cameraSelect)
                    if (session == null) selectedCameraId = cameraSelect?.value.orEmpty()
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
            releasePhotoObjectUrl()
            session?.let { activeSession ->
                launchCameraOperation({ activeSession.close() }) { _, _ -> }
            }
        },
        options = true,
    )

    document.getElementById("camera-start")?.addEventListener("click") {
        if (session?.state is BrowserCameraSessionState.Running ||
            session?.state is BrowserCameraSessionState.Starting
        ) {
            return@addEventListener
        }
        val activeSession = session ?: BrowserCameraSession.create(
            BrowserCameraOptions(deviceId = selectedCameraId.takeIf(String::isNotBlank)),
        ).also { created ->
            session = created
            created.observeState { state ->
                showSessionState(state)
            }
            created.observeEvents { event ->
                if (event is BrowserCameraSessionEvent.DevicesChanged) {
                    renderCameraDevices(event.devices, cameraSelect)
                }
            }
            updateControls()
        }
        launchCameraOperation(
            block = {
                activeSession.start()
                cameraVideo?.let { preview.attach(activeSession, it) }
            },
            onResult = { _, error ->
                if (error != null) {
                    showStatus("Camera or preview failed: ${error.message}", hasError = true)
                } else {
                    updateControls()
                }
            },
        )
    }

    document.getElementById("photo-capture")?.addEventListener("click") {
        val activeSession = session ?: return@addEventListener
        if (photoCaptureInProgress || activeSession.state !is BrowserCameraSessionState.Running ||
            !preview.isAttached
        ) {
            return@addEventListener
        }

        val format =
            if (photoFormatSelect?.value == "png") BrowserPhotoFormat.PNG else BrowserPhotoFormat.JPEG
        photoCaptureInProgress = true
        showPhotoStatus("Capturing photo…")
        updateControls()
        launchCameraOperation(
            block = {
                photoCapture.capture(
                    session = activeSession,
                    preview = preview,
                    options = BrowserPhotoOptions(format = format),
                )
            },
            onResult = { result, error ->
                photoCaptureInProgress = false
                when {
                    error != null -> {
                        val detail = if (error is BrowserCameraException) {
                            buildString {
                                append("[${error.code}] ")
                                error.browserErrorName?.let { append("$it: ") }
                                append(error.message)
                            }
                        } else {
                            error.message ?: "An unexpected error occurred."
                        }
                        showPhotoStatus("Photo capture failed: $detail", hasError = true)
                    }

                    result == null -> {
                        showPhotoStatus("Photo capture returned no result.", hasError = true)
                    }

                    else -> try {
                        showCapturedPhoto(result)
                        showPhotoStatus(
                            "Photo captured: ${result.width} × ${result.height} ${result.mimeType}.",
                        )
                    } catch (displayError: Throwable) {
                        showPhotoStatus(
                            "Photo captured but could not be displayed: " +
                                (displayError.message ?: "unknown display error"),
                            hasError = true,
                        )
                    }
                }
                updateControls()
            },
        )
    }

    document.getElementById("camera-select")?.addEventListener("change") {
        if (session != null) {
            cameraSelect?.value = selectedCameraId
            showStatus("Close the camera before choosing another device.")
            return@addEventListener
        }
        selectedCameraId = cameraSelect?.value.orEmpty()
        showStatus("Camera selected. Open it when you're ready.")
    }

    document.getElementById("camera-stop")?.addEventListener("click") {
        val activeSession = session ?: return@addEventListener
        launchCameraOperation(
            block = { activeSession.stop() },
            onResult = { _, error ->
                if (error != null) showStatus("Camera stop failed: ${error.message}", hasError = true)
            },
        )
    }

    document.getElementById("camera-close")?.addEventListener("click") {
        val activeSession = session ?: return@addEventListener
        launchCameraOperation(
            block = { activeSession.close() },
            onResult = { _, error ->
                if (error != null) {
                    showStatus("Camera close failed: ${error.message}", hasError = true)
                } else {
                    session = null
                    showStatus("Camera closed. Choose a camera to continue.")
                    updateControls()
                }
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

private fun <T> launchCameraOperation(block: suspend () -> T, onResult: (T?, Throwable?) -> Unit) {
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                onResult(result.getOrNull(), result.exceptionOrNull())
            }
        },
    )
}
