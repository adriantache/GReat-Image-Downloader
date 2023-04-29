package com.example.greatimagedownloader.domain.data.model

data class PhotoDownloadInfo(
    val name: String,
    val uri: String,
    val downloadProgress: Int,
) {
    val isImage: Boolean = name.split(".").last().lowercase() == "jpg"
}
