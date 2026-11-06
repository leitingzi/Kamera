package com.kashif.cameraK.web.recording

import org.w3c.files.Blob

/** Final browser recording data. The caller owns the Blob; it is not a filesystem path. */
data class BrowserVideoResult(val blob: Blob, val mimeType: String, val durationMs: Long)
