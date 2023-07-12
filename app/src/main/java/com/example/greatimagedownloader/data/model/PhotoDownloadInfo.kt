package com.example.greatimagedownloader.data.model

import android.net.Uri
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.utils.model.Kbps

data class PhotoDownloadInfo(
    val name: String,
    val uri: Uri,
    val downloadProgress: Int,
    val downloadSpeed: Kbps,
) {
    fun toDomain(): PhotoDownloadInfo {
        return PhotoDownloadInfo(
            name = name,
            uri = uri.toString(),
            downloadProgress = downloadProgress,
            downloadSpeed = downloadSpeed,
        )
    }
}
