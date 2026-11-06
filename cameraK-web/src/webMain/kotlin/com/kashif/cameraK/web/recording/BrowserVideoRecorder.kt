package com.kashif.cameraK.web.recording

/**
 * Owns one MediaRecorder and any microphone tracks acquired specifically for it.
 *
 * [stop] completes only after the recorder's final data-available event has been collected. The
 * returned Blob is transferred to the caller; this API does not create or own a blob URL.
 */
interface BrowserVideoRecorder {
    /** Starts recording the associated active session. */
    suspend fun start()

    /** Pauses recording when the browser recorder supports the operation. */
    suspend fun pause()

    /** Resumes a paused recording when the browser recorder supports the operation. */
    suspend fun resume()

    /** Stops recording and returns the complete recording, including the final data chunk. */
    suspend fun stop(): BrowserVideoResult

    /** Stops any active recording and releases recorder-owned resources. Safe to call repeatedly. */
    suspend fun close()
}
