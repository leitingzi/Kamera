package com.kashif.cameraK.web.preview

import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.error.BrowserCameraException
import org.w3c.dom.HTMLVideoElement

/**
 * Binds a session's stream to an HTML video element for preview.
 *
 * The preview owns only the element binding: [close] detaches the element but does not stop or
 * close the session. A browser playback rejection is reported as [BrowserCameraException].
 */
interface BrowserCameraPreview {
    /** Whether this preview is currently attached to a video element. */
    val isAttached: Boolean

    /** Attaches the active session to [videoElement] and starts playback. */
    suspend fun attach(session: BrowserCameraSession, videoElement: HTMLVideoElement)

    /** Clears the current video-element binding without stopping the camera session. */
    fun detach()

    /** Detaches the video element permanently. Safe to call repeatedly. */
    fun close()
}
