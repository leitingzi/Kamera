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
private suspend fun Promise<Nothing?>.awaitCameraConstraints(): Unit = suspendCoroutine { continuation ->
    then {
        continuation.resume(Unit)
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
            val acquired = try {
                acquire(options)
            } catch (error: Throwable) {
                val cameraError = asCameraException(error)
                if (!cameraError.requiresCameraReleaseForRetry()) throw cameraError

                releaseStream()
                try {
                    acquire(options)
                } catch (switchError: Throwable) {
                    val switchException = asCameraException(switchError)
                    if (closed || activeOperationId != requestId) throw switchException

                    try {
                        val restoredStream = acquire(currentOptions)
                        ensureOperationCurrent(requestId, restoredStream)
                        installStream(restoredStream, currentOptions)
                        setState(BrowserCameraSessionState.Running(currentDevice))
                        refreshDevices()
                    } catch (restoreError: Throwable) {
                        val restoreException = asCameraException(restoreError)
                        if (!closed && activeOperationId == requestId) {
                            setState(BrowserCameraSessionState.Failed(restoreException))
                        }
                        throw BrowserCameraException(
                            code = BrowserCameraErrorCode.DEVICE_UNAVAILABLE,
                            message = "Could not switch cameras, and the previous camera could not be restored: " +
                                restoreException.message,
                            browserErrorName = restoreException.browserErrorName,
                            constraint = restoreException.constraint,
                            cause = switchException,
                        )
                    }
                    throw switchException
                }
            }
            ensureOperationCurrent(requestId, acquired)
            val previousTrack = currentTrack
            val previousStream = currentStream
            installStream(acquired, options)
            detachTrackListeners(previousTrack)
            previousStream?.let(::stopTracks)
            setState(BrowserCameraSessionState.Running(currentDevice))
            refreshDevices()
        } finally {
            finishOperation(requestId)
        }
    }

    override val supportedConstraints: BrowserCameraSupportedConstraints
        get() = currentTrack?.let { readTrackCameraCapabilities(it).supportedConstraints }
            ?: readSupportedCameraConstraints()

    override fun readCapabilities(): BrowserCameraCapabilities = readTrackCameraCapabilities(requireActiveTrack())

    override fun readSettings(): BrowserCameraSettings = readTrackCameraSettings(requireActiveTrack())

    override fun readConstraints(): BrowserCameraAppliedConstraints = readTrackCameraConstraints(requireActiveTrack())

    override suspend fun applyConstraints(constraints: BrowserCameraConstraintSet) {
        ensureOpen()
        ensureNoPendingOperation()
        val track = requireActiveTrack()
        validateConstraints(constraints, readTrackCameraCapabilities(track))
        val requestId = beginOperation()
        try {
            applyTrackCameraConstraints(track, constraints.toConstraintsJson()).awaitCameraConstraints()
            if (closed || activeOperationId != requestId || currentTrack !== track) {
                throw BrowserCameraException(
                    code = BrowserCameraErrorCode.INVALID_STATE,
                    message = "The camera track changed before its constraints were applied.",
                )
            }
            emit(BrowserCameraSessionEvent.ConstraintsChanged(readTrackCameraSettings(track)))
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

    private fun requireActiveTrack(): MediaStreamTrack = currentTrack ?: throw BrowserCameraException(
        code = BrowserCameraErrorCode.INVALID_STATE,
        message = "Camera capabilities and settings are available only while a camera is active.",
    )

    private fun validateConstraints(
        constraints: BrowserCameraConstraintSet,
        capabilities: BrowserCameraCapabilities,
    ) {
        val supported = capabilities.supportedConstraints
        fun requireSupported(requested: Boolean, available: Boolean, name: String) {
            if (requested && !available) {
                throw BrowserCameraException(
                    code = BrowserCameraErrorCode.UNSUPPORTED,
                    message = "The active camera does not support the '$name' constraint.",
                    constraint = name,
                )
            }
        }

        requireSupported(constraints.width != null, supported.width, "width")
        requireSupported(constraints.height != null, supported.height, "height")
        requireSupported(constraints.aspectRatio != null, supported.aspectRatio, "aspectRatio")
        requireSupported(constraints.frameRate != null, supported.frameRate, "frameRate")
        requireSupported(constraints.deviceId != null, supported.deviceId, "deviceId")
        requireSupported(constraints.facingMode != null, supported.facingMode, "facingMode")
        requireSupported(constraints.zoom != null, capabilities.zoom != null, "zoom")
        requireSupported(constraints.torch != null, capabilities.torch == true, "torch")

        val exactZoom = constraints.zoom?.exact
        val range = capabilities.zoom
        if (exactZoom != null && range != null &&
            ((range.min != null && exactZoom < range.min) || (range.max != null && exactZoom > range.max))
        ) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.CONSTRAINT_NOT_SATISFIED,
                message = "Required zoom $exactZoom is outside the camera's supported range.",
                constraint = "zoom",
            )
        }

        if (constraints == BrowserCameraConstraintSet()) {
            throw BrowserCameraException(
                code = BrowserCameraErrorCode.INVALID_STATE,
                message = "At least one camera constraint must be specified.",
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

    private fun BrowserCameraException.requiresCameraReleaseForRetry(): Boolean =
        code == BrowserCameraErrorCode.DEVICE_UNAVAILABLE ||
            browserErrorName == "NotReadableError" ||
            browserErrorName == "AbortError"

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
