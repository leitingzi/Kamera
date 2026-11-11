package com.kashif.cameraK.web.photo

import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.device.BrowserCameraSessionState
import com.kashif.cameraK.web.error.BrowserCameraErrorCode
import com.kashif.cameraK.web.error.BrowserCameraException
import com.kashif.cameraK.web.error.toBrowserCameraException
import com.kashif.cameraK.web.preview.BrowserCameraPreview
import com.kashif.cameraK.web.preview.BrowserCameraPreviewCaptureSource
import org.w3c.dom.HTMLVideoElement
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserPhotoCaptureImpl : BrowserPhotoCapture {
    override suspend fun capture(
        session: BrowserCameraSession,
        preview: BrowserCameraPreview,
        options: BrowserPhotoOptions,
    ): BrowserPhotoResult {
        validateOptions(options)

        val track = session.videoTrack
        if (session.state !is BrowserCameraSessionState.Running || session.mediaStream == null ||
            track == null || !isVideoTrackLive(track)
        ) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_STATE,
                message = "Photo capture requires a running camera session with a live video track.",
            )
        }

        val previewSource = preview as? BrowserCameraPreviewCaptureSource
        if (!preview.isAttached || previewSource?.captureSession !== session) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_STATE,
                message = "Attach the preview to this camera session before capturing a photo.",
            )
        }
        val video = previewSource.captureVideoElement ?: throw BrowserCameraException(
            code = BrowserCameraErrorCode.INVALID_STATE,
            message = "The attached camera preview has no video element.",
        )

        val canUseImageCapture = options.format == BrowserPhotoFormat.JPEG && options.quality == null &&
            supportsImageCapture()
        var usedImageCapture = false
        val blob = if (canUseImageCapture) {
            try {
                takeImageCapturePhoto(track).awaitPhotoBlob().also { usedImageCapture = true }
            } catch (error: Throwable) {
                val captureError = error.asPhotoCaptureFailure()
                if (captureError.browserErrorName !in setOf("NotSupportedError", "NotImplementedError")) {
                    throw captureError
                }
                captureFromPreview(video, options)
            }
        } else {
            captureFromPreview(video, options)
        }

        if (readPhotoSize(blob) <= 0.0) {
            throw captureFailure("The browser returned an empty photo Blob.")
        }

        val mimeType = readPhotoMimeType(blob).lowercase()
        if (mimeType != options.format.mimeType) {
            throw captureFailure(
                "The browser encoded ${mimeType.ifBlank { "an unknown type" }} instead of ${options.format.mimeType}.",
            )
        }

        val dimensions = try {
            readPhotoDimensions(blob, video.videoWidth, video.videoHeight).awaitPhotoDimensions()
        } catch (error: Throwable) {
            throw error.asPhotoCaptureFailure()
        }
        if (dimensions.width <= 0 || dimensions.height <= 0) {
            throw captureFailure("The captured photo has invalid dimensions.")
        }

        return BrowserPhotoResult(
            blob = blob,
            mimeType = mimeType,
            width = dimensions.width,
            height = dimensions.height,
            source = if (usedImageCapture) BrowserPhotoSource.IMAGE_CAPTURE else BrowserPhotoSource.PREVIEW_FRAME,
        )
    }

    private suspend fun captureFromPreview(
        video: HTMLVideoElement,
        options: BrowserPhotoOptions,
    ): Blob {
        if (video.readyState < 2 || video.videoWidth <= 0 || video.videoHeight <= 0) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_STATE,
                message = "The camera preview has no decoded video frame to capture.",
            )
        }

        val blob = try {
            capturePreviewFrame(
                video = video,
                mimeType = options.format.mimeType,
                quality = options.quality.takeIf { options.format == BrowserPhotoFormat.JPEG },
            ).awaitPhotoBlob()
        } catch (error: Throwable) {
            throw error.asPhotoCaptureFailure()
        }
        return blob
    }

    private fun validateOptions(options: BrowserPhotoOptions) {
        if (options.format != BrowserPhotoFormat.JPEG) return
        val quality = options.quality ?: return
        if (!quality.isFinite() || quality !in 0.0..1.0) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_ARGUMENT,
                message = "JPEG quality must be a finite value between 0.0 and 1.0.",
            )
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun Promise<Blob>.awaitPhotoBlob(): Blob = suspendCoroutine { continuation ->
    then { blob ->
        continuation.resume(blob)
        null
    }.catch { error ->
        continuation.resumeWithException(error.toBrowserCameraException().asPhotoCaptureFailure())
        null
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun Promise<BrowserPhotoDimensions>.awaitPhotoDimensions(): BrowserPhotoDimensions =
    suspendCoroutine { continuation ->
    then { dimensions ->
        continuation.resume(dimensions)
        null
    }.catch { error ->
        continuation.resumeWithException(error.toBrowserCameraException().asPhotoCaptureFailure())
        null
    }
}

private fun Throwable.asPhotoCaptureFailure(): BrowserCameraException = when (this) {
    is BrowserCameraException -> if (code == BrowserCameraErrorCode.CAPTURE_FAILED) {
        this
    } else {
        BrowserCameraException(
            code = BrowserCameraErrorCode.CAPTURE_FAILED,
            message = message,
            browserErrorName = browserErrorName,
            constraint = constraint,
            cause = this,
        )
    }

    else -> captureFailure(message?.takeIf(String::isNotBlank) ?: "The browser could not capture a photo.", this)
}

private fun captureFailure(message: String, cause: Throwable? = null): BrowserCameraException =
    BrowserCameraException(
        code = BrowserCameraErrorCode.CAPTURE_FAILED,
        message = message,
        cause = cause,
    )
