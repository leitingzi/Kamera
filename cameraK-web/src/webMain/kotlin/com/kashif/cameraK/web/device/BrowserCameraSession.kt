package com.kashif.cameraK.web.device

import com.kashif.cameraK.web.error.BrowserCameraException

/**
 * Owns the camera stream and video tracks created for one camera session.
 *
 * Call [start] from a user action so the browser can show its permission prompt. [stop] releases
 * active tracks but allows the session to be started again; [close] permanently releases the
 * session and is idempotent. Preview bindings and recorders are separate resources.
 */
interface BrowserCameraSession {
    /** Latest session state. */
    val state: BrowserCameraSessionState

    /** Observes state changes. Cancel the returned subscription when the listener is no longer needed. */
    fun observeState(listener: (BrowserCameraSessionState) -> Unit): BrowserCameraSubscription

    /** Requests camera access and starts the video track. May fail with [BrowserCameraException]. */
    suspend fun start()

    /** Switches to another device or facing preference, preserving the current stream on failure. */
    suspend fun switchCamera(options: BrowserCameraOptions)

    /** Stops active camera tracks. Safe to call repeatedly. */
    suspend fun stop()

    /** Stops the session and releases all session-owned resources permanently. Safe to call repeatedly. */
    suspend fun close()
}
