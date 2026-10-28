package com.adriantache.greatimagedownloader.domain.data.model

import com.adriantache.greatimagedownloader.domain.utils.model.Kbps

data class PhotoDownloadInfo(
    val name: String,
    val uri: String,
    val downloadProgress: Int,
    val downloadSpeed: Kbps,
    val isLandscape: Boolean = false,
) {
    val isImage: Boolean = name.endsWith(".jpg", ignoreCase = true) ||
        name.endsWith(".jpeg", ignoreCase = true)

    val isFinished: Boolean = downloadProgress == 100
}
