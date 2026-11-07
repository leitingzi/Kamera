package com.kashif.cameraK.web.device

import org.w3c.dom.mediacapture.MediaStreamTrack
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(name) => !!globalThis.navigator?.mediaDevices?.getSupportedConstraints?.()[name]")
private external fun isSupportedConstraint(name: String): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, key, bound) => { const v = track.getCapabilities()?.[key]?.[bound]; return typeof v === 'number' ? v : null; }")
private external fun getCapabilityRangeValue(track: JsAny, key: String, bound: String): Double?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, key) => { const v = track.getCapabilities()?.[key]; return typeof v === 'boolean' ? v : null; }")
private external fun getCapabilityBoolean(track: JsAny, key: String): Boolean?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track) => track.getCapabilities()?.facingMode?.join(',') ?? ''")
private external fun getCapabilityFacingModes(track: JsAny): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, key) => { const v = track.getSettings()?.[key]; return typeof v === 'number' ? v : null; }")
private external fun getSettingNumber(track: JsAny, key: String): Double?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, key) => { const v = track.getSettings()?.[key]; return typeof v === 'string' ? v : null; }")
private external fun getSettingString(track: JsAny, key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, key) => { const v = track.getSettings()?.[key]; return typeof v === 'boolean' ? v : null; }")
private external fun getSettingBoolean(track: JsAny, key: String): Boolean?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, key, kind) => { const v = track.getConstraints()?.[key]; const x = (v && typeof v === 'object') ? v[kind] : (kind === 'ideal' ? v : null); return typeof x === 'number' ? x : null; }")
private external fun getConstraintNumber(track: JsAny, key: String, kind: String): Double?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, key, kind) => { const v = track.getConstraints()?.[key]; const x = (v && typeof v === 'object') ? v[kind] : (kind === 'ideal' ? v : null); return typeof x === 'string' ? x : null; }")
private external fun getConstraintString(track: JsAny, key: String, kind: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, key) => { const v = track.getConstraints()?.[key]; return typeof v === 'boolean' ? v : (typeof v?.exact === 'boolean' ? v.exact : null); }")
private external fun getConstraintBoolean(track: JsAny, key: String): Boolean?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(track, constraints) => track.applyConstraints(JSON.parse(constraints))")
private external fun applyTrackConstraintsImpl(track: JsAny, constraints: String): JsAny

internal actual fun readSupportedCameraConstraints(): BrowserCameraSupportedConstraints =
    BrowserCameraSupportedConstraints(
        width = isSupportedConstraint("width"),
        height = isSupportedConstraint("height"),
        aspectRatio = isSupportedConstraint("aspectRatio"),
        frameRate = isSupportedConstraint("frameRate"),
        facingMode = isSupportedConstraint("facingMode"),
        deviceId = isSupportedConstraint("deviceId"),
        zoom = isSupportedConstraint("zoom"),
        torch = isSupportedConstraint("torch"),
    )

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun readTrackCameraCapabilities(track: MediaStreamTrack): BrowserCameraCapabilities {
    val jsTrack = track.unsafeCast<JsAny>()
    val zoom = readNumericRange(jsTrack, "zoom")
    val torch = getCapabilityBoolean(jsTrack, "torch")
    val supported = readSupportedCameraConstraints()
    return BrowserCameraCapabilities(
        supportedConstraints = supported.copy(
            zoom = supported.zoom || zoom != null,
            torch = supported.torch || torch != null,
        ),
        width = readNumericRange(jsTrack, "width"),
        height = readNumericRange(jsTrack, "height"),
        aspectRatio = readNumericRange(jsTrack, "aspectRatio"),
        frameRate = readNumericRange(jsTrack, "frameRate"),
        zoom = zoom,
        facingModes = getCapabilityFacingModes(jsTrack).split(',').filter(String::isNotBlank),
        torch = torch,
    )
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun readTrackCameraSettings(track: MediaStreamTrack): BrowserCameraSettings {
    val jsTrack = track.unsafeCast<JsAny>()
    return BrowserCameraSettings(
        width = getSettingNumber(jsTrack, "width")?.toInt(),
        height = getSettingNumber(jsTrack, "height")?.toInt(),
        aspectRatio = getSettingNumber(jsTrack, "aspectRatio"),
        frameRate = getSettingNumber(jsTrack, "frameRate"),
        facingMode = getSettingString(jsTrack, "facingMode"),
        deviceId = getSettingString(jsTrack, "deviceId"),
        zoom = getSettingNumber(jsTrack, "zoom"),
        torch = getSettingBoolean(jsTrack, "torch"),
    )
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun readTrackCameraConstraints(track: MediaStreamTrack): BrowserCameraAppliedConstraints {
    val jsTrack = track.unsafeCast<JsAny>()
    return BrowserCameraAppliedConstraints(
        width = readNumericConstraint(jsTrack, "width"),
        height = readNumericConstraint(jsTrack, "height"),
        aspectRatio = readNumericConstraint(jsTrack, "aspectRatio"),
        frameRate = readNumericConstraint(jsTrack, "frameRate"),
        deviceId = readStringConstraint(jsTrack, "deviceId"),
        facingMode = readStringConstraint(jsTrack, "facingMode"),
        zoom = readNumericConstraint(jsTrack, "zoom"),
        torch = getConstraintBoolean(jsTrack, "torch"),
    )
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun applyTrackCameraConstraints(
    track: MediaStreamTrack,
    constraintsJson: String,
): Promise<Nothing?> = applyTrackConstraintsImpl(track.unsafeCast(), constraintsJson).unsafeCast()

@OptIn(ExperimentalWasmJsInterop::class)
private fun readNumericRange(track: JsAny, key: String): BrowserNumericRange? {
    val min = getCapabilityRangeValue(track, key, "min")
    val max = getCapabilityRangeValue(track, key, "max")
    val step = getCapabilityRangeValue(track, key, "step")
    return if (min == null && max == null && step == null) null else BrowserNumericRange(min, max, step)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun readNumericConstraint(track: JsAny, key: String): BrowserCameraConstraintValue<Double>? {
    val ideal = getConstraintNumber(track, key, "ideal")
    val exact = getConstraintNumber(track, key, "exact")
    return if (ideal == null && exact == null) null else BrowserCameraConstraintValue(ideal, exact)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun readStringConstraint(track: JsAny, key: String): BrowserCameraConstraintValue<String>? {
    val ideal = getConstraintString(track, key, "ideal")
    val exact = getConstraintString(track, key, "exact")
    return if (ideal == null && exact == null) null else BrowserCameraConstraintValue(ideal, exact)
}
