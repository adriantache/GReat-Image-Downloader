package com.adriantache.greatimagedownloader.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val rememberLastDownloadedPhotos: Boolean? = null,
    val downloadRawFiles: Boolean = false,
    val downloadVideos: Boolean = true,
    val fastPreviewDownload: Boolean = false,
)
