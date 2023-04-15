package com.example.greatimagedownloader.domain.data

import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.data.model.WifiDetails
import kotlinx.coroutines.flow.Flow

interface Repository {
    // Wifi
    fun getWifiDetails(): WifiDetails
    fun saveWifiDetails(wifiDetails: WifiDetails)

    // Data storage
    fun getSavedPhotos(): List<String>
    fun getSavedMovies(): List<String>

    // Camera operations
    suspend fun getCameraPhotoList(): Result<List<PhotoFile>>
    fun downloadMediaToStorage(photo: PhotoFile): Flow<PhotoDownloadInfo>
    suspend fun shutDownCamera()
}
