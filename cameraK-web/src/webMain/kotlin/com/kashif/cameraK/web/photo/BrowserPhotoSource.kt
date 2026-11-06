package com.kashif.cameraK.web.photo

/** Which browser path produced a still image. */
enum class BrowserPhotoSource {
    /** Captured directly from the camera through the optional Image Capture API. */
    IMAGE_CAPTURE,

    /** Captured from the currently displayed video frame through Canvas. */
    PREVIEW_FRAME,
}
