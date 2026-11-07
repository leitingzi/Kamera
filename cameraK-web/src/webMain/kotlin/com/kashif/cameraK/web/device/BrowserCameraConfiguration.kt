package com.kashif.cameraK.web.device

/** A numeric range reported by the active camera track. */
data class BrowserNumericRange(val min: Double?, val max: Double?, val step: Double? = null)

/** Browser-reported constraints available for the current video track. */
data class BrowserCameraSupportedConstraints(
    val width: Boolean,
    val height: Boolean,
    val aspectRatio: Boolean,
    val frameRate: Boolean,
    val facingMode: Boolean,
    val deviceId: Boolean,
    val zoom: Boolean,
    val torch: Boolean,
)

/** Device capabilities actually reported by the active track. Unsupported capabilities are null. */
data class BrowserCameraCapabilities(
    val supportedConstraints: BrowserCameraSupportedConstraints,
    val width: BrowserNumericRange?,
    val height: BrowserNumericRange?,
    val aspectRatio: BrowserNumericRange?,
    val frameRate: BrowserNumericRange?,
    val zoom: BrowserNumericRange?,
    val facingModes: List<String>,
    val torch: Boolean?,
)

/** Effective settings currently reported by the active video track. */
data class BrowserCameraSettings(
    val width: Int?,
    val height: Int?,
    val aspectRatio: Double?,
    val frameRate: Double?,
    val facingMode: String?,
    val deviceId: String?,
    val zoom: Double?,
    val torch: Boolean?,
)

/** A value requested as a browser preference ([ideal]) or as a strict requirement ([exact]). */
data class BrowserNumericConstraint(val ideal: Double? = null, val exact: Double? = null) {
    init {
        require(ideal != null || exact != null) { "Specify an ideal or exact value." }
        require(ideal == null || exact == null) { "Specify either ideal or exact, not both." }
        require(ideal == null || ideal.isFinite() && ideal > 0) { "Numeric camera constraints must be finite and positive." }
        require(exact == null || exact.isFinite() && exact > 0) { "Numeric camera constraints must be finite and positive." }
    }
}

/** A string value requested as a browser preference ([ideal]) or strict requirement ([exact]). */
data class BrowserStringConstraint(val ideal: String? = null, val exact: String? = null) {
    init {
        require(!ideal.isNullOrBlank() || !exact.isNullOrBlank()) { "Specify a non-blank ideal or exact value." }
        require(ideal == null || exact == null) { "Specify either ideal or exact, not both." }
    }
}

/** Dynamic constraints to apply to an active camera track. Unset fields are left unchanged. */
data class BrowserCameraConstraintSet(
    val width: BrowserNumericConstraint? = null,
    val height: BrowserNumericConstraint? = null,
    val aspectRatio: BrowserNumericConstraint? = null,
    val frameRate: BrowserNumericConstraint? = null,
    val deviceId: BrowserStringConstraint? = null,
    val facingMode: BrowserStringConstraint? = null,
    val zoom: BrowserNumericConstraint? = null,
    val torch: Boolean? = null,
)

/** Constraint values currently reported by `MediaStreamTrack.getConstraints()`. */
data class BrowserCameraAppliedConstraints(
    val width: BrowserCameraConstraintValue<Double>?,
    val height: BrowserCameraConstraintValue<Double>?,
    val aspectRatio: BrowserCameraConstraintValue<Double>?,
    val frameRate: BrowserCameraConstraintValue<Double>?,
    val deviceId: BrowserCameraConstraintValue<String>?,
    val facingMode: BrowserCameraConstraintValue<String>?,
    val zoom: BrowserCameraConstraintValue<Double>?,
    val torch: Boolean?,
)

/** A simple exact or preferred value read from the browser's current track constraints. */
data class BrowserCameraConstraintValue<T>(val ideal: T?, val exact: T?)
