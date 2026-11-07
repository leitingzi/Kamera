# Web Camera Example

The `cameraK-web` module is a standalone Kotlin/JS and Kotlin/WasmJS browser demo. It currently demonstrates camera discovery, permission on demand, live `<video>` preview, camera selection, and releasing camera resources. It is separate from the `cameraK` Compose API.

Try the [live CameraK Web demo](https://leitingzi.github.io/Kamera/cameraK-web/). Camera access requires HTTPS or `localhost`.

## Run the example locally

From the repository root, run either browser target:

```shell
./gradlew :cameraK-web:jsBrowserDevelopmentRun
```

```shell
./gradlew :cameraK-web:wasmJsBrowserDevelopmentRun
```

On Windows, use `gradlew.bat` instead of `./gradlew`. Open the local URL printed by Gradle, grant camera permission, then choose a camera and select **Open camera**.

## What the demo does

1. Lists the video input devices visible to the browser. Names or devices may be hidden before permission is granted; the list refreshes after opening the camera and when devices change.
2. Requests camera access only after **Open camera** is clicked.
3. Shows the active stream in a muted, inline-playing HTML video element.
4. Allows the current stream to be stopped or closed. Stop releases its tracks but keeps the session; close ends the session and lets you choose a different camera.
5. On page teardown, detaches the preview and closes the session.

## Using the browser session and preview

The browser-specific API uses its own types and does not require `com.kashif.cameraK` core types. A typical preview lifecycle looks like this:

```kotlin
import com.kashif.cameraK.web.device.BrowserCameraOptions
import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.preview.BrowserCameraPreview
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement

val session = BrowserCameraSession.create(
    BrowserCameraOptions(deviceId = selectedDeviceId),
)
val preview = BrowserCameraPreview.create()
val video = document.getElementById("camera-preview") as HTMLVideoElement

// Call from a user click handler. The browser may reject access or playback.
suspend fun openCamera() {
    session.start()
    preview.attach(session, video)
}

suspend fun closeCamera() {
    preview.detach()
    session.close()
}
```

`attach` binds the active `MediaStream` to the video element, sets `muted` and `playsInline`, and waits for `play()` to finish. A playback rejection is reported as a `BrowserCameraException`; later playback errors are available from `playbackError` and `observePlaybackErrors`. The preview follows a stream replacement during a session camera switch and detaches when the session stops, closes, or fails.

`stop()` releases the active camera tracks but leaves the session reusable. `close()` is permanent and idempotent. The preview owns only the element binding; close the session as well when the camera is no longer needed.

## Current scope

- **Available in the demo:** video device discovery, camera start/stop/close, device selection (close the active session before selecting another camera), video preview, and camera/session error reporting.
- **Not yet demonstrated or provided as a complete browser feature:** photo capture and video recording. The source contains result and service API declarations, but they are not currently wired into the demo as working capture/recording implementations.
- **Not a `cameraK` core web target:** the current module builds browser executables. It is not a published library dependency for Compose apps, and `rememberCameraKState` / `CameraPreviewView` are not backed by this browser module.

See the [Web camera guide](../guides/web-camera.md) for browser requirements, lifecycle notes, and platform boundaries.
