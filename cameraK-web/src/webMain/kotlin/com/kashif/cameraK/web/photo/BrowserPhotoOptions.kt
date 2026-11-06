package com.kashif.cameraK.web.photo

/** Still-image capture options. */
data class BrowserPhotoOptions(
    val format: BrowserPhotoFormat = BrowserPhotoFormat.JPEG,
    /** JPEG quality in the inclusive range 0.0..1.0; ignored for PNG. */
    val quality: Double? = null,
)
