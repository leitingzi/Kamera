import org.w3c.dom.mediacapture.MediaDeviceInfo

data class CameraDeviceInfo(
    val deviceId: String,
    val groupId: String,
    val kind: String,
    val label: String,
) {
    companion object {
        fun fromDevice(device: MediaDeviceInfo): CameraDeviceInfo {
            return CameraDeviceInfo(
                deviceId = device.deviceId,
                groupId = device.groupId,
                kind = device.kind.toString(),
                label = device.label,
            )
        }
    }
}
