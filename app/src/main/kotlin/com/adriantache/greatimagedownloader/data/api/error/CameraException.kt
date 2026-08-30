package com.adriantache.greatimagedownloader.data.api.error

sealed class CameraException : Exception() {
    data object CameraDisconnected : CameraException()
    data class NetworkError(override val cause: Throwable) : CameraException()
    data class Unknown(override val message: String?) : CameraException()
}
