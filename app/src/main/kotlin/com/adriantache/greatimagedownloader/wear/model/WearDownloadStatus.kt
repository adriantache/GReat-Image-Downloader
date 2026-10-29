package com.adriantache.greatimagedownloader.wear.model

import kotlinx.serialization.Serializable

@Serializable
data class WearDownloadStatus(
    val state: String = "IDLE", // IDLE, CONNECTING, CHECKING, DOWNLOADING, STOPPING, DONE, ERROR
    val stageText: String = "",
    val currentPhoto: Int = 0,
    val totalPhotos: Int = 0,
    val photoProgressPercent: Int = 0,
    val downloadCount: Int = 0,
    val errorMessage: String? = null,
)
