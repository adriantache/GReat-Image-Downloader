package com.adriantache.greatimagedownloader.data.storage

import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody

interface FilesStorage {
    fun getSavedPhotos(): List<PhotoDownloadInfo>
    fun getSavedMovies(): List<String>

    fun savePhoto(
        responseBody: ResponseBody,
        file: PhotoFile,
    ): Flow<PhotoDownloadInfo>

    fun deleteMedia(uri: String)

    suspend fun deleteAll()
}
