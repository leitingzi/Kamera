package com.kashif.cameraK.web.device

import org.w3c.dom.mediacapture.MediaStream
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(deviceId, facingMode, width, height, frameRate) => {
        const video = {};
        if (deviceId != null) video.deviceId = { exact: deviceId };
        else if (facingMode != null) video.facingMode = { ideal: facingMode };
        if (width != null) video.width = { ideal: width };
        if (height != null) video.height = { ideal: height };
        if (frameRate != null) video.frameRate = { ideal: frameRate };
        return globalThis.navigator.mediaDevices.getUserMedia({ audio: false, video });
    }""",
)
private external fun requestCameraStreamImpl(
    deviceId: String?,
    facingMode: String?,
    width: Int?,
    height: Int?,
    frameRate: Double?,
): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun requestCameraStream(
    deviceId: String?,
    facingMode: String?,
    width: Int?,
    height: Int?,
    frameRate: Double?,
): Promise<MediaStream> = requestCameraStreamImpl(deviceId, facingMode, width, height, frameRate).unsafeCast()
