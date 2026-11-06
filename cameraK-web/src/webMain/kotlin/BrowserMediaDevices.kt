import org.w3c.dom.mediacapture.MediaDevices
import org.w3c.dom.mediacapture.MediaTrackSupportedConstraints
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toList

@OptIn(ExperimentalWasmJsInterop::class)
class BrowserMediaDevices(private val mediaDevices: MediaDevices) {
    fun getSupportedConstraints(): List<String> {
        val constraints = mediaDevices.getSupportedConstraints()
        return buildList {
            if (constraints.aspectRatio == true) add("aspectRatio")
            if (constraints.autoGainControl == true) add("autoGainControl")
            if (constraints.channelCount == true) add("channelCount")
            if (constraints.deviceId == true) add("deviceId")
            if (isExtendedMediaConstraintSupported(constraints, "displaySurface")) add("displaySurface")
            if (constraints.echoCancellation == true) add("echoCancellation")
            if (constraints.facingMode == true) add("facingMode")
            if (constraints.frameRate == true) add("frameRate")
            if (constraints.groupId == true) add("groupId")
            if (constraints.height == true) add("height")
            if (constraints.latency == true) add("latency")
            if (constraints.noiseSuppression == true) add("noiseSuppression")
            if (isExtendedMediaConstraintSupported(constraints, "logicalSurface")) add("logicalSurface")
            if (constraints.resizeMode == true) add("resizeMode")
            if (constraints.sampleRate == true) add("sampleRate")
            if (constraints.sampleSize == true) add("sampleSize")
            if (isExtendedMediaConstraintSupported(constraints, "restrictOwnAudio")) add("restrictOwnAudio")
            if (isExtendedMediaConstraintSupported(
                    constraints,
                    "suppressLocalAudioPlayback",
                )
            ) {
                add("suppressLocalAudioPlayback")
            }
            if (constraints.volume == true) add("volume")
            if (constraints.width == true) add("width")
        }
    }

    fun enumerateDevices(onSuccess: (List<String>) -> Unit, onFailure: (String) -> Unit) {
        mediaDevices.enumerateDevices()
            .then { devices ->
                onSuccess(
                    devices.toList().map { device ->
                        val label = device.label.ifBlank { "Label hidden until permission is granted" }
                        "${device.kind}: $label"
                    },
                )
                null
            }
            .catch { error ->
                onFailure(error.toString())
                null
            }
    }
}

internal expect fun isExtendedMediaConstraintSupported(
    constraints: MediaTrackSupportedConstraints,
    name: String,
): Boolean
