internal actual fun runPlatformInteropDemo() {
    val browser = js("globalThis")
    browser.cameraKotlinGreeting = { cameraKotlinGreeting() }

    val result = browser.cameraKWebExample()
    browser.document.getElementById("output").textContent = result
    browser.console.log(result)
}
