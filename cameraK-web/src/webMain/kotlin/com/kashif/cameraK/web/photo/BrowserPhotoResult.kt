package com.kashif.cameraK.web.photo

import org.w3c.files.Blob

/** Still image data returned by [BrowserPhotoCapture.capture]. The caller owns the returned Blob. */
data class BrowserPhotoResult(
    val blob: Blob,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val source: BrowserPhotoSource,
)
