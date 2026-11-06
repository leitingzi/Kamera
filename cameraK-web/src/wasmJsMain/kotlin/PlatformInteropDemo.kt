import kotlin.JsFun
import kotlin.js.ExperimentalWasmJsInterop

internal actual fun runPlatformInteropDemo(): CameraWebDemoResult = CameraWebDemoResult(
    interopMessage = cameraKotlinGreeting(),
    isCameraSupported = supportsMediaDeviceEnumeration(),
)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Boolean(navigator.mediaDevices && typeof navigator.mediaDevices.enumerateDevices === 'function')")
private external fun supportsMediaDeviceEnumeration(): Boolean
