package com.example.greatimagedownloader.data.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private const val PATH = "Pictures/Image Sync"

class FilesStorageImpl(private val context: Context) : FilesStorage {

    // TODO: simplify this if we don't need all queries
    override fun getSavedPhotos(): List<String> {
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} like ?"
        val selectionArgs = arrayOf("%$PATH%")
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.MediaColumns.TITLE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            OpenableColumns.SIZE,
        )

        val results = mutableListOf<String>()

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Images.Media.DATE_TAKEN
        ).use { cursor ->
            if (cursor == null) return emptyList()

            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val date = cursor.getString(1)
                val title = cursor.getString(2)
                val mime = cursor.getString(3)
                val path = cursor.getString(4)
                val size = cursor.getLong(5)

                // TODO: return files to delete (size 51) instead
                if (size > 100) {
                    results.add(title)
                }
            }
        }

        return results
    }

    // TODO: clean up this implementation
    override suspend fun savePhoto(photoData: ResponseBody, name: String): Uri? {
        return photoData.parseImage(context, name)
    }

    private suspend fun ResponseBody.parseImage(context: Context, name: String): Uri? {
        return writeResponseBody(this, context, name)
    }

    private fun getOutputStream(context: Context, name: String): Pair<OutputStream?, Uri?> {
        val resolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        contentValues.put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/Image Sync"
        )
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return imageUri?.let { resolver.openOutputStream(it) } to imageUri
    }

    private suspend fun writeResponseBody(
        body: ResponseBody,
        context: Context,
        name: String
    ): Uri? {
        return withContext(Dispatchers.IO) {
            val inputStream: InputStream =
                body.byteStream() ?: throw IllegalArgumentException("No bytes")
            // TODO: clean this up
            val (outputStream, fileUri) =
                getOutputStream(context, name)

            if (outputStream == null) return@withContext null

            try {
                val fileReader = ByteArray(4096)
                //long fileSize = body.contentLength();
                //long fileSizeDownloaded = 0;

                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                    //fileSizeDownloaded += read;
                }
                outputStream.flush()
                fileUri
            } catch (e: IOException) {
                null
            } finally {
                inputStream.close()
                outputStream.close()
            }
        }
    }
}
