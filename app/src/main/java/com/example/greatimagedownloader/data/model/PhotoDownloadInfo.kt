package com.example.greatimagedownloader.data.model

import android.net.Uri
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo

data class PhotoDownloadInfo(
    val uri: Uri?,
    val downloadProgress: Int,
) {
    fun toDomain(): PhotoDownloadInfo {
        return PhotoDownloadInfo(
            uri = uri.toString(),
            downloadProgress = downloadProgress,
        )
    }
}
