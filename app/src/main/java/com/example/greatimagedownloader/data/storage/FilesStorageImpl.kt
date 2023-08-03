package com.example.greatimagedownloader.data.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.example.greatimagedownloader.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.data.utils.speedCalculator.SpeedCalculator
import com.example.greatimagedownloader.data.utils.speedCalculator.SpeedCalculatorImpl
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.utils.model.Kbps
import com.example.greatimagedownloader.ui.util.findActivity
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
private const val OKIO_MAX_BYTES = 8092L

class FilesStorageImpl(
    private val context: Context,
    private val speedCalculator: SpeedCalculator = SpeedCalculatorImpl(),
) : FilesStorage {
    override fun getSavedPhotos(): List<String> {
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} like ?"
        val selectionArgs = arrayOf("%$RICOH_PHOTOS_PATH%")
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.MediaColumns.TITLE,
            OpenableColumns.SIZE,
        )

        val results = mutableListOf<String>()

        contentResolver.query(
            /* uri = */ MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            /* projection = */ projection,
            /* selection = */ selection,
            /* selectionArgs = */ selectionArgs,
            /* sortOrder = */ MediaStore.Images.Media.DATE_TAKEN,
        ).use { cursor ->
            if (cursor == null) return emptyList()

            while (cursor.moveToNext()) {
                val title = cursor.getString(0)
                val size = cursor.getLong(1)

                // TODO: return files to delete (size 51) instead
                if (size > MIN_SIZE_BYTES) {
                    results.add(title)
                }
            }
        }

        return results
    }

    override fun getSavedMovies(): List<String> {
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} like ?"
        val selectionArgs = arrayOf("%$RICOH_MOVIES_PATH%")
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.MediaColumns.TITLE,
            OpenableColumns.SIZE,
        )

        val results = mutableListOf<String>()

        contentResolver.query(
            /* uri = */ MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            /* projection = */ projection,
            /* selection = */ selection,
            /* selectionArgs = */ selectionArgs,
            /* sortOrder = */ MediaStore.Images.Media.DATE_TAKEN,
        ).use { cursor ->
            if (cursor == null) return emptyList()

            while (cursor.moveToNext()) {
                val title = cursor.getString(0)
                val size = cursor.getLong(1)

                // TODO: return files to delete (size 51) instead
                if (size > MIN_SIZE_BYTES) {
                    results.add(title)
                }
            }
        }

        return results
    }

    override fun savePhoto(
        responseBody: ResponseBody,
        file: PhotoFile,
    ): Flow<PhotoDownloadInfo> {
        return flow {
            val fileSize = responseBody.contentLength()

            val contentResolver = context.contentResolver
            val imageUri = getFileUri(contentResolver, file, responseBody.contentType()) ?: return@flow
            val outputStream = contentResolver.openOutputStream(imageUri) ?: return@flow

            responseBody.source().use { source ->
                outputStream.sink().buffer().use { destination ->
                    try {
                        var totalBytesRead = 0L
                        var lastProgressReportTime = 0L

                        while (!source.exhausted()) {
                            val bytesRead = source.buffer.read(destination.buffer, OKIO_MAX_BYTES).takeUnless { it == -1L } ?: break
                            destination.emit()

                            totalBytesRead += bytesRead
                            speedCalculator.registerData(bytesRead)

                            val progress = if (fileSize == -1L) {
                                0
                            } else {
                                (totalBytesRead.toFloat() / fileSize * 100).roundToInt().coerceAtMost(99)
                            }

                            if (System.currentTimeMillis() - lastProgressReportTime > 1000L) {
                                emit(
                                    PhotoDownloadInfo(
                                        uri = imageUri,
                                        downloadProgress = progress,
                                        name = file.name,
                                        downloadSpeed = Kbps(speedCalculator.getAverageSpeedKbps()),
                                    )
                                )

                                lastProgressReportTime = System.currentTimeMillis()
                            }
                        }

                        emit(
                            PhotoDownloadInfo(
                                uri = imageUri,
                                downloadProgress = 100,
                                name = file.name,
                                downloadSpeed = Kbps(speedCalculator.getAverageSpeedKbps()),
                            )
                        )
                    } catch (e: IOException) {
                        // TODO: fix this not working
                        deleteInvalidFile(contentResolver, imageUri)
                        return@flow
                    }
                }
            }

            outputStream.close()
            responseBody.close()
        }.flowOn(Dispatchers.IO)
    }

    override fun deleteMedia(uri: String) {
        val contentResolver = context.contentResolver

        deleteInvalidFile(
            contentResolver = contentResolver,
            uri = Uri.parse(uri),
        )
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
