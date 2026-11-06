internal actual fun runPlatformInteropDemo(): CameraWebDemoResult {
    val browser = js("globalThis")
    browser.cameraKotlinGreeting = { cameraKotlinGreeting() }

    return CameraWebDemoResult(
        interopMessage = browser.cameraKWebExample() as String,
        isCameraSupported = browser.cameraKWebIsCameraSupported() as Boolean,
    )
}
