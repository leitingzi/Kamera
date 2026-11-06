globalThis.cameraKWebExample = function () {
    const greeting = globalThis.cameraKotlinGreeting();
    return "JavaScript called Kotlin: " + greeting;
};

globalThis.cameraKWebIsCameraSupported = function () {
    return Boolean(
        navigator.mediaDevices &&
        typeof navigator.mediaDevices.getUserMedia === "function"
    );
};
