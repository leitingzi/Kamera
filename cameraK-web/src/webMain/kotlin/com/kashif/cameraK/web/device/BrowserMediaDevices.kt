package com.kashif.cameraK.web.device

import com.kashif.cameraK.web.error.BrowserCameraErrorCode
import com.kashif.cameraK.web.error.BrowserCameraException
import com.kashif.cameraK.web.error.toBrowserCameraException
import com.kashif.cameraK.web.interop.hasEnumerateDevicesApi
import com.kashif.cameraK.web.interop.hasGetUserMediaApi
import com.kashif.cameraK.web.interop.hasMediaDevicesApi
import com.kashif.cameraK.web.interop.isCameraSecureContext
import kotlinx.browser.window
import org.w3c.dom.mediacapture.MediaDeviceKind
import org.w3c.dom.mediacapture.VIDEOINPUT
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toList

/** Internal device-enumeration bridge used by the current demo. */
@OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserMediaDevices {
    val support = BrowserCameraSupport(
        isSecureContext = isCameraSecureContext(),
        hasMediaDevices = hasMediaDevicesApi(),
        hasGetUserMedia = hasGetUserMediaApi(),
        hasEnumerateDevices = hasEnumerateDevicesApi(),
    )

    private val mediaDevices = if (support.hasMediaDevices) window.navigator.mediaDevices else null

    /**
     * Enumerates video inputs. Calling this method again explicitly refreshes the list; labels may
     * remain blank until camera permission is granted.
     */
    fun getCameraDevices(onSuccess: (List<CameraDeviceInfo>) -> Unit, onFailure: (BrowserCameraException) -> Unit) {
        if (!support.canEnumerateDevices) {
            onFailure(
                BrowserCameraException(
                    code = BrowserCameraErrorCode.UNSUPPORTED,
                    message = requireNotNull(support.enumerationUnsupportedReason),
                ),
            )
            return
        }

        val mediaDevices = mediaDevices ?: run {
            onFailure(
                BrowserCameraException(
                    code = BrowserCameraErrorCode.UNSUPPORTED,
                    message = "Camera device access is unavailable.",
                ),
            )
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
            onFailure(error.toBrowserCameraException())
            null
        }
    }

    /** Subscribes to hot-plug changes; cancel the returned subscription to remove this listener. */
    fun observeDeviceChanges(listener: () -> Unit): BrowserCameraSubscription {
        if (!support.canEnumerateDevices) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.UNSUPPORTED,
                message = requireNotNull(support.enumerationUnsupportedReason),
            )
        }

        val mediaDevices = mediaDevices ?: throw BrowserCameraException(
            code = BrowserCameraErrorCode.UNSUPPORTED,
            message = "Camera device access is unavailable.",
        )
        val eventListener: (org.w3c.dom.events.Event) -> Unit = { listener() }
        mediaDevices.addEventListener("devicechange", eventListener)

        var isCancelled = false
        return BrowserCameraSubscription {
            if (!isCancelled) {
                mediaDevices.removeEventListener("devicechange", eventListener)
                isCancelled = true
            }
        }
    }
}
