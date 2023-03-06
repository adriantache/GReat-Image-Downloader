package com.example.greatimagedownloader.data.storage

import okhttp3.ResponseBody

interface FilesStorage {
    fun getSavedPhotos(): List<String>
    suspend fun savePhoto(photoData: ResponseBody, name: String)
}
