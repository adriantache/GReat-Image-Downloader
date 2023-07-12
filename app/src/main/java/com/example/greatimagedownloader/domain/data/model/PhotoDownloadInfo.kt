package com.example.greatimagedownloader.domain.data.model

import com.example.greatimagedownloader.domain.utils.model.Kbps

data class PhotoDownloadInfo(
    val name: String,
    val uri: String,
    val downloadProgress: Int,
    val downloadSpeed: Kbps,
) {
    val isImage: Boolean = name.split(".").last().lowercase() == "jpg"
}
