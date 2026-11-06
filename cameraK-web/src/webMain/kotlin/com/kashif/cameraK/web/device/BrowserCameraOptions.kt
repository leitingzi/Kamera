package com.kashif.cameraK.web.device

/** Preferences used when acquiring or switching a browser camera. */
data class BrowserCameraOptions(
    /** Exact device requested by the caller, when selected from [com.kashif.cameraK.web.api.BrowserCameraApi.enumerateCameras]. */
    val deviceId: String? = null,
    /** Facing preference used when no exact [deviceId] was specified or available. */
    val facingMode: BrowserCameraFacingMode? = null,
    /** Preferred capture width in pixels. */
    val width: Int? = null,
    /** Preferred capture height in pixels. */
    val height: Int? = null,
    /** Preferred capture frame rate. */
    val frameRate: Double? = null,
)
