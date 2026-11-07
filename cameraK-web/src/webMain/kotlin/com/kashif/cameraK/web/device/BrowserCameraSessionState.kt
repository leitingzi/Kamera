package com.kashif.cameraK.web.device

import com.kashif.cameraK.web.error.BrowserCameraException

/** Current state of a browser camera session. */
sealed interface BrowserCameraSessionState {
    data object Idle : BrowserCameraSessionState

    data object Starting : BrowserCameraSessionState

    data class Running(val device: CameraDeviceInfo?) : BrowserCameraSessionState

    data object Stopped : BrowserCameraSessionState

    data object Closed : BrowserCameraSessionState

    data class Failed(val error: BrowserCameraException) : BrowserCameraSessionState
}
