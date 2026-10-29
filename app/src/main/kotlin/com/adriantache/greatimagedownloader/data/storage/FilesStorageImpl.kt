package com.adriantache.greatimagedownloader.data.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.adriantache.greatimagedownloader.data.storage.error.FilesStorageException
import com.adriantache.greatimagedownloader.data.utils.speedCalculator.SpeedCalculator
import com.adriantache.greatimagedownloader.data.utils.speedCalculator.SpeedCalculatorImpl
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
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
private const val DEFAULT_MIME_TYPE = "image/jpeg"
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
    override fun getSavedPhotos(): Result<List<PhotoDownloadInfo>> = runCatching {
        getSavedPhotoFiles().map {
            PhotoDownloadInfo(
                name = it.name,
                uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.id).toString(),
                downloadProgress = 100,
                downloadSpeed = Kbps(0.0),
                isLandscape = it.isLandscape
            )
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(FilesStorageException.Unknown(it)) }
    )

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
            MediaStore.MediaColumns.DISPLAY_NAME,
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
                val displayName = cursor.getString(6)

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
                            name = displayName?.takeIf { it.isNotBlank() } ?: title,
                            isLandscape = isActuallyLandscape
                        )
                    )
                }
            }
        }

        return results
    }

    override fun getSavedMovies(): Result<List<String>> = runCatching {
        getSavedMovieFiles().map { it.name }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(FilesStorageException.Unknown(it)) }
    )

    private fun getSavedMovieFiles(): List<DownloadedFile> {
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} like ?"
        val selectionArgs = arrayOf("%$RICOH_MOVIES_PATH%")
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.TITLE,
            OpenableColumns.SIZE,
            MediaStore.MediaColumns.DISPLAY_NAME,
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
                val displayName = cursor.getString(3)

                if (size > MIN_SIZE_BYTES) {
                    results.add(
                        DownloadedFile(
                            id = id,
                            name = displayName?.takeIf { it.isNotBlank() } ?: title,
                            isLandscape = false
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
    ): Flow<Result<PhotoDownloadInfo>> {
        return flow {
            val fileSize = responseBody.contentLength()

            val contentResolver = context.contentResolver
            val imageUri = getFileUri(contentResolver, file, responseBody.contentType()) ?: run {
                responseBody.close()
                emit(Result.failure(FilesStorageException.FileCreationError))
                return@flow
            }
            val outputStream = contentResolver.openOutputStream(imageUri) ?: run {
                deleteInvalidFile(contentResolver, imageUri)
                responseBody.close()
                emit(Result.failure(FilesStorageException.FileCreationError))
                return@flow
            }

            try {
                val imageUriString = imageUri.toString()

                responseBody.use { body ->
                    body.source().use { source ->
                        outputStream.sink().buffer().use { destination ->
                            var totalBytesRead = 0L
                            var bytesSinceLastReport = 0L
                            var lastProgressReportTime = 0L

                            while (true) {
                                val bytesRead = source.read(destination.buffer, OKIO_BUFFER_SIZE)
                                if (bytesRead == -1L) break

                                if (destination.buffer.size >= OKIO_BUFFER_SIZE) {
                                    destination.emitCompleteSegments()
                                }

                                totalBytesRead += bytesRead
                                bytesSinceLastReport += bytesRead

                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastProgressReportTime > LAST_PROGRESS_TIME) {
                                    speedCalculator.registerData(bytesSinceLastReport, currentTime)
                                    bytesSinceLastReport = 0L

                                    val progress = if (fileSize <= 0L) {
                                        0
                                    } else {
                                        (totalBytesRead.toFloat() / fileSize * 100).roundToInt().coerceAtMost(99)
                                    }

                                    emit(
                                        Result.success(
                                            PhotoDownloadInfo(
                                                uri = imageUriString,
                                                downloadProgress = progress,
                                                name = file.name,
                                                downloadSpeed = Kbps(speedCalculator.getAverageSpeedKbps(currentTime)),
                                            )
                                        )
                                    )

                                    lastProgressReportTime = currentTime
                                }
                            }

                            if (bytesSinceLastReport > 0L) {
                                speedCalculator.registerData(bytesSinceLastReport)
                            }
                            destination.flush()
                        }
                    }
                }

                markFileComplete(contentResolver, imageUri)

                // Skip EXIF/bitmap dimension inspection for video files
                val isLandscape = if (file.isVideoFile) {
                    false
                } else {
                    isLandscape(contentResolver, imageUri)
                }

                emit(
                    Result.success(
                        PhotoDownloadInfo(
                            uri = imageUriString,
                            downloadProgress = 100,
                            name = file.name,
                            downloadSpeed = Kbps(speedCalculator.getAverageSpeedKbps()),
                            isLandscape = isLandscape,
                        )
                    )
                )
            } catch (e: IOException) {
                deleteInvalidFile(contentResolver, imageUri)
                val error = if (e.message?.contains("No space left on device", ignoreCase = true) == true) {
                    FilesStorageException.StorageFull
                } else {
                    FilesStorageException.NetworkError(e)
                }
                emit(Result.failure(error))
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun isLandscape(resolver: ContentResolver, uri: Uri): Boolean {
        return try {
            var orientation = ExifInterface.ORIENTATION_NORMAL
            var width = 0
            var height = 0

            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    .takeIf { it > 0 }
                    ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0)
                height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                    .takeIf { it > 0 }
                    ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0)
            }

            if (width <= 0 || height <= 0) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                width = options.outWidth
                height = options.outHeight
            }

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                height > width
            } else {
                width > height
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun deleteMedia(uri: String): Result<Unit> = runCatching {
        val contentResolver = context.contentResolver

        deleteInvalidFile(
            contentResolver = contentResolver,
            uri = uri.toUri(),
        )
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(FilesStorageException.Unknown(it)) }
    )

    override suspend fun deleteAll(): Result<Unit> = runCatching {
        getSavedPhotoFiles().forEach {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.id)
            deleteMedia(uri.toString())
        }

        getSavedMovieFiles().forEach {
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it.id)
            deleteMedia(uri.toString())
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(FilesStorageException.Unknown(it)) }
    )


    private fun getFileUri(
        contentResolver: ContentResolver,
        file: PhotoFile,
        contentType: MediaType?,
    ): Uri? {
        val mediaTypeString = resolveMimeType(file.name, contentType)
        val isVideo = mediaTypeString.startsWith("$MIME_TYPE_VIDEO/")
        val rootFolder = if (isVideo) RICOH_MOVIES_PATH else RICOH_PHOTOS_PATH
        val fullPath = rootFolder + "/" + file.directory

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mediaTypeString)
            put(MediaStore.MediaColumns.RELATIVE_PATH, fullPath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val contentUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        return contentResolver.insert(contentUri, contentValues)
    }

    private fun markFileComplete(
        contentResolver: ContentResolver,
        uri: Uri,
    ) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            contentResolver.update(uri, values, null, null)
        }
    }

    internal fun resolveMimeType(fileName: String, contentType: MediaType?): String {
        val reported = contentType?.let { "${it.type}/${it.subtype}".lowercase() }
        if (reported != null && reported != "application/octet-stream") {
            return if (reported == "image/jpg") DEFAULT_MIME_TYPE else reported
        }

        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "dng" -> "image/x-adobe-dng"
            "raw" -> "image/x-raw"
            "jpg", "jpeg" -> DEFAULT_MIME_TYPE
            "mov" -> "video/quicktime"
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            else -> DEFAULT_MIME_TYPE
        }
    }

    private fun deleteInvalidFile(
        contentResolver: ContentResolver,
        uri: Uri,
    ) {
        runCatching {
            contentResolver.delete(uri, null, null)
        }
    }
}
