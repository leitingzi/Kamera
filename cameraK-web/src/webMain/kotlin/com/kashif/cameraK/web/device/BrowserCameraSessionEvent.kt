package com.kashif.cameraK.web.device

import com.kashif.cameraK.web.error.BrowserCameraException

/** Events emitted for camera track lifecycle and changes to browser-visible video devices. */
sealed interface BrowserCameraSessionEvent {
    data class TrackEnded(val trackId: String) : BrowserCameraSessionEvent

    data class TrackMuted(val trackId: String) : BrowserCameraSessionEvent

    data class TrackUnmuted(val trackId: String) : BrowserCameraSessionEvent

    data class DevicesChanged(val devices: List<CameraDeviceInfo>) : BrowserCameraSessionEvent

    data class DeviceRefreshFailed(val error: BrowserCameraException) : BrowserCameraSessionEvent

    data class ConstraintsChanged(val settings: BrowserCameraSettings) : BrowserCameraSessionEvent
}
