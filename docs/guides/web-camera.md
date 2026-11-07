# Web Camera

Kamera currently provides a standalone browser camera demo in `cameraK-web`. It uses the browser Media Capture APIs and Kotlin/JS or Kotlin/WasmJS; it is not the Android/iOS/Desktop `cameraK` Compose API running in a browser.

## Browser requirements

- Serve the page in a **secure context**: HTTPS in production or `localhost` during development. Browsers generally do not expose `navigator.mediaDevices.getUserMedia()` on insecure origins.
- Camera access must begin in response to a user action, such as a button click. Let the user know why permission is needed and report permission errors instead of retrying silently.
- A top-level page can request camera permission. If the demo is embedded in an iframe, the parent page and server Permissions Policy must allow camera access (for example, `allow="camera"` on the iframe and an appropriate `Permissions-Policy` response header).
- Browser permissions, OS privacy settings, device availability, and browser support all affect the result. A successful device enumeration does not guarantee that a camera can be opened.

## Devices and preview

The demo enumerates `videoinput` devices. Until the user grants access, a browser may hide labels, IDs, or non-default devices. Refresh the list after permission is granted. Do not treat a device ID as a durable identifier across browser sessions.

The preview API accepts an active `BrowserCameraSession` and an `HTMLVideoElement`. It assigns the session stream to `srcObject`, mutes the element, enables inline playback, and waits for `play()`. Playback can still be rejected by browser policy; surface that error to the user.

The demo follows this lifecycle:

| Action | Result |
| --- | --- |
| Open camera | Requests permission and starts the selected input. |
| Choose another input | Close the current session first; the sample disables the selector while the session is open. |
| Stop | Releases active camera tracks; the session remains reusable. |
| Close camera | Stops the session permanently, detaches resources, and enables selection of another input. |
| Page teardown | Detaches the preview and closes the camera session. |

The sample UI intentionally disables the device selector while a session exists. Stop is not the same as close: close the session before selecting another camera.

The underlying `BrowserCameraSession.switchCamera()` API can request a new device without rebuilding the session. If the browser reports that the device is busy, it may release the old stream before retrying and attempts to restore it if switching fails. The sample UI uses the simpler close-then-select flow.

## Current web support boundary

The `cameraK-web` Gradle module currently configures **JS and WasmJS browser executables** for its demo. It is not published as a reusable library artifact. The core `cameraK` module does not currently expose browser targets, and its Compose camera preview and plugins are not connected to this module.

The working browser demo covers device discovery, camera permission, live preview, close-before-select camera changes, and track cleanup. Although some source files define photo and recording API shapes, a complete photo capture or video recording workflow is not currently implemented in the demo. Do not rely on those declarations as supported features.

Browser APIs and codecs differ by browser and device. Avoid assuming that a browser provides a physical front/back camera classification, torch, hardware flash, or a particular recording MIME type. Expose only capabilities the active browser track reports.

## Build and manual testing

Compile both browser targets from the repository root:

```shell
./gradlew :cameraK-web:compileKotlinJs :cameraK-web:compileKotlinWasmJs
```

Run either development server:

```shell
./gradlew :cameraK-web:jsBrowserDevelopmentRun
```

```shell
./gradlew :cameraK-web:wasmJsBrowserDevelopmentRun
```

Test on HTTPS or `localhost` in the browsers you intend to support. Verify permission grant and denial, no-camera and busy-device behavior, selection changes, stream switching, stop/close track release, page teardown, and playback failures. Record the browser and version with manual test results; support should not be inferred from a successful Kotlin compilation alone.
