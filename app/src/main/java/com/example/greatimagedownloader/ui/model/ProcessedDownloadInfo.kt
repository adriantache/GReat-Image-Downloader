package com.example.greatimagedownloader.ui.model

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo

sealed class ProcessedDownloadInfo(val uri: String) {
    class Pending(val downloadProgress: Int, uri: String) : ProcessedDownloadInfo(uri)
    class Finished(val bitmap: ImageBitmap, uri: String) : ProcessedDownloadInfo(uri)

    companion object {
        fun PhotoDownloadInfo.toProcessedDownloadInfo(
            contentResolver: ContentResolver,
            processedDownloadInfo: List<ProcessedDownloadInfo>,
        ): ProcessedDownloadInfo {
            // TODO: after building correct ui object, add boolean to replace this logic
            if (this.downloadProgress < 100) return Pending(this.downloadProgress, this.uri)

            val existing = this.getExisting(processedDownloadInfo)

            return if (existing != null) {
                existing
            } else {
                val bitmap = this.uri.toImageBitmap(contentResolver)
                Finished(bitmap, this.uri)
            }
        }

        // Used to prevent regenerating bitmaps where we already have them.
        private fun PhotoDownloadInfo.getExisting(
            processedDownloadInfo: List<ProcessedDownloadInfo>,
        ): Finished? {
            return processedDownloadInfo.filterIsInstance<Finished>().find { it.uri == this.uri }
        }

        private fun String.toImageBitmap(contentResolver: ContentResolver): ImageBitmap {
            val parsedUri = Uri.parse(this)
            val source = ImageDecoder.createSource(contentResolver, parsedUri)
            return ImageDecoder.decodeBitmap(source).asImageBitmap()
        }
    }
}
