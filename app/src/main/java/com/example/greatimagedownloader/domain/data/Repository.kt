package com.example.greatimagedownloader.domain.data

import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.data.model.WifiDetails
import com.example.greatimagedownloader.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface Repository {
    // Wifi
    fun getWifiDetails(): WifiDetails
    fun saveWifiDetails(wifiDetails: WifiDetails)

    // Data storage
    fun getSavedPhotos(): List<String>
    fun getSavedMovies(): List<String>
    fun deleteMedia(uri: String)
    suspend fun deleteAll()

    // Camera operations
    suspend fun getCameraPhotoList(): Result<List<PhotoFile>>
    fun downloadMediaToStorage(photo: PhotoFile): Flow<PhotoDownloadInfo>
    suspend fun shutDownCamera()

    // Settings
    suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>)
    suspend fun getLatestDownloadedPhotos(): List<PhotoFile>
    suspend fun saveSettings(settings: Settings)
    suspend fun getSettings(): Settings
}
