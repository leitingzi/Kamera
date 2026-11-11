package com.kashif.cameraK.web.photo

import kotlinx.browser.document
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.mediacapture.MediaStreamTrack
import org.w3c.files.Blob
import kotlin.js.JsName
import kotlin.js.Promise
import kotlin.js.asDynamic
import kotlin.js.unsafeCast

@JsName("ImageCapture")
private external class NativeImageCapture(track: MediaStreamTrack) {
    fun takePhoto(): Promise<Blob>
}

internal actual fun supportsImageCapture(): Boolean =
    js("typeof globalThis.ImageCapture === 'function'") as Boolean &&
        js("typeof globalThis.ImageCapture.prototype.takePhoto === 'function'") as Boolean

internal actual fun readPhotoMimeType(blob: Blob): String = blob.asDynamic().type as String

internal actual fun readPhotoSize(blob: Blob): Double = (blob.asDynamic().size as Number).toDouble()

internal actual fun isVideoTrackLive(track: MediaStreamTrack): Boolean =
    track.asDynamic().readyState == "live"

internal actual fun takeImageCapturePhoto(track: MediaStreamTrack): Promise<Blob> =
    Promise { resolve, reject ->
        try {
            NativeImageCapture(track).takePhoto().then { blob ->
                resolve(blob)
                null
            }.catch { error ->
                reject(error)
                null
            }
        } catch (error: Throwable) {
            reject(error)
        }
    }

internal actual fun capturePreviewFrame(
    video: HTMLVideoElement,
    mimeType: String,
    quality: Double?,
): Promise<Blob> {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    val context = canvas.getContext("2d") as? CanvasRenderingContext2D
        ?: throw IllegalStateException("A 2D canvas context is unavailable.")
    context.drawImage(video, 0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())

    return Promise { resolve, reject ->
        canvas.toBlob({ blob ->
            canvas.width = 0
            canvas.height = 0
            if (blob == null) reject(IllegalStateException("Canvas photo encoding returned no Blob."))
            else resolve(blob)
        }, mimeType, quality)
    }
}

internal actual fun readPhotoDimensions(
    blob: Blob,
    fallbackWidth: Int,
    fallbackHeight: Int,
): Promise<BrowserPhotoDimensions> {
    if (js("typeof globalThis.createImageBitmap === 'function'") as Boolean) {
        val bitmapPromise: dynamic = js("globalThis.createImageBitmap(blob)")
        return bitmapPromise.then { bitmap ->
            try {
                val dimensions = js("({})")
                dimensions.width = bitmap.width
                dimensions.height = bitmap.height
                dimensions.unsafeCast<BrowserPhotoDimensions>()
            } finally {
                bitmap.close()
            }
        }.unsafeCast<Promise<BrowserPhotoDimensions>>()
    }

    val dimensions = js("({})")
    dimensions.width = fallbackWidth
    dimensions.height = fallbackHeight
    return js("Promise.resolve(dimensions)").unsafeCast<Promise<BrowserPhotoDimensions>>()
}
