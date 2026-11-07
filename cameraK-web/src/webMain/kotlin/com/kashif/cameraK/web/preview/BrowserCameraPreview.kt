package com.kashif.cameraK.web.preview

import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.device.BrowserCameraSubscription
import com.kashif.cameraK.web.error.BrowserCameraException
import org.w3c.dom.HTMLVideoElement

/**
 * Binds a session's stream to an HTML video element for preview.
 *
 * The preview owns only the element binding: [close] detaches the element but does not stop or
 * close the session. A browser playback rejection during [attach] is reported as
 * [BrowserCameraException]. Later playback failures, including those after a camera switch, are
 * available through [playbackError] and [observePlaybackErrors].
 */
interface BrowserCameraPreview {
    companion object {
        /** Creates a preview binder. The caller must close it when its UI is torn down. */
        fun create(): BrowserCameraPreview = BrowserCameraPreviewImpl()
    }

    /** Whether this preview is currently attached to a video element. */
    val isAttached: Boolean

    /** Most recent playback error, cleared after playback successfully starts. */
    val playbackError: BrowserCameraException?

    /**
     * Attaches the active session to [videoElement], sets it muted and inline, and waits for
     * [HTMLVideoElement.play] to succeed. The preview follows session stream changes and detaches
     * when the session stops, closes, or fails.
     */
    suspend fun attach(session: BrowserCameraSession, videoElement: HTMLVideoElement)

    /**
     * Observes playback errors that occur after [attach] completes, such as a browser rejection
     * while switching cameras. Cancel when no longer needed.
     */
    fun observePlaybackErrors(listener: (BrowserCameraException) -> Unit): BrowserCameraSubscription

    /** Clears the current video-element binding without stopping the camera session. */
    fun detach()

    /** Detaches the video element permanently. Safe to call repeatedly. */
    fun close()
}
