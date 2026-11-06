package com.kashif.cameraK.web.device

import com.kashif.cameraK.web.interop.isCameraMediaDevicesSupported
import kotlinx.browser.window
import org.w3c.dom.mediacapture.MediaDeviceKind
import org.w3c.dom.mediacapture.VIDEOINPUT
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toList

/** Internal device-enumeration bridge used by the current demo. */
@OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserMediaDevices {
    private val mediaDevices = if (isCameraMediaDevicesSupported()) window.navigator.mediaDevices else null

    val isCameraSupported: Boolean get() = mediaDevices != null

    fun getCameraDevices(onSuccess: (List<CameraDeviceInfo>) -> Unit, onFailure: (String) -> Unit) {
        if (!isCameraSupported) {
            onFailure("Camera access is not supported by this browser.")
            return
        }

        val mediaDevices = mediaDevices ?: run {
            onFailure("Camera device access is unavailable.")
            return
        }

        mediaDevices.enumerateDevices().then { devices ->
            onSuccess(
                devices.toList().filter {
                    it.kind == MediaDeviceKind.VIDEOINPUT
                }.map { device ->
                    CameraDeviceInfo.fromDevice(device)
                },
            )
            null
        }.catch { error ->
            onFailure(error.toString())
            null
        }
    }
}
