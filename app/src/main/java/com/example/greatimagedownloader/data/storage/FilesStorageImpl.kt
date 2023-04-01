package com.example.greatimagedownloader.data.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toFile
import com.example.greatimagedownloader.data.model.PhotoDownloadInfo
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
private const val MIN_SIZE_BYTES = 100
private const val OKIO_MAX_BYTES = 8092L

class FilesStorageImpl(private val context: Context) : FilesStorage {
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

    override suspend fun savePhoto(
        responseBody: ResponseBody,
        filename: String,
    ): Flow<PhotoDownloadInfo> {
        Log.i("TAGXXX", "Saving responsebody $responseBody")

        return flow {
            val contentResolver = context.contentResolver
            val imageUri =
                getImageUri(contentResolver, filename, responseBody.contentType()) ?: return@flow
            val outputStream = contentResolver.openOutputStream(imageUri) ?: return@flow

            Log.i("TAGXXX", "Got outputstream $outputStream")

            val source = responseBody.source()

            Log.i("TAGXXX", "Got source $source")

            val destination = outputStream.sink().buffer()
            val fileSize = responseBody.contentLength()

            try {
                var totalBytesRead = 0L

                while (!source.exhausted()) {
                    val bytesRead = source.buffer.read(destination.buffer, OKIO_MAX_BYTES)
                        .takeUnless { it == -1L } ?: break
                    destination.emit()

                    Log.i("TAGXXX", "writing $bytesRead total $totalBytesRead")

                    totalBytesRead += bytesRead

                    val progress = if (fileSize == -1L) {
                        0
                    } else {
                        (totalBytesRead.toFloat() / fileSize * 100).roundToInt()
                    }
                    emit(PhotoDownloadInfo(uri = imageUri, downloadProgress = progress))
                }

                emit(PhotoDownloadInfo(uri = imageUri, downloadProgress = 100))
            } catch (e: IOException) {
                // TODO: test this works as intended
                imageUri.toFile().delete()
                return@flow
            } finally {
                source.close()
                destination.close()
                outputStream.close()
                responseBody.close()
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun getImageUri(
        contentResolver: ContentResolver,
        filename: String,
        contentType: MediaType?,
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, contentType?.toString() ?: DEFAULT_MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RICOH_PHOTOS_PATH)
        }

        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }
}
