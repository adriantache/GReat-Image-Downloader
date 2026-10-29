package com.adriantache.greatimagedownloader.domain.model

data class CameraStatus(
    val model: String? = null,
    val firmware: String? = null,
    val batteryLevel: Int? = null,
    val isHot: Boolean = false,
    val macAddress: String? = null,
    val activeStorage: String? = null,
) {
    val formattedBattery: String?
        get() = batteryLevel?.let { level ->
            when {
                level >= 80 -> "Full (3/3 bars)"
                level >= 50 -> "Medium (2/3 bars)"
                level > 20 -> "Low (1/3 bar)"
                else -> "Critical ($level%)"
            }
        }
}
