package com.adriantache.greatimagedownloader.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoFile(
    val directory: String,
    val name: String,
) {
    val isRawFile: Boolean
        get() = when (name.substringAfterLast('.', "").lowercase()) {
            "dng", "raw" -> true
            else -> false
        }

    val isVideoFile: Boolean
        get() = when (name.substringAfterLast('.', "").lowercase()) {
            "mov", "mp4", "avi" -> true
            else -> false
        }
}
