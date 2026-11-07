package com.kashif.cameraK.web.preview

import com.kashif.cameraK.web.device.BrowserCameraSession
import com.kashif.cameraK.web.device.BrowserCameraSessionState
import com.kashif.cameraK.web.device.BrowserCameraSubscription
import com.kashif.cameraK.web.error.BrowserCameraErrorCode
import com.kashif.cameraK.web.error.BrowserCameraException
import com.kashif.cameraK.web.error.toBrowserCameraException
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.mediacapture.MediaStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserCameraPreviewImpl : BrowserCameraPreview {
    private val errorListeners = mutableSetOf<(BrowserCameraException) -> Unit>()
    private var videoElement: HTMLVideoElement? = null
    private var session: BrowserCameraSession? = null
    private var boundStream: MediaStream? = null
    private var sessionSubscription: BrowserCameraSubscription? = null
    private var closed = false

    override var playbackError: BrowserCameraException? = null
        private set

    override val isAttached: Boolean
        get() = videoElement != null && boundStream != null

    override suspend fun attach(session: BrowserCameraSession, videoElement: HTMLVideoElement) {
        ensureOpen()
        val stream = session.mediaStream ?: throw BrowserCameraException(
            code = BrowserCameraErrorCode.INVALID_STATE,
            message = "Start the camera session before attaching a preview.",
        )

        detach()
        this.session = session
        this.videoElement = videoElement
        bindStream(stream)
        sessionSubscription = session.observeState { state -> onSessionStateChanged(state) }

        try {
            play(videoElement)
            if (this.session !== session || this.videoElement !== videoElement ||
                session.mediaStream !== boundStream
            ) {
                throw BrowserCameraException(
                    code = BrowserCameraErrorCode.INVALID_STATE,
                    message = "The camera session changed before preview playback started.",
                )
            }
            playbackError = null
        } catch (error: Throwable) {
            val playbackFailure = error as? BrowserCameraException ?: error.asSynchronousPlaybackError()
            if (this.session === session && this.videoElement === videoElement) {
                playbackError = playbackFailure
                detach()
            }
            throw playbackFailure
        }
    }

    override fun observePlaybackErrors(listener: (BrowserCameraException) -> Unit): BrowserCameraSubscription {
        if (closed) return BrowserCameraSubscription {}
        errorListeners += listener
        var cancelled = false
        return BrowserCameraSubscription {
            if (!cancelled) {
                errorListeners -= listener
                cancelled = true
            }
        }
    }

    override fun detach() {
        sessionSubscription?.cancel()
        sessionSubscription = null
        videoElement?.srcObject = null
        videoElement = null
        session = null
        boundStream = null
    }

    override fun close() {
        if (closed) return
        detach()
        closed = true
        errorListeners.clear()
    }

    private fun onSessionStateChanged(state: BrowserCameraSessionState) {
        when (state) {
            is BrowserCameraSessionState.Running -> {
                val activeSession = session ?: return
                val stream = activeSession.mediaStream ?: return
                if (stream !== boundStream) {
                    bindStream(stream)
                    videoElement?.let(::playAndReportFailure)
                }
            }

            BrowserCameraSessionState.Stopped,
            BrowserCameraSessionState.Closed,
            is BrowserCameraSessionState.Failed,
                -> detach()

            BrowserCameraSessionState.Idle,
            BrowserCameraSessionState.Starting,
                -> Unit
        }
    }

    private fun bindStream(stream: MediaStream) {
        val video = videoElement ?: return
        boundStream = stream
        playbackError = null
        video.muted = true
        video.playsInline = true
        video.srcObject = stream
    }

    private fun playAndReportFailure(video: HTMLVideoElement) {
        val stream = boundStream ?: return
        try {
            video.play().then {
                if (videoElement === video && boundStream === stream) playbackError = null
                null
            }.catch { error ->
                if (videoElement === video && boundStream === stream) {
                    reportPlaybackError(error.asRejectedPlaybackError())
                }
                null
            }
        } catch (error: Throwable) {
            if (videoElement === video && boundStream === stream) {
                reportPlaybackError(error.asSynchronousPlaybackError())
            }
        }
    }

    private suspend fun play(video: HTMLVideoElement): Unit = suspendCoroutine { continuation ->
        try {
            video.play().then {
                continuation.resume(Unit)
                null
            }.catch { error ->
                continuation.resumeWithException(error.asRejectedPlaybackError())
                null
            }
        } catch (error: Throwable) {
            continuation.resumeWithException(error.asSynchronousPlaybackError())
        }
    }

    private fun JsPromiseError.asRejectedPlaybackError(): BrowserCameraException =
        toBrowserCameraException().asPlaybackFailure()

    private fun Throwable.asSynchronousPlaybackError(): BrowserCameraException =
        (this as? BrowserCameraException)?.asPlaybackFailure()
            ?: BrowserCameraException(
                code = BrowserCameraErrorCode.PLAYBACK_FAILED,
                message = message?.takeIf(String::isNotBlank) ?: "The browser could not start video playback.",
                cause = this,
            )

    private fun BrowserCameraException.asPlaybackFailure(): BrowserCameraException =
        BrowserCameraException(
            code = BrowserCameraErrorCode.PLAYBACK_FAILED,
            message = message,
            browserErrorName = browserErrorName,
            constraint = constraint,
            cause = this,
        )

    private fun reportPlaybackError(error: BrowserCameraException) {
        playbackError = error
        errorListeners.toList().forEach { it(error) }
    }

    private fun ensureOpen() {
        if (closed) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_STATE,
                message = "The camera preview has been closed.",
            )
        }
    }
}
