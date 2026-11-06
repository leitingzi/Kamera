package com.kashif.cameraK.web.device

import org.w3c.dom.mediacapture.MediaDeviceInfo

/** A browser media input device. Labels and identifiers may be restricted before permission is granted. */
data class CameraDeviceInfo(val deviceId: String, val groupId: String, val kind: String, val label: String) {
    companion object {
        fun fromDevice(device: MediaDeviceInfo): CameraDeviceInfo = CameraDeviceInfo(
            deviceId = device.deviceId,
            groupId = device.groupId,
            kind = device.kind.toString(),
            label = device.label,
        )
    }
}
