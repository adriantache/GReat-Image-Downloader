package com.adriantache.greatimagedownloader.domain.data

import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails
import com.adriantache.greatimagedownloader.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface Repository {
    // Wifi
    fun getWifiDetails(): Result<WifiDetails>
    fun saveWifiDetails(wifiDetails: WifiDetails): Result<Unit>

    // Data storage
    fun getSavedPhotos(): Result<List<PhotoDownloadInfo>>
    fun getSavedMovies(): Result<List<String>>
    fun deleteMedia(uri: String): Result<Unit>
    suspend fun deleteAll(): Result<Unit>

    // Camera operations
    suspend fun getCameraPhotoList(): Result<List<PhotoFile>>
    fun downloadMediaToStorage(photo: PhotoFile): Flow<Result<PhotoDownloadInfo>>
    suspend fun shutDownCamera(): Result<Unit>

    // Settings
    suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>): Result<Unit>
    suspend fun getLatestDownloadedPhotos(): Result<List<PhotoFile>>
    suspend fun saveSettings(settings: Settings): Result<Unit>
    suspend fun getSettings(): Result<Settings>
}
