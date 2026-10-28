package com.adriantache.greatimagedownloader.domain.model

data class CameraStatus(
    val model: String? = null,
    val firmware: String? = null,
    val batteryLevel: Int? = null,
    val isHot: Boolean = false,
    val macAddress: String? = null,
    val activeStorage: String? = null,
)
