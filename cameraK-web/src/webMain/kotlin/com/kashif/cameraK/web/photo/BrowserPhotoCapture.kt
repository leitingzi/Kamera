package com.kashif.cameraK.web.photo

import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.preview.BrowserCameraPreview

/** Captures a still image from an active session. */
interface BrowserPhotoCapture {
    /** Captures a photo using ImageCapture when available, otherwise the attached preview frame. */
    suspend fun capture(
        session: BrowserCameraSession,
        preview: BrowserCameraPreview,
        options: BrowserPhotoOptions = BrowserPhotoOptions(),
    ): BrowserPhotoResult
}
