package com.kashif.cameraK.web.api

import com.kashif.cameraK.web.device.BrowserCameraOptions
import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.device.BrowserCameraSubscription
import com.kashif.cameraK.web.device.CameraDeviceInfo
import com.kashif.cameraK.web.photo.BrowserPhotoCapture
import com.kashif.cameraK.web.preview.BrowserCameraPreview
import com.kashif.cameraK.web.recording.BrowserVideoRecorder
import com.kashif.cameraK.web.recording.BrowserVideoRecordingOptions

/**
 * Browser-only camera facade for applications that use the standalone cameraK-web module.
 *
 * This API deliberately does not depend on CameraK core types. It provides browser-specific device,
 * session, preview, photo, and recording boundaries for a later core adapter.
 */
interface BrowserCameraApi {
    /** Whether the current browser context exposes the required media-device APIs. */
    val isSupported: Boolean

    /**
     * Enumerates video input devices. Before permission is granted, labels and non-default devices
     * may be unavailable.
     */
    suspend fun enumerateCameras(): List<CameraDeviceInfo>

    /**
     * Observes changes to the available device list. The listener should call [enumerateCameras]
     * to obtain the updated list. The returned subscription owns only this listener.
     */
    fun observeDeviceChanges(listener: () -> Unit): BrowserCameraSubscription

    /**
     * Creates a session; camera access and the permission prompt begin only when [BrowserCameraSession.start]
     * is called.
     */
    fun createSession(options: BrowserCameraOptions = BrowserCameraOptions()): BrowserCameraSession

    /** Creates a preview binder. A preview owns its video-element binding, not the camera tracks. */
    fun createPreview(): BrowserCameraPreview

    /** Creates a still-photo service that captures from an active session and its attached preview. */
    fun createPhotoCapture(): BrowserPhotoCapture

    /** Creates a recorder for [session]. The session must be started before recording begins. */
    fun createVideoRecorder(
        session: BrowserCameraSession,
        options: BrowserVideoRecordingOptions = BrowserVideoRecordingOptions(),
    ): BrowserVideoRecorder
}
