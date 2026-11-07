package com.kashif.cameraK.web.device

import kotlinx.browser.window
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamConstraints
import kotlin.js.Promise

internal actual fun requestCameraStream(
    deviceId: String?,
    facingMode: String?,
    width: Int?,
    height: Int?,
    frameRate: Double?,
): Promise<MediaStream> {
    val video = js("({})")
    if (deviceId != null) {
        video.deviceId = js("({ exact: deviceId })")
    } else if (facingMode != null) {
        video.facingMode = js("({ ideal: facingMode })")
    }
    if (width != null) video.width = js("({ ideal: width })")
    if (height != null) video.height = js("({ ideal: height })")
    if (frameRate != null) video.frameRate = js("({ ideal: frameRate })")

    val constraints = js("({ audio: false, video: video })").unsafeCast<MediaStreamConstraints>()
    return window.navigator.mediaDevices.getUserMedia(constraints)
}
