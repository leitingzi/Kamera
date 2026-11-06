import org.w3c.dom.mediacapture.MediaTrackSupportedConstraints

internal actual fun isExtendedMediaConstraintSupported(
    constraints: MediaTrackSupportedConstraints,
    name: String,
): Boolean = js("constraints[name] === true")
