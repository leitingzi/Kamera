package com.kashif.cameraK.web.photo

import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.preview.BrowserCameraPreview

/** Captures a still image from an active session. */
interface BrowserPhotoCapture {
    companion object {
        /** Creates a still-photo service. Each capture returns a caller-owned Blob. */
        fun create(): BrowserPhotoCapture = BrowserPhotoCaptureImpl()
    }

    /**
     * Captures a still image from [session]. JPEG uses the browser's ImageCapture API when
     * available; PNG and requests with an explicit JPEG quality use the attached preview frame so
     * the requested encoding is honored. If ImageCapture is unavailable, the preview frame is used.
     * The preview must be attached to [session], and the caller owns the returned Blob.
     */
    suspend fun capture(
        session: BrowserCameraSession,
        preview: BrowserCameraPreview,
        options: BrowserPhotoOptions = BrowserPhotoOptions(),
    ): BrowserPhotoResult
}
