package com.kashif.cameraK.web.device

import org.w3c.dom.mediacapture.MediaStreamTrack
import org.w3c.dom.mediacapture.MediaTrackConstraints
import kotlin.js.Promise

internal actual fun readSupportedCameraConstraints(): BrowserCameraSupportedConstraints {
    val mediaDevices = kotlinx.browser.window.navigator.asDynamic().mediaDevices
    if (mediaDevices?.getSupportedConstraints == null) {
        return BrowserCameraSupportedConstraints(
            width = false,
            height = false,
            aspectRatio = false,
            frameRate = false,
            facingMode = false,
            deviceId = false,
            zoom = false,
            torch = false,
        )
    }
    val supported = mediaDevices.getSupportedConstraints()
    return BrowserCameraSupportedConstraints(
        width = supported.width == true,
        height = supported.height == true,
        aspectRatio = supported.aspectRatio == true,
        frameRate = supported.frameRate == true,
        facingMode = supported.facingMode == true,
        deviceId = supported.deviceId == true,
        zoom = supported.zoom == true,
        torch = supported.torch == true,
    )
}

internal actual fun readTrackCameraCapabilities(track: MediaStreamTrack): BrowserCameraCapabilities {
    val capabilities = track.getCapabilities().asDynamic()
    val zoom = capabilities.zoom
    val torch = capabilities.torch as? Boolean
    val supported = readSupportedCameraConstraints()
    return BrowserCameraCapabilities(
        supportedConstraints = supported.copy(
            zoom = supported.zoom || zoom != null,
            torch = supported.torch || torch != null,
        ),
        width = numericRange(capabilities.width),
        height = numericRange(capabilities.height),
        aspectRatio = numericRange(capabilities.aspectRatio),
        frameRate = numericRange(capabilities.frameRate),
        zoom = numericRange(zoom),
        facingModes = (capabilities.facingMode as? Array<String>)?.toList().orEmpty(),
        torch = torch,
    )
}

internal actual fun readTrackCameraSettings(track: MediaStreamTrack): BrowserCameraSettings {
    val settings = track.getSettings().asDynamic()
    return BrowserCameraSettings(
        width = (settings.width as? Number)?.toInt(),
        height = (settings.height as? Number)?.toInt(),
        aspectRatio = (settings.aspectRatio as? Number)?.toDouble(),
        frameRate = (settings.frameRate as? Number)?.toDouble(),
        facingMode = settings.facingMode as? String,
        deviceId = settings.deviceId as? String,
        zoom = (settings.zoom as? Number)?.toDouble(),
        torch = settings.torch as? Boolean,
    )
}

internal actual fun readTrackCameraConstraints(track: MediaStreamTrack): BrowserCameraAppliedConstraints {
    val constraints = track.getConstraints().asDynamic()
    return BrowserCameraAppliedConstraints(
        width = numericConstraint(constraints.width),
        height = numericConstraint(constraints.height),
        aspectRatio = numericConstraint(constraints.aspectRatio),
        frameRate = numericConstraint(constraints.frameRate),
        deviceId = stringConstraint(constraints.deviceId),
        facingMode = stringConstraint(constraints.facingMode),
        zoom = numericConstraint(constraints.zoom),
        torch = booleanConstraint(constraints.torch),
    )
}

internal actual fun applyTrackCameraConstraints(
    track: MediaStreamTrack,
    constraintsJson: String,
): Promise<Nothing?> = track.applyConstraints(
    js("JSON.parse(constraintsJson)").unsafeCast<MediaTrackConstraints>(),
).unsafeCast<Promise<Nothing?>>()

private fun numericRange(value: dynamic): BrowserNumericRange? {
    if (value == null) return null
    return BrowserNumericRange(
        min = (value.min as? Number)?.toDouble(),
        max = (value.max as? Number)?.toDouble(),
        step = (value.step as? Number)?.toDouble(),
    )
}

private fun numericConstraint(value: dynamic): BrowserCameraConstraintValue<Double>? {
    if (value == null) return null
    if (value is Number) return BrowserCameraConstraintValue(ideal = value.toDouble(), exact = null)
    return BrowserCameraConstraintValue(
        ideal = (value.ideal as? Number)?.toDouble(),
        exact = (value.exact as? Number)?.toDouble(),
    )
}

private fun stringConstraint(value: dynamic): BrowserCameraConstraintValue<String>? {
    if (value == null) return null
    if (value is String) return BrowserCameraConstraintValue(ideal = value, exact = null)
    return BrowserCameraConstraintValue(
        ideal = value.ideal as? String,
        exact = value.exact as? String,
    )
}

private fun booleanConstraint(value: dynamic): Boolean? {
    if (value is Boolean) return value
    return value?.exact as? Boolean
}
