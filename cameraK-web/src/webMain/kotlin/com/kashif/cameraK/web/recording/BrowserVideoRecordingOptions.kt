package com.kashif.cameraK.web.recording

/** Browser recording configuration. */
data class BrowserVideoRecordingOptions(
    /** Whether a microphone track should be requested and included in the recording. */
    val enableAudio: Boolean = false,
    /** Preferred MIME type; the browser may select a supported fallback. */
    val preferredMimeType: String? = null,
    /** Optional video bitrate in bits per second. */
    val videoBitsPerSecond: Int? = null,
    /** Optional recording limit in milliseconds; zero means unlimited. */
    val maxDurationMs: Long = 0L,
)
