package com.example.greatimagedownloader.data.storage

import com.example.greatimagedownloader.data.model.PhotoDownloadInfo
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody

interface FilesStorage {
    fun getSavedPhotos(): List<String>
    suspend fun savePhoto(photoData: ResponseBody, filename: String): Flow<PhotoDownloadInfo>
}
