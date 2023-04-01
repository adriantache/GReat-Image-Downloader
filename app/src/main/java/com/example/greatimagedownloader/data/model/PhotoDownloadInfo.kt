package com.example.greatimagedownloader.data.model

import android.net.Uri
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo

data class PhotoDownloadInfo(
    val uri: Uri,
    val downloadProgress: Int,
) {
    // TODO: consider if we need this name
    fun toDomain(name: String): PhotoDownloadInfo {
        return PhotoDownloadInfo(
            name = name,
            uri = uri.toString(),
            downloadProgress = downloadProgress,
        )
    }
}
