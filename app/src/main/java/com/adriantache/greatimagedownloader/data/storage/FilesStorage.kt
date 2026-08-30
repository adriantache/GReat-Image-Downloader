package com.adriantache.greatimagedownloader.data.storage

import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody

interface FilesStorage {
    fun getSavedPhotos(): Result<List<PhotoDownloadInfo>>
    fun getSavedMovies(): Result<List<String>>

    fun savePhoto(
        responseBody: ResponseBody,
        file: PhotoFile,
    ): Flow<Result<PhotoDownloadInfo>>

    fun deleteMedia(uri: String): Result<Unit>

    suspend fun deleteAll(): Result<Unit>
}
