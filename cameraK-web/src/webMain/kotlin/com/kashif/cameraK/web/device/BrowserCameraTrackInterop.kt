package com.kashif.cameraK.web.device

import org.w3c.dom.mediacapture.MediaStreamTrack
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

internal expect fun readSupportedCameraConstraints(): BrowserCameraSupportedConstraints

internal expect fun readTrackCameraCapabilities(track: MediaStreamTrack): BrowserCameraCapabilities

internal expect fun readTrackCameraSettings(track: MediaStreamTrack): BrowserCameraSettings

internal expect fun readTrackCameraConstraints(track: MediaStreamTrack): BrowserCameraAppliedConstraints

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun applyTrackCameraConstraints(track: MediaStreamTrack, constraintsJson: String): Promise<Nothing?>

internal fun BrowserCameraConstraintSet.toConstraintsJson(): String = buildString {
    append('{')
    var hasEntry = false
    fun separator() {
        if (hasEntry) append(',') else hasEntry = true
    }

    fun numeric(name: String, constraint: BrowserNumericConstraint?) {
        constraint ?: return
        val value = constraint.exact ?: constraint.ideal ?: return
        separator()
        append('"').append(name).append("\":{")
        append(if (constraint.exact != null) "\"exact\":" else "\"ideal\":")
        append(value).append("}")
    }

    fun text(name: String, constraint: BrowserStringConstraint?) {
        constraint ?: return
        val value = constraint.exact ?: constraint.ideal ?: return
        separator()
        append('"').append(name).append("\":{")
        append(if (constraint.exact != null) "\"exact\":" else "\"ideal\":")
        appendJsonString(value)
        append('}')
    }

    numeric("width", width)
    numeric("height", height)
    numeric("aspectRatio", aspectRatio)
    numeric("frameRate", frameRate)
    text("deviceId", deviceId)
    text("facingMode", facingMode)
    numeric("zoom", zoom)
    torch?.let {
        separator()
        append("\"torch\":{\"exact\":").append(it).append('}')
    }
    append('}')
}

private fun StringBuilder.appendJsonString(value: String) {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character < ' ') {
                append("\\u").append(character.code.toString(16).padStart(4, '0'))
            } else {
                append(character)
            }
        }
    }
    append('"')
}
