package com.adriantache.greatimagedownloader.domain.data.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps


data class PhotoDownloadInfo(
    val name: String,
    val uri: String,
    val downloadProgress: Int,
    val downloadSpeed: Kbps,
) {
    val isImage: Boolean = name.split(".").last().lowercase() == "jpg"

    val isFinished = downloadProgress == 100

    fun isLandscape(context: Context): Boolean {
        val imageSize = getImageSize(context.contentResolver, Uri.parse(uri)) ?: return false

        return imageSize.width > imageSize.height
    }

    private fun getImageSize(resolver: ContentResolver, uri: Uri): Size? {
        val projection = arrayOf(
            MediaStore.Images.ImageColumns.WIDTH,
            MediaStore.Images.ImageColumns.HEIGHT
        )
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val w = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.ImageColumns.WIDTH
                    )
                )
                val h = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Images.ImageColumns.HEIGHT
                    )
                )
                return Size(w, h)
            }
        }
        return null
    }
}
