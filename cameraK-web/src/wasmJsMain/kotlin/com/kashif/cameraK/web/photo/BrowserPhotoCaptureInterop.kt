package com.kashif.cameraK.web.photo

import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.mediacapture.MediaStreamTrack
import org.w3c.files.Blob
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
@kotlin.JsFun("() => typeof globalThis.ImageCapture === 'function'")
private external fun hasImageCaptureConstructor(): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@kotlin.JsFun("() => typeof globalThis.ImageCapture.prototype.takePhoto === 'function'")
private external fun hasImageCaptureTakePhoto(): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@kotlin.JsFun("(blob) => blob.type")
private external fun readPhotoMimeTypeImpl(blob: JsAny): String

@OptIn(ExperimentalWasmJsInterop::class)
@kotlin.JsFun("(blob) => blob.size")
private external fun readPhotoSizeImpl(blob: JsAny): Double

@OptIn(ExperimentalWasmJsInterop::class)
@kotlin.JsFun("(track) => track.readyState === 'live'")
private external fun isVideoTrackLiveImpl(track: JsAny): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@kotlin.JsFun("(track) => Promise.resolve().then(() => new globalThis.ImageCapture(track).takePhoto())")
private external fun takeImageCapturePhotoImpl(track: JsAny): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@kotlin.JsFun(
    """(video, mimeType, quality) => new Promise((resolve, reject) => {
        let canvas = null;
        const releaseCanvas = () => {
            if (canvas) {
                canvas.width = 0;
                canvas.height = 0;
                canvas = null;
            }
        };
        try {
            canvas = globalThis.document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            const context = canvas.getContext('2d');
            if (!context) {
                releaseCanvas();
                reject(new DOMException('A 2D canvas context is unavailable.', 'NotSupportedError'));
                return;
            }
            context.drawImage(video, 0, 0, canvas.width, canvas.height);
            canvas.toBlob((blob) => {
                releaseCanvas();
                if (blob) resolve(blob);
                else reject(new DOMException('Canvas photo encoding returned no Blob.', 'EncodingError'));
            }, mimeType, quality == null ? undefined : quality);
        } catch (error) {
            releaseCanvas();
            reject(error);
        }
    })""",
)
private external fun capturePreviewFrameImpl(video: JsAny, mimeType: String, quality: Double?): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@kotlin.JsFun(
    """(blob, fallbackWidth, fallbackHeight) => typeof globalThis.createImageBitmap === 'function'
        ? globalThis.createImageBitmap(blob).then((bitmap) => {
            try { return { width: bitmap.width, height: bitmap.height }; }
            finally { bitmap.close(); }
        })
        : Promise.resolve({ width: fallbackWidth, height: fallbackHeight })""",
)
private external fun readPhotoDimensionsImpl(blob: JsAny, fallbackWidth: Int, fallbackHeight: Int): JsAny

internal actual fun supportsImageCapture(): Boolean =
    hasImageCaptureConstructor() && hasImageCaptureTakePhoto()

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun readPhotoMimeType(blob: Blob): String = readPhotoMimeTypeImpl(blob.unsafeCast())

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun readPhotoSize(blob: Blob): Double = readPhotoSizeImpl(blob.unsafeCast())

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun isVideoTrackLive(track: MediaStreamTrack): Boolean =
    isVideoTrackLiveImpl(track.unsafeCast())

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun takeImageCapturePhoto(track: MediaStreamTrack): Promise<Blob> =
    takeImageCapturePhotoImpl(track.unsafeCast()).unsafeCast()

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun capturePreviewFrame(
    video: HTMLVideoElement,
    mimeType: String,
    quality: Double?,
): Promise<Blob> = capturePreviewFrameImpl(video.unsafeCast(), mimeType, quality).unsafeCast()

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun readPhotoDimensions(
    blob: Blob,
    fallbackWidth: Int,
    fallbackHeight: Int,
): Promise<BrowserPhotoDimensions> =
    readPhotoDimensionsImpl(blob.unsafeCast(), fallbackWidth, fallbackHeight).unsafeCast()
