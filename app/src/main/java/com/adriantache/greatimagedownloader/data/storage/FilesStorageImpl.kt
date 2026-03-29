package com.adriantache.greatimagedownloader.data.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.adriantache.greatimagedownloader.data.utils.speedCalculator.SpeedCalculator
import com.adriantache.greatimagedownloader.data.utils.speedCalculator.SpeedCalculatorImpl
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
import com.adriantache.greatimagedownloader.ui.util.findActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.buffer
import okio.sink
import java.io.IOException
import kotlin.math.roundToInt

private val RICOH_PHOTOS_PATH = Environment.DIRECTORY_PICTURES + "/Image Sync"
private val RICOH_MOVIES_PATH = Environment.DIRECTORY_MOVIES + "/Image Sync"
private const val DEFAULT_MIME_TYPE = "image/jpg"
private const val MIME_TYPE_VIDEO = "video"
private const val MIN_SIZE_BYTES = 100
private const val OKIO_BUFFER_SIZE = 256 * 1024L // 256KB buffer for optimized I/O
private const val LAST_PROGRESS_TIME = 300L

private data class DownloadedFile(
    val id: Long,
    val name: String,
    val isLandscape: Boolean,
)

class FilesStorageImpl(
    private val context: Context,
    private val speedCalculator: SpeedCalculator = SpeedCalculatorImpl(),
) : FilesStorage {
    override fun getSavedPhotos(): List<com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo> {
        return getSavedPhotoFiles().map {
            com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo(
                name = it.name,
                uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.id).toString(),
                downloadProgress = 100,
                downloadSpeed = Kbps(0.0),
                isLandscape = it.isLandscape
            )
        }
    }

    private fun getSavedPhotoFiles(): List<DownloadedFile> {
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} like ?"
        val selectionArgs = arrayOf("%$RICOH_PHOTOS_PATH%")
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.TITLE,
            OpenableColumns.SIZE,
            MediaStore.Images.ImageColumns.WIDTH,
            MediaStore.Images.ImageColumns.HEIGHT,
            MediaStore.Images.ImageColumns.ORIENTATION,
        )

        val results = mutableListOf<DownloadedFile>()

        contentResolver.query(
            /* uri = */ MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            /* projection = */ projection,
            /* selection = */ selection,
            /* selectionArgs = */ selectionArgs,
            /* sortOrder = */ MediaStore.Images.Media.DATE_TAKEN,
        ).use { cursor ->
            if (cursor == null) return emptyList()

            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val title = cursor.getString(1)
                val size = cursor.getLong(2)
                val width = cursor.getInt(3)
                val height = cursor.getInt(4)
                val orientation = cursor.getInt(5)

                if (size > MIN_SIZE_BYTES) {
                    // Check if EXIF orientation swaps width and height
                    val isActuallyLandscape = if (orientation == 90 || orientation == 270) {
                        height > width
                    } else {
                        width > height
                    }

                    results.add(
                        DownloadedFile(
                            id = id,
                            name = title,
                            isLandscape = isActuallyLandscape
                        )
                    )
                }
            }
        }

        return results
    }

    override fun getSavedMovies(): List<String> {
        return getSavedMovieFiles().map { it.name }
    }

    private fun getSavedMovieFiles(): List<DownloadedFile> {
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} like ?"
        val selectionArgs = arrayOf("%$RICOH_MOVIES_PATH%")
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.TITLE,
            OpenableColumns.SIZE,
            MediaStore.Video.VideoColumns.WIDTH,
            MediaStore.Video.VideoColumns.HEIGHT,
        )

        val results = mutableListOf<DownloadedFile>()

        contentResolver.query(
            /* uri = */ MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            /* projection = */ projection,
            /* selection = */ selection,
            /* selectionArgs = */ selectionArgs,
            /* sortOrder = */ MediaStore.Images.Media.DATE_TAKEN,
        ).use { cursor ->
            if (cursor == null) return emptyList()

            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val title = cursor.getString(1)
                val size = cursor.getLong(2)
                val width = cursor.getInt(3)
                val height = cursor.getInt(4)

                if (size > MIN_SIZE_BYTES) {
                    results.add(
                        DownloadedFile(
                            id = id,
                            name = title,
                            isLandscape = width > height
                        )
                    )
                }
            }
        }

        return results
    }

    override fun savePhoto(
        responseBody: ResponseBody,
        file: PhotoFile,
    ): Flow<com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo> {
        return flow {
            val fileSize = responseBody.contentLength()
            speedCalculator.reset()

            val contentResolver = context.contentResolver
            val imageUri = getFileUri(contentResolver, file, responseBody.contentType()) ?: return@flow
            val outputStream = contentResolver.openOutputStream(imageUri) ?: return@flow

            responseBody.source().use { source ->
                outputStream.sink().buffer().use { destination ->
                    try {
                        var totalBytesRead = 0L
                        var lastProgressReportTime = 0L

                        while (true) {
                            val bytesRead = source.read(destination.buffer, OKIO_BUFFER_SIZE)
                            if (bytesRead == -1L) break

                            destination.emit()

                            totalBytesRead += bytesRead
                            speedCalculator.registerData(bytesRead)

                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastProgressReportTime > LAST_PROGRESS_TIME) {
                                val progress = if (fileSize <= 0L) {
                                    0
                                } else {
                                    (totalBytesRead.toFloat() / fileSize * 100).roundToInt().coerceAtMost(99)
                                }

                                emit(
                                    com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo(
                                        uri = imageUri.toString(),
                                        downloadProgress = progress,
                                        name = file.name,
                                        downloadSpeed = Kbps(speedCalculator.getAverageSpeedKbps(currentTime)),
                                    )
                                )

                                lastProgressReportTime = currentTime
                            }
                        }

                        // Get image dimensions after download completes
                        val isLandscape = isLandscape(contentResolver, imageUri)

                        emit(
                            com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo(
                                uri = imageUri.toString(),
                                downloadProgress = 100,
                                name = file.name,
                                downloadSpeed = Kbps(speedCalculator.getAverageSpeedKbps()),
                                isLandscape = isLandscape,
                            )
                        )
                    } catch (e: IOException) {
                        deleteInvalidFile(contentResolver, imageUri)
                        return@flow
                    }
                }
            }

            outputStream.close()
            responseBody.close()
        }.flowOn(Dispatchers.IO)
    }

    private fun isLandscape(resolver: ContentResolver, uri: Uri): Boolean {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            var orientation = ExifInterface.ORIENTATION_NORMAL
            resolver.openInputStream(uri)?.use {
                orientation = ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                options.outHeight > options.outWidth
            } else {
                options.outWidth > options.outHeight
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteMedia(uri: String) {
        val contentResolver = context.contentResolver

        deleteInvalidFile(
            contentResolver = contentResolver,
            uri = Uri.parse(uri),
        )
    }

    override suspend fun deleteAll() {
        getSavedPhotoFiles().forEach {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.id)
            deleteMedia(uri.toString())
        }

        getSavedMovieFiles().forEach {
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it.id)
            deleteMedia(uri.toString())
        }
    }

    private fun getFileUri(
        contentResolver: ContentResolver,
        file: PhotoFile,
        contentType: MediaType?,
    ): Uri? {
        val mediaTypeString = contentType?.toString() ?: DEFAULT_MIME_TYPE
        val isVideo = contentType?.type == MIME_TYPE_VIDEO
        val rootFolder = if (isVideo) RICOH_MOVIES_PATH else RICOH_PHOTOS_PATH
        val fullPath = rootFolder + "/" + file.directory

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mediaTypeString)
            put(MediaStore.MediaColumns.RELATIVE_PATH, fullPath)
        }

        val contentUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        return contentResolver.insert(contentUri, contentValues)
    }

    private fun deleteInvalidFile(
        contentResolver: ContentResolver,
        uri: Uri,
    ) {
        val activity = context.findActivity() ?: return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }

        val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
        startIntentSenderForResult(activity, pendingIntent.intentSender, 1, null, 0, 0, 0, null)
    }
}
