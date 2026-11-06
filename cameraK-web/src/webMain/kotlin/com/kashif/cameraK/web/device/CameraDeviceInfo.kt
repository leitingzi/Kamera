package com.kashif.cameraK.web.device

import org.w3c.dom.mediacapture.MediaDeviceInfo

/**
 * A browser video input device.
 *
 * Browsers may hide labels, IDs, or devices before permission is granted. Refresh the device list
 * after a successful camera permission request to obtain the information the browser permits.
 */
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
