package com.kashif.cameraK.web.device

/** Availability of the browser prerequisites for camera capture. */
data class BrowserCameraSupport(
    /** Whether the page is running in a secure context such as HTTPS or localhost. */
    val isSecureContext: Boolean,
    /** Whether `navigator.mediaDevices` exists. */
    val hasMediaDevices: Boolean,
    /** Whether `MediaDevices.getUserMedia()` exists. */
    val hasGetUserMedia: Boolean,
    /** Whether `MediaDevices.enumerateDevices()` exists. */
    val hasEnumerateDevices: Boolean,
) {
    /** Whether all prerequisites required by the camera facade are available. */
    val isSupported: Boolean
        get() = isSecureContext && hasMediaDevices && hasGetUserMedia && hasEnumerateDevices

    /** Whether the browser can safely enumerate the available video input devices. */
    val canEnumerateDevices: Boolean
        get() = isSecureContext && hasMediaDevices && hasEnumerateDevices

    /** Explains why device enumeration is unavailable, or null when it can be used. */
    val enumerationUnsupportedReason: String?
        get() = when {
            !isSecureContext -> "Camera access requires a secure context (HTTPS or localhost)."
            !hasMediaDevices -> "navigator.mediaDevices is not available in this browser context."
            !hasEnumerateDevices -> "MediaDevices.enumerateDevices() is not available."
            else -> null
        }

    /** Explains the first missing prerequisite, or null when the browser is ready. */
    val unsupportedReason: String?
        get() = when {
            !isSecureContext -> "Camera access requires a secure context (HTTPS or localhost)."
            !hasMediaDevices -> "navigator.mediaDevices is not available in this browser context."
            !hasGetUserMedia -> "MediaDevices.getUserMedia() is not available."
            !hasEnumerateDevices -> "MediaDevices.enumerateDevices() is not available."
            else -> null
        }
}
