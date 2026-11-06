import org.w3c.dom.mediacapture.MediaTrackSupportedConstraints
import kotlin.JsFun
import kotlin.js.ExperimentalWasmJsInterop

internal actual fun runPlatformInteropDemo(): CameraWebDemoResult = CameraWebDemoResult(
    interopMessage = cameraKotlinGreeting(),
    isCameraSupported = supportsMediaDeviceEnumeration(),
)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "() => Boolean(navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function' && typeof navigator.mediaDevices.enumerateDevices === 'function' && typeof navigator.mediaDevices.getSupportedConstraints === 'function')",
)
private external fun supportsMediaDeviceEnumeration(): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(constraints, name) => constraints[name] === true")
private external fun isExtendedMediaConstraintSupportedImpl(
    constraints: MediaTrackSupportedConstraints,
    name: String,
): Boolean

internal actual fun isExtendedMediaConstraintSupported(
    constraints: MediaTrackSupportedConstraints,
    name: String,
): Boolean = isExtendedMediaConstraintSupportedImpl(constraints, name)
