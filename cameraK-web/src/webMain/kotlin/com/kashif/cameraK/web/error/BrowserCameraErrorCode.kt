package com.kashif.cameraK.web.error

/** Stable error categories exposed by the browser camera facade. */
enum class BrowserCameraErrorCode {
    UNSUPPORTED,
    PERMISSION_DENIED,
    DEVICE_NOT_FOUND,
    DEVICE_UNAVAILABLE,
    CONSTRAINT_NOT_SATISFIED,
    INVALID_ARGUMENT,
    INVALID_STATE,
    PLAYBACK_FAILED,
    CAPTURE_FAILED,
    RECORDING_FAILED,
    UNKNOWN,
}
