package com.kashif.cameraK.web.photo

import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.mediacapture.MediaStreamTrack
import org.w3c.files.Blob
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal external interface BrowserPhotoDimensions : JsAny {
    val width: Int
    val height: Int
}

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun supportsImageCapture(): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun readPhotoMimeType(blob: Blob): String

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun readPhotoSize(blob: Blob): Double

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun isVideoTrackLive(track: MediaStreamTrack): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun takeImageCapturePhoto(track: MediaStreamTrack): Promise<Blob>

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun capturePreviewFrame(
    video: HTMLVideoElement,
    mimeType: String,
    quality: Double?,
): Promise<Blob>

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun readPhotoDimensions(
    blob: Blob,
    fallbackWidth: Int,
    fallbackHeight: Int,
): Promise<BrowserPhotoDimensions>
