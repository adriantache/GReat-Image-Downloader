package com.adriantache.greatimagedownloader.data.model

import android.net.Uri
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps

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
