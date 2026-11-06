package com.kashif.cameraK.web.device

/** A cancellable registration for a browser-device or session-state listener. */
fun interface BrowserCameraSubscription {
    /** Removes the listener. Calling this more than once has no effect. */
    fun cancel()
}
