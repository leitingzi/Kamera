package com.kashif.cameraK.web.device

import com.kashif.cameraK.web.error.BrowserCameraErrorCode
import com.kashif.cameraK.web.error.BrowserCameraException
import com.kashif.cameraK.web.error.toBrowserCameraException
import org.w3c.dom.mediacapture.MediaStream
import org.w3c.dom.mediacapture.MediaStreamTrack
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal expect fun requestCameraStream(
    deviceId: String?,
    facingMode: String?,
    width: Int?,
    height: Int?,
    frameRate: Double?,
): Promise<MediaStream>

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun Promise<MediaStream>.awaitCameraStream(): MediaStream = suspendCoroutine { continuation ->
    then { stream ->
        continuation.resume(stream)
        null
    }.catch { error ->
        continuation.resumeWithException(error.toBrowserCameraException())
        null
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserCameraSessionImpl(initialOptions: BrowserCameraOptions) : BrowserCameraSession {
    private val devices = BrowserMediaDevices()
    private val support = devices.support
    private val stateListeners = mutableSetOf<(BrowserCameraSessionState) -> Unit>()
    private val eventListeners = mutableSetOf<(BrowserCameraSessionEvent) -> Unit>()
    private var currentOptions = initialOptions
    private var currentStream: MediaStream? = null
    private var currentTrack: MediaStreamTrack? = null
    private var currentDevice: CameraDeviceInfo? = null
    private var deviceChangeSubscription: BrowserCameraSubscription? = null
    private var operationId = 0
    private var activeOperationId: Int? = null
    private var closed = false

    override var state: BrowserCameraSessionState = BrowserCameraSessionState.Idle
        private set

    override val mediaStream: MediaStream?
        get() = currentStream

    override val videoTrack: MediaStreamTrack?
        get() = currentTrack

    override fun observeState(listener: (BrowserCameraSessionState) -> Unit): BrowserCameraSubscription =
        subscribe(stateListeners, listener)

    override fun observeEvents(listener: (BrowserCameraSessionEvent) -> Unit): BrowserCameraSubscription =
        subscribe(eventListeners, listener)

    override suspend fun start() {
        ensureOpen()
        if (state is BrowserCameraSessionState.Running && currentTrack != null) return
        ensureNoPendingOperation()

        val requestId = beginOperation()
        setState(BrowserCameraSessionState.Starting)
        try {
            val acquired = acquire(currentOptions)
            ensureOperationCurrent(requestId, acquired)
            installStream(acquired, currentOptions)
            setState(BrowserCameraSessionState.Running(currentDevice))
            refreshDevices()
        } catch (error: Throwable) {
            if (activeOperationId == requestId) {
                setState(BrowserCameraSessionState.Failed(asCameraException(error)))
            }
            throw error
        } finally {
            finishOperation(requestId)
        }
    }

    override suspend fun switchCamera(options: BrowserCameraOptions) {
        ensureOpen()
        ensureNoPendingOperation()

        val previousStream = currentStream
        if (previousStream == null) {
            currentOptions = options
            start()
            return
        }

        val requestId = beginOperation()
        try {
            val acquired = acquire(options)
            ensureOperationCurrent(requestId, acquired)
            val previousTrack = currentTrack
            installStream(acquired, options)
            detachTrackListeners(previousTrack)
            stopTracks(previousStream)
            setState(BrowserCameraSessionState.Running(currentDevice))
            refreshDevices()
        } finally {
            finishOperation(requestId)
        }
    }

    override suspend fun stop() {
        if (closed) return
        if (state is BrowserCameraSessionState.Stopped && currentStream == null && activeOperationId == null) return
        invalidatePendingOperation()
        releaseStream()
        setState(BrowserCameraSessionState.Stopped)
    }

    override suspend fun close() {
        if (closed) return
        closed = true
        invalidatePendingOperation()
        releaseStream()
        deviceChangeSubscription?.cancel()
        deviceChangeSubscription = null
        setState(BrowserCameraSessionState.Closed)
        stateListeners.clear()
        eventListeners.clear()
    }

    private suspend fun acquire(options: BrowserCameraOptions): MediaStream {
        val accessUnsupportedReason = when {
            !support.isSecureContext -> "Camera access requires a secure context (HTTPS or localhost)."
            !support.hasMediaDevices -> "navigator.mediaDevices is not available in this browser context."
            !support.hasGetUserMedia -> "MediaDevices.getUserMedia() is not available."
            else -> null
        }
        if (accessUnsupportedReason != null) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.UNSUPPORTED,
                message = accessUnsupportedReason,
            )
        }

        val facing = options.facingMode?.toConstraintValue()
        val requestedDeviceId = options.deviceId?.takeIf(String::isNotBlank)
        try {
            return requestCameraStream(
                deviceId = requestedDeviceId,
                facingMode = facing,
                width = options.width,
                height = options.height,
                frameRate = options.frameRate,
            ).awaitCameraStream()
        } catch (error: Throwable) {
            val cameraError = asCameraException(error)
            if (requestedDeviceId == null || facing == null ||
                cameraError.code !in setOf(
                    BrowserCameraErrorCode.DEVICE_NOT_FOUND,
                    BrowserCameraErrorCode.CONSTRAINT_NOT_SATISFIED,
                )
            ) {
                throw cameraError
            }
            return requestCameraStream(
                deviceId = null,
                facingMode = facing,
                width = options.width,
                height = options.height,
                frameRate = options.frameRate,
            ).awaitCameraStream()
        }
    }

    private fun installStream(stream: MediaStream, options: BrowserCameraOptions) {
        val videoTrack = stream.getVideoTracks().toList().firstOrNull()
        if (videoTrack == null) {
            stopTracks(stream)
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.DEVICE_NOT_FOUND,
                message = "The browser returned a camera stream without a video track.",
            )
        }

        currentStream = stream
        currentTrack = videoTrack
        currentOptions = options
        currentDevice = null
        attachTrackListeners(videoTrack)
        ensureDeviceChangeListener()
    }

    private fun attachTrackListeners(track: MediaStreamTrack) {
        track.onended = {
            if (currentTrack === track) {
                emit(BrowserCameraSessionEvent.TrackEnded(track.id))
                releaseStream()
                setState(
                    BrowserCameraSessionState.Failed(
                        BrowserCameraException(
                            code = BrowserCameraErrorCode.DEVICE_UNAVAILABLE,
                            message = "The active camera track ended unexpectedly.",
                        ),
                    ),
                )
            }
        }
        track.onmute = { emit(BrowserCameraSessionEvent.TrackMuted(track.id)) }
        track.onunmute = { emit(BrowserCameraSessionEvent.TrackUnmuted(track.id)) }
    }

    private fun detachTrackListeners(track: MediaStreamTrack?) {
        track ?: return
        track.onended = null
        track.onmute = null
        track.onunmute = null
    }

    private fun ensureDeviceChangeListener() {
        if (deviceChangeSubscription != null || !support.canEnumerateDevices) return
        deviceChangeSubscription = devices.observeDeviceChanges {
            devices.getCameraDevices(
                onSuccess = { list ->
                    if (closed || currentTrack == null) return@getCameraDevices
                    val activeId = currentTrack?.getSettings()?.deviceId
                    currentDevice = list.firstOrNull { it.deviceId == activeId }
                    setState(BrowserCameraSessionState.Running(currentDevice))
                    emit(BrowserCameraSessionEvent.DevicesChanged(list))
                },
                onFailure = { error -> emit(BrowserCameraSessionEvent.DeviceRefreshFailed(error)) },
            )
        }
    }

    private fun refreshDevices() {
        devices.getCameraDevices(
            onSuccess = { list ->
                if (closed || currentTrack == null) return@getCameraDevices
                val activeId = currentTrack?.getSettings()?.deviceId
                currentDevice = list.firstOrNull { it.deviceId == activeId }
                if (state is BrowserCameraSessionState.Running) {
                    setState(BrowserCameraSessionState.Running(currentDevice))
                }
                emit(BrowserCameraSessionEvent.DevicesChanged(list))
            },
            onFailure = { error -> emit(BrowserCameraSessionEvent.DeviceRefreshFailed(error)) },
        )
    }

    private fun releaseStream() {
        val stream = currentStream
        detachTrackListeners(currentTrack)
        currentStream = null
        currentTrack = null
        currentDevice = null
        deviceChangeSubscription?.cancel()
        deviceChangeSubscription = null
        if (stream != null) stopTracks(stream)
    }

    private fun stopTracks(stream: MediaStream) {
        stream.getTracks().toList().forEach { track ->
            detachTrackListeners(track)
            track.stop()
        }
    }

    private fun setState(value: BrowserCameraSessionState) {
        state = value
        stateListeners.toList().forEach { it(value) }
    }

    private fun emit(event: BrowserCameraSessionEvent) {
        eventListeners.toList().forEach { it(event) }
    }

    private fun beginOperation(): Int {
        val id = ++operationId
        activeOperationId = id
        return id
    }

    private fun ensureOperationCurrent(requestId: Int, acquired: MediaStream) {
        if (closed || activeOperationId != requestId) {
            stopTracks(acquired)
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_STATE,
                message = "The camera request was superseded by stop or close.",
            )
        }
    }

    private fun finishOperation(requestId: Int) {
        if (activeOperationId == requestId) activeOperationId = null
    }

    private fun invalidatePendingOperation() {
        operationId += 1
        activeOperationId = null
    }

    private fun ensureOpen() {
        if (closed) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_STATE,
                message = "The camera session has been closed.",
            )
        }
    }

    private fun ensureNoPendingOperation() {
        if (activeOperationId != null) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_STATE,
                message = "A camera start or switch request is already in progress.",
            )
        }
    }

    private fun asCameraException(error: Throwable): BrowserCameraException =
        error as? BrowserCameraException ?: BrowserCameraException(
            code = BrowserCameraErrorCode.UNKNOWN,
            message = error.message?.takeIf(String::isNotBlank) ?: "An unexpected camera error occurred.",
            cause = error,
        )

    private fun BrowserCameraFacingMode.toConstraintValue(): String = when (this) {
        BrowserCameraFacingMode.USER -> "user"
        BrowserCameraFacingMode.ENVIRONMENT -> "environment"
        BrowserCameraFacingMode.LEFT -> "left"
        BrowserCameraFacingMode.RIGHT -> "right"
    }

    private fun <T> subscribe(
        listeners: MutableSet<(T) -> Unit>,
        listener: (T) -> Unit,
    ): BrowserCameraSubscription {
        if (closed) return BrowserCameraSubscription {}
        listeners += listener
        var cancelled = false
        return BrowserCameraSubscription {
            if (!cancelled) {
                listeners -= listener
                cancelled = true
            }
        }
    }
}
