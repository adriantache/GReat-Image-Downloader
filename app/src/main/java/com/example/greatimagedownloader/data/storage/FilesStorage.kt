package com.example.greatimagedownloader.data.storage

import com.example.greatimagedownloader.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody

interface FilesStorage {
    fun getSavedPhotos(): List<String>
    fun getSavedMovies(): List<String>

    fun savePhoto(
        responseBody: ResponseBody,
        file: PhotoFile,
    ): Flow<PhotoDownloadInfo>

    fun deleteMedia(uri: String)

    suspend fun deleteAll()
}
