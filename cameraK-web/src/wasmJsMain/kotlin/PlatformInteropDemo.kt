import kotlin.JsFun
import kotlin.js.ExperimentalWasmJsInterop

internal actual fun runPlatformInteropDemo(): CameraWebDemoResult = CameraWebDemoResult(
    interopMessage = cameraKotlinGreeting(),
)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "() => Boolean(navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function' && typeof navigator.mediaDevices.enumerateDevices === 'function')",
)
private external fun isCameraMediaDevicesSupportedImpl(): Boolean

internal actual fun isCameraMediaDevicesSupported(): Boolean = isCameraMediaDevicesSupportedImpl()
